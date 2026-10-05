# Assets do Solar Client

O client nao depende de nenhum asset externo para funcionar: se os arquivos
faltarem, ele cai para a fonte do sistema e nao desenha a textura do cosmetico
(sem crash). Mas para o visual ficar completo, adicione aqui:

```
src/main/resources/assets/solarclient/
├── fonts/
│   ├── Sora.ttf          -> fonte da UI (SIL Open Font License 1.1)
│   └── Sora-Bold.ttf     -> versao bold (titulos, botoes)
└── textures/
    └── cosmetics/
        └── capes/
            ├── solar_cape.png   (64x32, com alpha)
            ├── ember_cape.png
            └── frost_cape.png
```

## Fontes

A fonte e rasterizada uma vez pelo AWT e vira um atlas de glifos
(`com.solarclient.ui.SolarFont`). Formatos aceitos: `.ttf` (e `.otf`, que o
AWT tambem le).

Sugestoes com licenca livre:

| Fonte | Licenca | Observacao |
|-------|---------|------------|
| [Sora](https://fonts.google.com/specimen/Sora) | OFL 1.1 | padrao do projeto, bem redonda |
| [Inter](https://fonts.google.com/specimen/Inter) | OFL 1.1 | alternativa mais neutra |
| [Product Sans](https://github.com/ProductSans/ProductSans) | Apache 2.0 | uso pessoal/estudo |

Se quiser trocar a fonte padrao, mude a chamada `SolarFont.get()` em
`com.solarclient.ui.SolarFont` (ou passe o nome da familia).

## Texturas de cosmetico

* **Capas / mantos**: 64x32 (mesmo formato do vanilla) - qualquer PNG com alpha.
* **Asas / chapeu / oculos**: 128x128 ou 256x256, com alpha.
* O carregamento e por caminho: `textures/cosmetics/capes/solar_cape.png`
  corresponde a `TextureUtil.load("cosmetics/capes/solar_cape.png")`.

Para adicionar um cosmético novo, crie o PNG e registre a textura em
`com.solarclient.cosmetics.AnimatedCape` (ou crie uma nova classe de
cosmético e registre em `CosmeticManager#buildCatalog`).
