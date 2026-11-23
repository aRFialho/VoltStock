# Felomenau T-Shirts 🎸 — Android V1.3

Projeto Android offline para **tablets e celulares**, com banco **SQLite nativo**, WebView local e interface rock. Nenhuma API, conta, servidor, assinatura ou conexão com a internet é necessária para usar o aplicativo. O logo provisório será substituído quando a marca oficial for enviada.

## O que há na V1.3

- **PDV visual:** nova venda começa pela vitrine de produtos, com imagem, categoria, descrição, SKU, preço e disponibilidade. Toque na foto/cartão para abrir o seletor de tamanho/numeração, quantidade e preço unitário daquela venda.
- **Carrinho interativo:** várias categorias na mesma compra, fotos dos itens, ajustes com `+` e `−`, remoção, quantidade total, subtotal, desconto, taxa do pagamento, forma de pagamento, observações, total final e margem/sobra bruta estimada. Em celular aparece um atalho fixo para o carrinho.
- **Conferência antes de concluir:** modal de revisão com todos os itens e valores. A baixa acontece **somente ao confirmar**, por transação SQLite. Se o Android retornar erro, o carrinho permanece preservado.
- **Produtos de várias categorias:** camisetas, anéis, bandanas, bonés, pulseiras, colares, chaveiros, patches, acessórios ou categoria personalizada. Cada produto tem foto, custo, preço, SKU, gênero, coleção e observações. Pode ser arquivado como “fora de linha”.
- **Variações livres por item:** tamanhos de camisa (P/M/G etc.), numeração de anel (16/18/20), único, personalizados, cada um com seu estoque e mínimo configuráveis.
- **Catálogo:** busca por nome, SKU ou coleção; filtros combinados de categoria, tamanho, disponibilidade e fora de linha; ordenação A-Z, mais vendidos e menos vendidos.
- **Estoque:** entrada, correção, motivo/observação, saldos independentes por variação e alerta de estoque mínimo.
- **Histórico e relatórios:** vendas com pagamento, itens, preços históricos, custo histórico e sobra; estorno de venda com devolução ao estoque; indicadores, mais vendidos e relatório por forma de pagamento.
- **Backups:** JSON exporta produtos, fotos, variações, vendas e movimentações; restauração de backups das versões 1.0–1.3; exportação de vendas CSV.
- **UX rock:** interface preta/grafite/laranja com sidebar em tablet, navegação inferior em celular e controles maiores para toque. Funciona totalmente offline.

## Instalar como APK

### Compilar no GitHub Actions (sem instalar o Android Studio)

1. Crie um repositório **privado** no GitHub para este app e carregue **o conteúdo** desta pasta na branch `main` (não apenas o ZIP sem descompactar).
2. Abra **Actions → Build APK offline-first**.
3. Dispare **Run workflow** ou faça um novo commit em `main`.
4. Aguarde a execução terminar. Baixe o artifact **felomenau-tshirts-apk** da página da execução.
5. Descompacte o artifact e instale `app-debug.apk` no tablet Android permitindo a instalação de apps do navegador/arquivos.

A rotina `.github/workflows/android.yml` instala Java/Gradle, executa os testes JS e chama `gradle assembleDebug`. O nome do pacote é `com.felomenau.tshirts`, Android mínimo 8.0 (API 26), compile/target SDK 35.

**Atenção para atualizações:** o APK *debug* gerado no GitHub pode usar uma chave de assinatura diferente a cada compilação. Para manter atualizações futuras sem reinstalação e evitar perda de dados, configure assinatura fixa de release com keystore mantida em segurança. **Faça backup antes de trocar/reinstalar o APK**. Não desinstale um aplicativo com dados sem ter exportado uma cópia.

### Compilar no Android Studio

Importe a pasta do projeto, configure Android SDK 35, Gradle 8.9/Android Gradle Plugin 8.7.3 e JDK 17. Execute **Build → Build APK(s)**.

## Dados e cálculos

- Banco `felomenau.db`, versão do esquema 2; migração não destrutiva da V1.2 ao acrescentar `category` com padrão `Camiseta`.
- O preço de custo fica congelado em `sale_items.unit_cost` na venda; preço realmente praticado também é preservado na venda.
- `Sobra bruta estimada = total da venda − custo histórico das unidades − taxa informada`.
- Descontos diminuem o total recebido, taxas reduzem a sobra, mas **não** são adicionadas ao preço final.
- Impostos, frete, aluguel e despesas não cadastradas não entram no resultado. Pix e cartão não cobram o cliente automaticamente: a forma de pagamento é **apenas registrada**.
- Vendas canceladas não entram nos rankings, e correções manuais de estoque nunca viram vendas.
- Arquivar um produto preserva o histórico e impede vendas novas.

## Testes

Execute no computador (Node.js 18+):

```bash
node tests/filter-search.test.js
node tests/sales-order.test.js
node tests/checkout.test.js
```

Os testes cobrem buscas, variações customizadas, categorias, filtros, mais/menos vendidos, itens esgotados, carrinho, revisão e validação de estoque/pagamento.

## Arquivos

- `app/src/main/assets/index.html`: página local.
- `app/src/main/assets/app.css`: identidade visual para tablet e celular.
- `app/src/main/assets/app.js`: lógica de telas, catálogo, carrinho e relatórios.
- `app/src/main/java/com/felomenau/tshirts/MainActivity.java`: integração WebView/SQLite, fotos e backups.
- `.github/workflows/android.yml`: compilação automática.

## Limitações atuais

O projeto foi testado logicamente em Node e com uma simulação de interface em Chromium nas resoluções de tablet/celular. **O APK não pode ser gerado neste contêiner sem o Android SDK e as dependências de build (rede externa indisponível).** Até conseguir rodar o workflow no GitHub ou Android Studio, o instalador não estará disponível. A V1.3 não possui operação de cobrança Pix real, sincronização entre tablets nem emissão de nota fiscal.
