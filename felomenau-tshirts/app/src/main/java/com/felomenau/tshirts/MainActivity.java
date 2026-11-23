package com.felomenau.tshirts;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.webkit.*;
import android.net.Uri;
import android.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.widget.Toast;
import org.json.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PHOTO = 100, BACKUP = 101, RESTORE = 102, CSV = 103;
    private WebView web;
    private Store store;
    private String pendingExport;
    private String pendingMime;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new Store();
        web = new WebView(this);
        web.setBackgroundColor(0xff111115);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(false);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setAllowFileAccessFromFileURLs(false);
        web.getSettings().setAllowUniversalAccessFromFileURLs(false);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    @Override @SuppressWarnings("deprecation") public void onBackPressed() {
        if (web == null) { super.onBackPressed(); return; }
        web.evaluateJavascript("window.handleDeviceBack && window.handleDeviceBack()", handled -> {
            if (!"true".equals(handled)) MainActivity.super.onBackPressed();
        });
    }
    private void send(String event, String data) {
        ui.post(() -> web.evaluateJavascript("window.nativeEvent(" + JSONObject.quote(event) + "," + data + ")", null));
    }
    private void notifyError(Exception e) { send("error", JSONObject.quote(e.getMessage() == null ? "Erro inesperado" : e.getMessage())); }

    class Bridge {
        @JavascriptInterface public String state() { try { return store.snapshot().toString(); } catch(Exception e) { return "{}"; } }
        @JavascriptInterface public void product(String json) { try { store.upsertProduct(new JSONObject(json)); send("updated", "null"); } catch (Exception e) { notifyError(e); } }
        @JavascriptInterface public void stock(String json) { try { store.changeStock(new JSONObject(json)); send("updated", "null"); } catch(Exception e) { notifyError(e); } }
        @JavascriptInterface public void sale(String json) { try { store.newSale(new JSONObject(json)); send("updated", "null"); } catch(Exception e) { notifyError(e); } }
        @JavascriptInterface public void cancelSale(long id) { try { store.cancelSale(id); send("updated", "null"); } catch(Exception e) { notifyError(e); } }
        @JavascriptInterface public void archive(long id, boolean archive) { try { store.archive(id, archive); send("updated", "null"); } catch(Exception e) { notifyError(e); } }
        @JavascriptInterface public void pickPhoto() { ui.post(() -> { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, PHOTO); }); }
        @JavascriptInterface public void backup() { try { pendingExport = store.exportAll().toString(2); pendingMime="application/json"; createDocument("felomenau-backup.json", BACKUP, pendingMime); } catch(Exception e) { notifyError(e); } }
        @JavascriptInterface public void csv() { try { pendingExport=store.salesCsv(); pendingMime="text/csv"; createDocument("felomenau-vendas.csv",CSV,pendingMime); } catch(Exception e) { notifyError(e); } }
        @JavascriptInterface public void restore() { ui.post(() -> { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/json"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,RESTORE); }); }
    }
    private void createDocument(String filename, int request, String mime) { ui.post(() -> { Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType(mime); i.addCategory(Intent.CATEGORY_OPENABLE); i.putExtra(Intent.EXTRA_TITLE,filename); startActivityForResult(i,request); }); }
    @Override protected void onActivityResult(int code,int result,Intent data) {
        super.onActivityResult(code,result,data);
        if(result!=RESULT_OK || data==null || data.getData()==null) return;
        Uri uri=data.getData();
        try {
            if(code==PHOTO) {
                try(InputStream stream=getContentResolver().openInputStream(uri)) {
                    Bitmap img=BitmapFactory.decodeStream(stream);
                    if(img==null) throw new Exception("Não foi possível abrir a foto");
                    int width=img.getWidth(),height=img.getHeight();
                    float scale=Math.min(1f,1024f/Math.max(width,height));
                    if(scale<1f) img=Bitmap.createScaledBitmap(img,Math.max(1,(int)(width*scale)),Math.max(1,(int)(height*scale)),true);
                    ByteArrayOutputStream out=new ByteArrayOutputStream(); img.compress(Bitmap.CompressFormat.JPEG,82,out);
                    send("photo",JSONObject.quote("data:image/jpeg;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP)));
                }
            } else if(code==BACKUP || code==CSV) {
                try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")) { if(out==null) throw new Exception("Arquivo indisponível"); out.write(pendingExport.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
                send("message",JSONObject.quote("Arquivo exportado com sucesso"));
            } else if(code==RESTORE) {
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=getContentResolver().openInputStream(uri)) { byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1) { out.write(b,0,n); if(out.size()>35_000_000) throw new Exception("Backup muito grande"); } }
                store.restore(new JSONObject(out.toString("UTF-8"))); send("updated","null"); send("message",JSONObject.quote("Backup restaurado"));
            }
        } catch(Exception e) { notifyError(e); }
    }
    class Store extends SQLiteOpenHelper {
        Store() { super(MainActivity.this,"felomenau.db",null,2); }
        @Override public void onCreate(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,sku TEXT,name TEXT NOT NULL,category TEXT NOT NULL DEFAULT 'Camiseta',gender TEXT,collection TEXT,notes TEXT,cost REAL,price REAL,photo TEXT,archived INTEGER DEFAULT 0,created_at TEXT DEFAULT CURRENT_TIMESTAMP)");
            d.execSQL("CREATE TABLE variants(id INTEGER PRIMARY KEY AUTOINCREMENT,product_id INTEGER NOT NULL,size TEXT NOT NULL,qty INTEGER DEFAULT 0,min_qty INTEGER DEFAULT 2,UNIQUE(product_id,size),FOREIGN KEY(product_id) REFERENCES products(id))");
            d.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,payment TEXT,discount REAL DEFAULT 0,fee REAL DEFAULT 0,total REAL DEFAULT 0,cost REAL DEFAULT 0,profit REAL DEFAULT 0,notes TEXT,cancelled INTEGER DEFAULT 0)");
            d.execSQL("CREATE TABLE sale_items(id INTEGER PRIMARY KEY AUTOINCREMENT,sale_id INTEGER,variant_id INTEGER,product_name TEXT,size TEXT,qty INTEGER,unit_price REAL,unit_cost REAL)");
            d.execSQL("CREATE TABLE movements(id INTEGER PRIMARY KEY AUTOINCREMENT,variant_id INTEGER,product_name TEXT,size TEXT,delta INTEGER,kind TEXT,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP,sale_id INTEGER)");
            d.execSQL("CREATE INDEX idx_variants_product ON variants(product_id)"); d.execSQL("CREATE INDEX idx_movements_variant ON movements(variant_id)");
        }
        @Override public void onUpgrade(SQLiteDatabase d,int a,int b) {
            if (a < 2) d.execSQL("ALTER TABLE products ADD COLUMN category TEXT NOT NULL DEFAULT 'Camiseta'");
        }
        private JSONArray rows(SQLiteDatabase db,String sql,String[] args) throws JSONException {
            JSONArray array=new JSONArray();
            try(Cursor c=db.rawQuery(sql,args)) { while(c.moveToNext()) { JSONObject o=new JSONObject(); for(int i=0;i<c.getColumnCount();i++) { int type=c.getType(i); if(type==Cursor.FIELD_TYPE_NULL)o.put(c.getColumnName(i),JSONObject.NULL); else if(type==Cursor.FIELD_TYPE_INTEGER)o.put(c.getColumnName(i),c.getLong(i)); else if(type==Cursor.FIELD_TYPE_FLOAT)o.put(c.getColumnName(i),c.getDouble(i)); else o.put(c.getColumnName(i),c.getString(i)); } array.put(o); } }
            return array;
        }
        synchronized JSONObject snapshot() throws JSONException {
            SQLiteDatabase d=getReadableDatabase(); JSONObject out=new JSONObject();
            out.put("products",rows(d,"SELECT * FROM products ORDER BY archived,name",null));
            out.put("variants",rows(d,"SELECT * FROM variants ORDER BY size",null));
            out.put("sales",rows(d,"SELECT * FROM sales ORDER BY id DESC",null));
            out.put("items",rows(d,"SELECT * FROM sale_items ORDER BY id DESC",null));
            out.put("movements",rows(d,"SELECT * FROM movements ORDER BY id DESC LIMIT 300",null));
            return out;
        }
        private double num(JSONObject j,String k) { return j.optDouble(k,0); }
        private String str(JSONObject j,String k) { return j.optString(k,"").trim(); }
        private void require(boolean ok,String m) { if(!ok) throw new IllegalArgumentException(m); }
        private String productName(SQLiteDatabase d,long variantId) { try(Cursor c=d.rawQuery("SELECT p.name FROM products p JOIN variants v ON p.id=v.product_id WHERE v.id=?",new String[]{""+variantId})) { return c.moveToFirst()?c.getString(0):""; } }
        private String variantSize(SQLiteDatabase d,long id) { try(Cursor c=d.rawQuery("SELECT size FROM variants WHERE id=?",new String[]{""+id})) { return c.moveToFirst()?c.getString(0):""; } }
        synchronized void upsertProduct(JSONObject j) throws Exception {
            String name=str(j,"name"); require(!name.isEmpty(),"Informe o nome do produto");
            JSONArray sizes=j.optJSONArray("sizes"); require(sizes!=null && sizes.length()>0,"Adicione ao menos uma opção ou tamanho");
            double cost=num(j,"cost"), price=num(j,"price"); require(cost>=0 && price>=0,"Valores inválidos");
            SQLiteDatabase d=getWritableDatabase(); d.beginTransaction();
            try {
                ContentValues values=new ContentValues();
                for(String k:new String[]{"sku","name","gender","collection","notes","photo"})values.put(k,str(j,k));
                String category=str(j,"category");
                if(category.isEmpty())category="Camiseta";
                require(category.length()<=60,"Categoria muito longa");
                values.put("category",category);
                values.put("cost",cost); values.put("price",price);
                long id=j.optLong("id",0);
                if(id>0) require(d.update("products",values,"id=?",new String[]{""+id})==1,"Produto não encontrado");
                else id=d.insertOrThrow("products",null,values);
                HashSet<String> seen=new HashSet<>();
                for(int i=0;i<sizes.length();i++) {
                    JSONObject s=sizes.getJSONObject(i); String size=str(s,"size").toUpperCase(Locale.ROOT);
                    require(!size.isEmpty() && seen.add(size),"Tamanhos vazios ou repetidos");
                    int min=Math.max(0,s.optInt("min_qty",2));
                    ContentValues v=new ContentValues();v.put("min_qty",min);
                    int updated=d.update("variants",v,"product_id=? AND size=?",new String[]{""+id,size});
                    if(updated==0) { v.put("product_id",id);v.put("size",size);v.put("qty",0);d.insertOrThrow("variants",null,v); }
                }
                d.setTransactionSuccessful();
            } finally { d.endTransaction(); }
        }
        synchronized void changeStock(JSONObject j) {
            long id=j.optLong("variant_id",0); int delta=j.optInt("delta",0); String notes=str(j,"notes");
            require(id>0 && delta!=0,"Selecione um tamanho e quantidade");
            SQLiteDatabase d=getWritableDatabase(); d.beginTransaction();
            try {
                int count=0;try(Cursor c=d.rawQuery("SELECT qty FROM variants WHERE id=?",new String[]{""+id})) { require(c.moveToFirst(),"Tamanho não encontrado");count=c.getInt(0); }
                require(count+delta>=0,"Estoque insuficiente");
                d.execSQL("UPDATE variants SET qty=qty+? WHERE id=?",new Object[]{delta,id});
                movement(d,id,delta,delta>0?"ENTRADA":"AJUSTE",notes,0);
                d.setTransactionSuccessful();
            } finally {d.endTransaction();}
        }
        private void movement(SQLiteDatabase d,long variantId,int delta,String kind,String notes,long saleId) {
            ContentValues v=new ContentValues();v.put("variant_id",variantId);v.put("product_name",productName(d,variantId));v.put("size",variantSize(d,variantId));v.put("delta",delta);v.put("kind",kind);v.put("notes",notes);if(saleId>0)v.put("sale_id",saleId);d.insertOrThrow("movements",null,v);
        }
        synchronized void newSale(JSONObject j) throws Exception {
            JSONArray lines=j.getJSONArray("lines");require(lines.length()>0,"Adicione itens à venda");
            double discount=num(j,"discount"), fee=num(j,"fee");require(discount>=0 && fee>=0,"Desconto ou taxa inválida");
            String payment=str(j,"payment");require(!payment.isEmpty(),"Selecione a forma de pagamento");
            SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
            try {
                double gross=0,cost=0; JSONArray prepared=new JSONArray();
                for(int i=0;i<lines.length();i++) {
                    JSONObject line=lines.getJSONObject(i);long variantId=line.optLong("variant_id");int qty=line.optInt("qty");double unitPrice=line.optDouble("price",-1);
                    require(variantId>0 && qty>0 && Double.isFinite(unitPrice) && unitPrice>=0,"Item de venda inválido");
                    try(Cursor c=d.rawQuery("SELECT v.qty,p.cost,p.name,p.archived,v.size FROM variants v JOIN products p ON p.id=v.product_id WHERE v.id=?",new String[]{""+variantId})) {
                        require(c.moveToFirst(),"Produto não encontrado");require(c.getInt(3)==0,"Produto fora de linha");require(c.getInt(0)>=qty,"Estoque insuficiente: "+c.getString(2)+" "+c.getString(4));
                        JSONObject x=new JSONObject();x.put("variant_id",variantId);x.put("qty",qty);x.put("unit_price",unitPrice);x.put("unit_cost",c.getDouble(1));x.put("product_name",c.getString(2));x.put("size",c.getString(4));prepared.put(x);
                        gross+=qty*unitPrice;cost+=qty*c.getDouble(1);
                    }
                }
                require(discount<=gross,"Desconto maior que o total");
                ContentValues sale=new ContentValues();sale.put("payment",payment);sale.put("discount",discount);sale.put("fee",fee);sale.put("total",gross-discount);sale.put("cost",cost);sale.put("profit",gross-discount-cost-fee);sale.put("notes",str(j,"notes"));
                long saleId=d.insertOrThrow("sales",null,sale);
                for(int i=0;i<prepared.length();i++) {
                    JSONObject x=prepared.getJSONObject(i);long variantId=x.getLong("variant_id");int qty=x.getInt("qty");
                    int have=0;try(Cursor c=d.rawQuery("SELECT qty FROM variants WHERE id=?",new String[]{""+variantId})) {require(c.moveToFirst(),"Item não encontrado");have=c.getInt(0);}
                    require(have>=qty,"Estoque insuficiente para itens repetidos");
                    ContentValues item=new ContentValues();item.put("sale_id",saleId);item.put("variant_id",variantId);item.put("product_name",x.getString("product_name"));item.put("size",x.getString("size"));item.put("qty",qty);item.put("unit_price",x.getDouble("unit_price"));item.put("unit_cost",x.getDouble("unit_cost"));d.insertOrThrow("sale_items",null,item);
                    d.execSQL("UPDATE variants SET qty=qty-? WHERE id=?",new Object[]{qty,variantId});
                    movement(d,variantId,-qty,"VENDA",str(j,"notes"),saleId);
                }
                d.setTransactionSuccessful();
            } finally { d.endTransaction(); }
        }
        synchronized void cancelSale(long id) {
            SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
            try {
                int cancelled=1;try(Cursor c=d.rawQuery("SELECT cancelled FROM sales WHERE id=?",new String[]{""+id})) {require(c.moveToFirst(),"Venda não encontrada");cancelled=c.getInt(0);}
                require(cancelled==0,"Venda já cancelada");
                try(Cursor c=d.rawQuery("SELECT variant_id,qty FROM sale_items WHERE sale_id=?",new String[]{""+id})) {
                    while(c.moveToNext()) {long variantId=c.getLong(0);int qty=c.getInt(1);d.execSQL("UPDATE variants SET qty=qty+? WHERE id=?",new Object[]{qty,variantId});movement(d,variantId,qty,"ESTORNO","Cancelamento venda #"+id,id);}
                }
                d.execSQL("UPDATE sales SET cancelled=1 WHERE id=?",new Object[]{id});d.setTransactionSuccessful();
            } finally {d.endTransaction();}
        }
        synchronized void archive(long id,boolean archived) {
            ContentValues v=new ContentValues();v.put("archived",archived?1:0);
            require(getWritableDatabase().update("products",v,"id=?",new String[]{""+id})==1,"Produto não encontrado");
        }
        synchronized JSONObject exportAll() throws JSONException {
            SQLiteDatabase d=getReadableDatabase();JSONObject j=new JSONObject();j.put("app","felomenau-tshirts");j.put("version",2);
            for(String table:new String[]{"products","variants","sales","sale_items","movements"})j.put(table,rows(d,"SELECT * FROM "+table,null));
            return j;
        }
        synchronized void restore(JSONObject j) throws JSONException {
            require("felomenau-tshirts".equals(j.optString("app")) && (j.optInt("version")==1 || j.optInt("version")==2),"Backup inválido ou incompatível");
            String[] tables={"products","variants","sales","sale_items","movements"};
            for(String table:tables) require(j.optJSONArray(table)!=null,"Falta tabela "+table);
            SQLiteDatabase d=getWritableDatabase();d.beginTransaction();
            try {
                for(int t=tables.length-1;t>=0;t--)d.delete(tables[t],null,null);
                for(String table:tables) {
                    JSONArray a=j.getJSONArray(table);require(a.length()<200000,"Backup excede limite");
                    for(int i=0;i<a.length();i++) {
                        JSONObject o=a.getJSONObject(i);ContentValues v=new ContentValues();Iterator<String> it=o.keys();
                        while(it.hasNext()) {String k=it.next();Object value=o.get(k);if(value==JSONObject.NULL)v.putNull(k);else if(value instanceof Integer || value instanceof Long)v.put(k,((Number)value).longValue());else if(value instanceof Number)v.put(k,((Number)value).doubleValue());else v.put(k,value.toString());}
                        d.insertOrThrow(table,null,v);
                    }
                }
                d.setTransactionSuccessful();
            } finally {d.endTransaction();}
        }
        synchronized String salesCsv() {
            StringBuilder s=new StringBuilder("Venda;Data;Status;Pagamento;Receita;Custo;Taxa;Desconto;Lucro estimado;Observacoes\n");
            SQLiteDatabase d=getReadableDatabase();
            try(Cursor c=d.rawQuery("SELECT id,created_at,cancelled,payment,total,cost,fee,discount,profit,notes FROM sales ORDER BY id DESC",null)) {
                while(c.moveToNext()) {for(int i=0;i<c.getColumnCount();i++) {if(i>0)s.append(';');String val=c.isNull(i)?"":c.getString(i);s.append('"').append(val.replace("\"","\"\"")).append('"');}s.append('\n');}
            }
            return "\uFEFF"+s;
        }
    }
}
