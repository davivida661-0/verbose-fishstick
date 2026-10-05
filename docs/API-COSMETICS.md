# API de cosméticos (REST)

API mínima, sem framework, que o `:api` sobe com o `com.sun.net.httpserver` do
Java 8 + SQLite. Ela guarda **o que o jogador desbloqueou** e **o que está
equipado**, por UUID.

```bash
gradle :api:run                       # http://localhost:8787
# ou
java -cp "api/build/libs/*:gson-2.8.9.jar:sqlite-jdbc.jar" \
     com.solarclient.api.CosmeticsApiServer
```

Variáveis:

| Variável | Padrão | O que faz |
|----------|--------|-----------|
| `SOLAR_API_PORT` | `8787` | porta |
| `SOLAR_API_KEY` | *(vazio)* | se definida, exige header `X-Api-Key` (**sempre defina em produção**) |

Sem `SOLAR_API_KEY` a API sobe **aberta** — prático para desenvolvimento,
inaceitável para produção. O servidor avisa isso no console.

---

## Rotas

### `GET /v1/health`

```json
{ "status": "ok" }
```

### `GET /v1/cosmetics/catalog`

Catálogo completo (o mod também tem um catálogo local para funcionar offline).

```json
[
  { "id": "solar_cape", "type": "CAPE", "rarity": "LEGENDARY",
    "name": "Capa Solar", "texture": "cosmetics/capes/solar_cape.png" }
]
```

### `GET /v1/cosmetics/owned?uuid=<uuid>`

O que o jogador desbloqueou. O UUID pode vir com ou sem traços.

```json
[
  { "id": "solar_cape", "rarity": "LEGENDARY", "type": "CAPE" },
  { "id": "frost_cape", "rarity": "RARE",     "type": "CAPE" }
]
```

### `GET /v1/cosmetics/loadout?uuid=<uuid>`

```json
{ "uuid": "8-4-...", "slots": { "CAPE": "solar_cape" } }
```

### `POST /v1/loadout`

Salva o equipado (chamar `X-Api-Key`).

```json
{ "uuid": "8-4-...", "slots": { "CAPE": "solar_cape", "HAT": "wizard_hat" } }
```

Resposta: `{"ok":true}`. Enviar `{"CAPE": null}` desequipa.

### `POST /v1/admin/unlock`

Libera um cosmético para um jogador (chamar `X-Api-Key`). É o gancho para
comprar na loja, ganhar por evento, por nível etc.

```json
{ "uuid": "8-4-...", "cosmeticId": "solar_cape" }
```

---

## Como o mod conversa com a API

`client/src/main/java/com/solarclient/cosmetics/CosmeticsApi.java`:

* `GET /v1/cosmetics/loadout` no login e a cada 5 minutos (thread daemon, nunca
  trava o jogo);
* `POST /v1/loadout` quando você equipa algo na GUI;
* tudo é cache local em `solar-client/cosmetics.json`, então **offline o client
  continua mostrando a capa certa**.

Para apontar para outro servidor, use a tela de cosméticos / ajuste a URL no
código (campo `baseUrl`) — o padrão vem de `apiBaseUrl` no `gradle.properties`.

---

## Banco

`api/sql/schema.sql` cria tudo e popula o catálogo inicial:

| Tabela | Colunas |
|--------|---------|
| `cosmetics` | `id` (PK), `type`, `rarity`, `display_name`, `texture` |
| `ownership` | `(uuid, cosmetic_id)` PK, `unlocked_at` |
| `loadout` | `(uuid, type)` PK, `cosmetic_id` |

```bash
sqlite3 data/cosmetics.db < api/sql/schema.sql
```

### Postgres (multi-servidor)

O `Database` é a única classe que fala SQL e as consultas são quase todas ANSI.
Para ir para Postgres, troque a conexão:

```java
connection = DriverManager.getConnection(System.getenv("DATABASE_URL"));
```

e ajuste os tipos (`TEXT` → `UUID`/`TEXT`, `INTEGER` → `BIGINT`,
`INSERT OR IGNORE` → `INSERT ... ON CONFLICT DO NOTHING`). A sugereça de
serviço gerenciado está na conversa do projeto (Postgres na nuvem, com plano
grátis para protótipo) — qualquer um dos dois serve; o schema é o mesmo.

---

## Segurança (leia antes de publicar)

* Defina `SOLAR_API_KEY` e mantenha-a fora do código.
* A API escuta em `0.0.0.0` — coloque atrás de reverse proxy com TLS.
* `uuid` **não é segredo**: com ele dá para ver o loadout de qualquer jogador.
  Se isso for um problema no seu produto, adicione autenticação por token de
  sessão do Minecraft (o launcher já tem o `accessToken`; dá para validar em
  `sessionserver.mojang.com/session/minecraft/hasJoined`).
* O cliente **nunca** envia token para a API: quem fala com ela é o launcher,
  no servidor, com a API key.
