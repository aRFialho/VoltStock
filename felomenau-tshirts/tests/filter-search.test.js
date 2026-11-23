const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const src=fs.readFileSync(path.join(__dirname,'..','app','src','main','assets','app.js'),'utf8');
const ids={},elem=id=>ids[id]??(ids[id]={innerHTML:'',value:'',textContent:'',hidden:true,focus(){},classList:{toggle(){}}});
const ctx={document:{getElementById:elem,querySelectorAll(){return[]}},window:{addEventListener(){},scrollTo(){}},console,Android:{state(){return '{}'}},confirm(){return true},setTimeout(){},clearTimeout(){}};
vm.createContext(ctx);vm.runInContext(src,ctx);
const data={products:[
{id:1,name:'Camiseta Rock Clássico',category:'Camiseta',sku:'ROCK-01',collection:'Clássicos',price:50,archived:0},
{id:2,name:'Rock Alternativo',category:'Camiseta',sku:'ROCK-02',collection:'Metal',price:55,archived:0},
{id:3,name:'Anel Caveira',category:'Anel',sku:'ANEL-01',collection:'Aço',price:45,archived:0},
{id:4,name:'Bandana Vermelha',category:'Bandana',sku:'BD-01',collection:'Vintage',price:22,archived:0},
{id:5,name:'Camiseta outra',category:'Camiseta',sku:'OUT',collection:'Vintage',price:40,archived:1}],
variants:[{id:11,product_id:1,size:'M',qty:3},{id:12,product_id:1,size:'XXL',qty:0},{id:21,product_id:2,size:'M',qty:0},{id:22,product_id:2,size:'G',qty:7},{id:31,product_id:3,size:'18',qty:4},{id:41,product_id:4,size:'ÚNICO',qty:1},{id:51,product_id:5,size:'M',qty:9}],sales:[],items:[],movements:[]};
vm.runInContext(`db=${JSON.stringify(data)}`,ctx);
const get=code=>vm.runInContext(code,ctx);let n=0;function test(code,expected,label){assert.deepStrictEqual(get(code),expected,label);n++}
test(`filter='classico';filteredProducts().length`,1,'Busca ignora acento');
test(`filter='';categoryFilter='Anel';filteredProducts().map(p=>p.name).join(',')`,'Anel Caveira','Filtro categoria Anel');
test(`categoryFilter='Camiseta';sizeFilter='M';filteredProducts().length`,2,'Camisetas tamanho M');
test(`onlyWithStock=true;filteredProducts().map(p=>p.name).join(',')`,'Camiseta Rock Clássico','Estoque M válido');
test(`categoryFilter='';sizeFilter='18';filteredProducts().map(p=>p.name).join(',')`,'Anel Caveira','Numeração 18 disponível');
test(`sizeFilter='ÚNICO';filteredProducts().map(p=>p.name).join(',')`,'Bandana Vermelha','Variação única');
test(`showArchived=false;filter='outra';sizeFilter='';filteredProducts().length`,0,'Arquivados ocultos');
test(`showArchived=true;filteredProducts().length`,1,'Arquivados visíveis');
test(`filter='';sizeFilter='';showArchived=false;categoryFilter='';onlyWithStock=false;filteredProducts().length`,4,'Categorias independentes');
test(`products().includes('productSalesSort')`,true,'Ordenação na tela');
test(`products().includes('productSizeFilter')`,true,'Tamanho na tela');
test(`products().includes('productCategory')`,true,'Categoria na tela');
test(`sale().includes('sellSearch')&&sale().includes('sellSize')&&sale().includes('saleCategory')`,true,'Filtros na venda');
test(`saleProductGrid(filteredSaleProducts()).includes('Anel Caveira')`,true,'Vitrine mostra acessórios');
test(`saleOnlyStock=true;saleSize='XXL';filteredSaleProducts().length`,0,'Esgotados ocultos');
test(`saleOnlyStock=false;filteredSaleProducts().map(p=>p.name).join(',')`,'Camiseta Rock Clássico','Esgotados visíveis');
console.log(`OK: ${n} verificações de busca, filtros e categorias`);
