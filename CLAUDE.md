# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The monthly timesheet ("espelho de ponto") from work: the user uploads the PDF of each month and the service stores what it says, so the frontend's "Ponto" tab can show the hour bank, monthly totals, daily detail and occurrences.

| | |
|---|---|
| Port | `9012` |
| Context path | `/ponto-service` |
| Role required | `ROLE_PONTO` |

Part of a personal microservices system; sibling repos live at `../sistema-*`. The API gateway fronts it at `https://sistema-backend.guilhermejr.net/ponto-service`.

## API

- `POST /espelhos` (multipart, field `arquivo`, up to 1 MB) — reads the PDF and saves the month. A month that already exists is **replaced whole** (HR may reissue a corrected one); `ImportacaoResponse.substituido` says so. The month is the one where the period starts.
- `GET /espelhos` — every month, oldest first, each with its totals (`EspelhoResumoResponse`).
- `GET /espelhos/{ano}/{mes}` — totals, days and adjustments (`EspelhoResponse`).
- `DELETE /espelhos/{ano}/{mes}`.

Hours go out as **minutes, signed**; an empty cell is `null`, not `0` (an empty "Horas Compensáveis" and `00:00` mean different things in the PDF). Dates out are ISO. A month of another user answers 404.

## Reading the PDF (`leitor/`)

The PDF comes from the employer's timesheet system (Itaú). Almost every file is encrypted with an owner password only; PDFBox opens it without one.

- **Read by position, never by plain text.** With an empty column the values of a row slide left, so `07:57 01:42` could be "compensáveis" or "acúmulo". `ExtratorPalavras` rebuilds words glyph by glyph (PDFBox hands out runs that cross columns, like `05/09/2026 (sáb) 05:17`) and `LeitorEspelho` puts each word in the column where its **center** falls, between `BORDAS` — the table's vertical lines, identical in every file since 12/2022.
- **Anything unexpected rejects the file** instead of saving a value in the wrong column: a word outside the table, text where hours were expected, a missing day in the period. If the employer changes the layout, uploads start failing with a message — check `BORDAS` against the new file first.
- A wrapped cell lands on its own line within half a row of its day and is attached to it.
- Day rows are `dd/MM/yyyy (sex)`; adjustment rows are `dd/MM/yyyy HH:mm`. "Marcações Incluídas" and "Marcações Descartadas" may sit **side by side** (each section owns the horizontal band from its title onwards) and may continue on the next page without a title (the bands carry over).
- Header fields come from the plain text by regex. Name, CPF and employee number are **not stored**.

`LeitorEspelhoTest` covers the rules with synthetic words at the real coordinates. `LeitorEspelhoArquivosTest` checks the 46 real PDFs (12/2022–09/2026) against an expected JSON built separately with pdfplumber; both live outside the repo because they hold personal data, so the test only runs when given the paths:

```bash
./mvnw test -Dtest=LeitorEspelhoArquivosTest -Dponto.pasta=/path/Ponto -Dponto.esperado=/path/esperado.json
```

## What the numbers mean

Stored exactly as the employer computed them — nothing is recalculated. Things found in the real data:

- **"Acúmulo Compensação" is the hour bank and it resets every semester** (January and July). Within a semester, `acúmulo = previous + compensáveis`, except for rare 1-minute rounding and entries like "Desconta do Saldo de Horas".
- **Tolerance**: differences of a few minutes a day don't become "compensáveis".
- `Resumo` only sums. The one derived number is **dias úteis**: `trabalhadas + abono − compensáveis − não compensáveis` is the daily journey (8h) on a working day and zero on weekends/holidays, worked or not; compared against half a journey because of the tolerance. `previstas = diasUteis × jornada`, the journey coming from "Horário Contratual" (`Horas.jornada`).
- `saldo` of a month is the last day with an "acúmulo" value.

## Database migrations

PostgreSQL with Flyway, migrations in `src/main/resources/db/migration`. `espelhos` (unique per user, year and month) → `dias` and `ajustes`, both `ON DELETE CASCADE`.

`spring-boot-flyway` is declared in `pom.xml` and **must stay there**: in Spring Boot 4 the Flyway auto-configuration lives in that module; with only `flyway-core` the service starts and silently applies nothing.

## Security comes from a library

JWT validation, the filter and `@EnableMethodSecurity` arrive from `net.guilhermejr.sistema:seguranca-jwt` through auto-configuration; `@PreAuthorize` only works because of it.

## Configuration comes from outside

`application.yml` only bootstraps `spring.config.import`:

- **Vault** — `secret/application` (shared) and `secret/ponto-service` (`eurekaHostname`, `pontoDBHost`, `pontoDBBase`, `pontoDBUser`, `pontoDBPass`)
- **Config Server** — `ponto-service/ponto-service.yml` in the config repo: port, context path, datasource

`VAULT_TOKEN` is required and has no default.

## Building and running

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw clean package -DskipTests
VAULT_TOKEN=<token> java -jar target/ponto-service-*.jar --spring.profiles.active=dev
```

`./mvnw test` runs the unit tests (no Spring context, no infrastructure).

## Deploying

`git push origin main` **is** the deploy, through the VPS `post-receive` hook, like the sibling services. The `Dockerfile` only copies a prebuilt jar and has a `HEALTHCHECK` on `/actuator/health`.
