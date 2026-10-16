# Assembly Voting API

API REST para pautas e votações de assembleias. Um associado válido pode votar uma vez por pauta, enquanto a sessão correspondente estiver aberta. A duração padrão é de um minuto.

## Stack e versões

- Java 25 LTS, Spring Boot 3.5.16 e Maven Wrapper 3.9.15.
- Spring MVC, Spring Data JPA, Bean Validation, Springdoc/OpenAPI e Actuator.
- PostgreSQL 17 com Flyway; H2 é usado nos testes automatizados.
- RabbitMQ 4 para publicação de resultados.
- JUnit 5, Mockito e Spring Boot Test.

## Executar localmente

Requisitos: Docker Compose e, para executar Maven fora do container, JDK 25.

1. Crie um arquivo `.env` a partir de `.env.example`.
2. Substitua as duas senhas de exemplo por valores locais fortes e exclusivos. Não versione o `.env`.
3. Na pasta `assembly`, inicialize a aplicação e seus serviços:

   ```powershell
   docker compose -f docker/docker-compose.yml up --build
   ```

4. Edite a configuração `sdui/mobile-screens` no Consul em `http://localhost:8500` ou publique o JSON inicial:

   ```powershell
   curl.exe --request PUT --data-binary "@src/main/resources/sdui/mobile-screens.json" "http://localhost:8500/v1/kv/sdui/mobile-screens"
   ```

   A aplicação verifica mudanças a cada 5 segundos e passa a servir a última configuração válida sem reiniciar. A interface Swagger fica em `http://localhost:8080/swagger-ui/index.html`; o health check está em `/actuator/health`.

Para executar apenas a suíte automatizada:

```powershell
.\mvnw.cmd clean test
```

Os testes usam H2 em memória e não exigem que PostgreSQL ou RabbitMQ estejam instalados localmente. A suíte não substitui um teste de integração das migrations em PostgreSQL.

## API

Os endpoints de negócio ficam exclusivamente em `/api/v1`. Além do fluxo de votação, estão disponíveis consultas paginadas e busca por identificador. A atualização é permitida somente para pautas.

### Criar pauta e abrir votação

```http
POST /api/v1/surveys
Content-Type: application/json

{"question":"Aprovar a proposta?"}
```

A criação da pauta não abre uma votação. Abra uma sessão explicitamente; omitir `minutes` usa o padrão configurado (um minuto):

```http
POST /api/v1/surveys/1/sessions
Content-Type: application/json

{"minutes":10}
```

### Associados, votos e resultados

O cadastro de associado fornece o identificador usado na votação e permite a integração opcional de elegibilidade por CPF:

```http
POST /api/v1/electors
Content-Type: application/json

{"cpf":"679.530.080-33"}
```

O request aceita somente o CPF, sem pontos ou traço. O CPF é validado e normalizado para 11 dígitos antes de persistir. A resposta inclui o status de elegibilidade calculado pelo servidor (`ABLE_TO_VOTE` ou `UNABLE_TO_VOTE`); o cliente não pode definir esse status. Quando a integração externa está desabilitada (`ELIGIBILITY_ENABLED=false`), o modo local considera o associado apto. Habilite a integração para consultar a elegibilidade pelo CPF.

```http
POST /api/v1/votes
Content-Type: application/json

{"surveyId":1,"electorId":1,"approval":"SIM"}
```

`approval` é um enum de contrato e aceita somente `SIM` ou `NAO`, sem acentuação. Valores como `NÃO`, `true`, `false` ou `MAYBE` são rejeitados com HTTP 400. A persistência usa booleano internamente (`SIM=true`, `NAO=false`) sem expor essa conversão no contrato de entrada. Um segundo voto do mesmo associado para a mesma pauta retorna `409 Conflict`, inclusive em solicitações concorrentes.

### Resultados e telas móveis

- `GET /api/v1/results/{surveyId}` contabiliza e retorna os votos de uma pauta.
- `GET /api/v1/mobile/screens` publica o contrato do Anexo 1, com telas `FORMULARIO` e `SELECAO`.

O JSON mobile segue os nomes definidos no anexo:

- `FORMULARIO`: `tipo`, `titulo`, `itens`, `botaoOk` e `botaoCancelar`;
- itens estáticos: `tipo: "TEXTO"` e `texto`; campos: `tipo`, `id`, `titulo` e `valor`;
- ações: `texto`, `url` e, quando aplicável, `body`;
- `SELECAO`: cada item contém `texto`, `url` e `body`.

As definições SDUI ativas são lidas do Consul KV, na chave `sdui/mobile-screens`, e atualizadas sem reiniciar a aplicação. O valor pode ser um único objeto de tela ou uma lista de telas. `src/main/resources/sdui/mobile-screens.json` é a configuração inicial/de fallback; `app.mobile.screens-location` escolhe o arquivo. Itens de formulário são mantidos na ordem do array e propriedades adicionais de cada item são repassadas ao cliente, permitindo evoluir o payload sem alterar o backend. Cada item deve ter um `tipo`; itens `TEXTO` também devem ter `texto`. `SELECAO` usa itens com `texto` e `url`; outros tipos de tela são tratados como formulário e seu `tipo` é preservado na resposta. O parser também aceita vírgulas finais no JSON. O serviço mantém a última configuração válida em memória: falhas de conexão ou configuração inválida não substituem o conteúdo em uso. O Consul local do Compose está em modo de desenvolvimento; em produção use um cluster persistente, ACL/token e TLS, configurando `MOBILE_CONSUL_ADDRESS` e `MOBILE_CONSUL_TOKEN`. `app.mobile.survey-limit` controla o máximo de pautas recentes na tela de seleção (`MOBILE_SURVEY_LIMIT`).

O endpoint de telas de seleção gera para cada pauta uma ação GET para consultar seu resultado em `/api/v1/results/{surveyId}`, substituindo `{surveyId}` pelo identificador da pauta. O cliente adiciona os valores dos campos de entrada ao `body` do `botaoOk` conforme o usuário interage; ao abrir uma sessão pela tela SDUI, deve substituir `{surveyId}` na URL `/api/v1/surveys/{surveyId}/sessions` pelo campo `surveyId` e enviar `minutes` no corpo. As rotas de criação, abertura de sessão e votação recebem esses dados por POST. Os campos de formulário aceitam `TEXTO`, `INPUT_TEXTO`, `INPUT_NUMERO` e `INPUT_DATA`; campos de seleção também podem declarar opções no valor. A resposta da criação de pauta e da abertura de sessão já contém os identificadores e dados necessários para as próximas chamadas.

O resultado contém pauta, votos favoráveis, contrários, total, situação (`IN_PROGRESS`/`FINISHED`) e resultado (`Aprovado`, `Reprovado` ou `Empate`).

### Endpoints de negócio

| Método | Rota | Finalidade |
| --- | --- | --- |
| `GET` | `/api/v1/mobile/screens` | Obter as definições JSON das telas e pautas disponíveis |
| `GET` | `/api/v1/electors` | Listar associados com paginação opcionalmente filtrados por `cpf` |
| `GET` | `/api/v1/electors/{id}` | Consultar associado |
| `POST` | `/api/v1/surveys` | Cadastrar pauta; descrições já cadastradas retornam `409 Conflict` |
| `POST` | `/api/v1/surveys/{surveyId}/sessions` | Abrir a primeira sessão da pauta; duração omitida usa um minuto. Pautas com votação aberta ou já encerrada retornam `409 Conflict`; crie uma nova pauta para uma nova votação |
| `POST` | `/api/v1/electors` | Recebe somente o CPF, cadastra o associado e retorna sua situação de elegibilidade |
| `POST` | `/api/v1/votes` | Registrar um voto `SIM` ou `NAO` por associado e pauta |
| `GET` | `/api/v1/surveys` | Listar pautas com paginação |
| `GET` | `/api/v1/surveys/{surveyId}` | Consultar pauta |
| `GET` | `/api/v1/votes` | Listar votos com paginação |
| `GET` | `/api/v1/votes/{id}` | Consultar voto |
| `GET` | `/api/v1/results` | Consultar resultados de todas as pautas |
| `GET` | `/api/v1/results/{surveyId}` | Consultar resultado de uma pauta |
| `PUT` | `/api/v1/surveys/{surveyId}` | Atualizar `question` e/ou `minutes` somente enquanto a pauta estiver `NOT_STARTED`; o identificador vai na URL. `question` é obrigatório, não pode ficar em branco e tem limite de 500 caracteres; `minutes`, quando informado, deve ser positivo |

As listagens paginadas aceitam os parâmetros Spring Data `page`, `size` e `sort` (por exemplo, `?page=0&size=20&sort=id,desc`). A listagem de associados também aceita `cpf`. As rotas de negócio estão disponíveis somente sob `/api/v1`; os aliases legados e os caminhos redundantes `/index` não são publicados.

Swagger/OpenAPI e `/actuator/health` continuam disponíveis para documentação e operação; não fazem parte do fluxo de negócio.

### Erros

Os erros são retornados em um objeto JSON com `timestamp`, `status`, `error`, `message` e `path`. Validações usam HTTP 400; recursos ausentes usam 404; votos e sessões em conflito usam 409; indisponibilidade do serviço externo de elegibilidade usa 503. Detalhes internos e stack traces não são enviados ao cliente.

## Configuração

As configurações de conexão e runtime são externalizadas:

| Variável | Padrão | Uso |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Porta HTTP |
| `DB_URL` | PostgreSQL em `localhost:5441/assembly` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `assembly` / obrigatório | Credenciais do banco; forneça via ambiente ou `.env` |
| `APP_TIME_ZONE` | `America/Sao_Paulo` | Conversão da data legada da pauta |
| `DEFAULT_VOTING_DURATION_MINUTES` | `1` | Duração padrão da sessão |
| `VOTING_EXPIRATION_CHECK_DELAY_MS` | `60000` | Intervalo, em milissegundos, para verificar e fechar sessões expiradas |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | Broker |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `assembly` / obrigatório | Credenciais do broker; forneça via ambiente ou `.env` |
| `ELIGIBILITY_ENABLED` | `false` | Habilita consulta externa de elegibilidade |
| `ELIGIBILITY_SERVICE_URL` | `https://user-info.herokuapp.com` | Base URL do serviço externo |
| `ELIGIBILITY_CONNECT_TIMEOUT` / `ELIGIBILITY_READ_TIMEOUT` | `PT2S` / `PT3S` | Timeouts da consulta externa |
| `MOBILE_CONSUL_ENABLED` | `true` | Habilita atualização SDUI pelo Consul KV; defina `false` para usar somente o fallback local |
| `MOBILE_CONSUL_ADDRESS` | `http://localhost:8500` | Endereço da API HTTP do Consul |
| `MOBILE_CONSUL_KEY` | `sdui/mobile-screens` | Chave KV que contém o JSON das telas |
| `MOBILE_CONSUL_TOKEN` | vazio | Token ACL enviado no header `X-Consul-Token` |
| `MOBILE_CONSUL_POLL_INTERVAL_MS` | `5000` | Intervalo de leitura da configuração remota |
| `MOBILE_CONSUL_CONNECT_TIMEOUT` / `MOBILE_CONSUL_READ_TIMEOUT` | `PT2S` / `PT3S` | Timeouts das chamadas ao Consul |
| `MOBILE_SURVEY_LIMIT` | `100` | Máximo de pautas recentes incluídas na tela de seleção |

Para desenvolvimento local fora do Compose, configure credenciais não vazias para PostgreSQL e RabbitMQ no ambiente. Quando habilitada, a integração externa consulta `GET /users/{cpf}` e interpreta status de elegibilidade em texto simples ou JSON. Erros e respostas inválidas são reportados explicitamente; o comportamento não muda silenciosamente para elegível.

## Banco e migrações

Flyway mantém o schema. Em banco vazio, `V1` cria as tabelas, `V2` aplica a integridade e índices, `V3` indexa a busca de sessões expiradas, `V4` cria o outbox de resultados e `V5` marca pautas antigas sem sessão como não iniciadas; em banco existente não versionado, `baseline-on-migrate` usa a versão 1 e aplica as migrations seguintes.

`V2` cria a tabela de sessões, migra os prazos legados e impõe unicidade do CPF, unicidade do voto por associado/pauta, referências válidas e apenas uma sessão aberta por pauta. Se houver dados duplicados, nulos ou órfãos que violem as novas regras, a migration falha com uma mensagem específica em vez de remover ou alterar esses dados automaticamente. Corrija e faça backup dos dados antes de reaplicar.

Flyway é a fonte única para os índices e restrições nomeados do schema; eles não são duplicados como metadados de DDL nas entidades JPA. As migrations contêm recursos específicos de PostgreSQL (índice parcial e `FOR UPDATE SKIP LOCKED`). A configuração H2 dos testes desabilita Flyway e declara os índices relevantes em `src/test/resources/import.sql`; valide migrations em uma instância PostgreSQL antes de atualizar bancos compartilhados ou de produção.

`src/main/resources/inserts.txt` contém dados sintéticos para desenvolvimento (10 associados, 10 pautas e votos de exemplo). Execute apenas após as migrations e nunca use os CPFs de exemplo como dados reais.

## Operação e observabilidade

- `X-Correlation-ID` é propagado/gerado por requisição e incluído no padrão de log.
- Actuator expõe `/actuator/health`, probes de prontidão/liveness e `/actuator/prometheus`.
- O CPF é normalizado no domínio e não deve ser usado como identificador em logs.
- O Compose inicia PostgreSQL, RabbitMQ e aplicação com health checks, dependências ordenadas e volume persistente para o banco.
- O arquivo `.env` é excluído do contexto Docker e deve permanecer fora do controle de versão.

## Arquitetura e organização de pacotes

O projeto utiliza um **monólito modular com arquitetura hexagonal leve**. Esta combinação é a mais adequada para o escopo atual: mantém a simplicidade de uma API CRUD, mas separa as regras de votação, sessão, elegibilidade e mensageria para permitir evolução e testes sem acoplamento desnecessário.

| Pacote | Responsabilidade |
| --- | --- |
| `resource` | Adaptadores de entrada HTTP e contrato REST |
| `service.elector` | Casos de uso e regras de associados |
| `service.survey` | Casos de uso de pautas |
| `service.session` | Abertura, validade e fechamento de sessões |
| `service.vote` | Registro, atualização, validação e consulta de votos |
| `service.result` | Cálculo e consulta de resultados |
| `service.outbox` | Persistência e publicação de eventos pendentes |
| `service.scheduler` | Orquestração periódica do fechamento e publicação |
| `integration` e `event` | Adaptadores para serviços externos e RabbitMQ |
| `repository` | Portas de persistência Spring Data |
| `entity` e `dto` | Modelo persistido e contratos de transporte |
| `config` e `util` | Configuração e componentes compartilhados |

MVC continua sendo utilizado na borda HTTP, mas não é a arquitetura dominante do domínio. Clean Architecture e Hexagonal são princípios de organização; neste projeto foram aplicados de forma pragmática, sem criar múltiplos módulos Maven ou abstrações para cada classe.

## Atendimento dos requisitos

| Requisito | Atendimento |
| --- | --- |
| Cadastrar pauta | `POST /api/v1/surveys`, criada como `NOT_STARTED` até a abertura da sessão; persistência PostgreSQL via JPA/Flyway |
| Abrir sessão com duração informada ou um minuto por padrão | `POST /api/v1/surveys/{surveyId}/sessions`, com `DEFAULT_VOTING_DURATION_MINUTES=1` |
| Persistir pautas, sessões e votos após restart | PostgreSQL persistente no Compose, migrations Flyway e `ddl-auto=validate` |
| Voto `Sim`/`Não` e um voto por associado/pauta | Enum de entrada aceita somente `SIM`/`NAO`; índice único PostgreSQL garante concorrência |
| Contabilizar e apresentar resultado | Resultado por pauta com `Aprovado`, `Reprovado`, `Empate` e totais |
| Comunicação JSON para aplicação móvel | DTOs JSON versionados e `GET /api/v1/mobile/screens` com campos, seleções e ações |
| Integração de elegibilidade por CPF | Cliente configurável; padrão `https://user-info.herokuapp.com/users/{cpf}`, timeouts e tratamento 404/503/502 |
| Performance para grande volume | Consultas agregadas de votos e índices para CPF, votos, status e sessões |
| Versionamento | Todos os contratos de negócio usam `/api/v1` |
| Tratamento de erros e observabilidade | JSON de erro padronizado, correlation ID, Actuator, logs sem CPF e health checks |
| Testes automatizados | `mvn clean verify` com 59 testes aprovados, incluindo contrato móvel, concorrência e outbox |

## Limitações e próximos passos

- A validação real executada com Docker confirmou PostgreSQL 17.11, Flyway V1/V2, todos os índices, startup da aplicação e o fluxo completo de voto. O teste de concorrência automatizado ainda usa H2; recomenda-se adicionar Testcontainers PostgreSQL para reproduzir concorrência no mesmo banco de produção.
- A integração de elegibilidade é opcional e desativada por padrão para permitir execução local sem dependência externa. Quando habilitada, uma falha nunca é convertida silenciosamente em “pode votar”.
- Autenticação/autorização e limitação de taxa não fazem parte do exercício, conforme a especificação, e devem ser adicionadas antes de exposição pública.
- O outbox transacional evita duplicidade de eventos criados por múltiplas réplicas e permite retry de publicação. Consumidores externos devem deduplicar pelo `messageId` para completar a garantia at-least-once.

## Documentação adicional

- [Decisões, trade-offs e evolução](docs/architecture-decisions.md)
