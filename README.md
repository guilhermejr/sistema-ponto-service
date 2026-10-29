# ponto-service

Microserviço do espelho de ponto mensal. Recebe o PDF de cada mês, lê a tabela pela posição das palavras e guarda dias, marcações, ocorrências, abonos e o banco de horas, para a aba **Ponto** do frontend.

| | |
|---|---|
| Porta | `9012` |
| Context path | `/ponto-service` |
| Perfil | `ROLE_PONTO` |

## Endpoints

| Método | Caminho | O que faz |
|---|---|---|
| `POST` | `/espelhos` | Envia o PDF (multipart, campo `arquivo`). Mês que já existe é substituído. |
| `GET` | `/espelhos` | Todos os meses com os totais. |
| `GET` | `/espelhos/{ano}/{mes}` | Um mês com os dias e as marcações incluídas/descartadas. |
| `DELETE` | `/espelhos/{ano}/{mes}` | Apaga o mês. |

Horas em minutos, com sinal; célula vazia vem `null`.

## Rodando

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw clean package -DskipTests
VAULT_TOKEN=<token> java -jar target/ponto-service-*.jar --spring.profiles.active=dev
```

Precisa de `secret/ponto-service` no Vault e de `ponto-service/ponto-service.yml` no repositório de configuração. Detalhes em `CLAUDE.md`.
