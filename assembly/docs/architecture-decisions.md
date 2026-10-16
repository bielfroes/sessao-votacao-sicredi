# Decisões de arquitetura e operação

## Contexto

O serviço administra pautas e votações com uma regra central: cada associado pode registrar no máximo um voto por pauta, e somente enquanto existir uma sessão válida. O objetivo é manter a solução compatível com os clientes legados sem depender apenas de verificações feitas na camada HTTP.

## Componentes

- **API REST**: controllers Spring MVC separados por domínio; novas rotas agrupadas sob `/api/v1`.
- **Serviços**: validações de negócio, controle de sessões, elegibilidade e montagem dos resultados.
- **Persistência**: PostgreSQL via Spring Data JPA e schema versionado com Flyway.
- **Mensageria**: RabbitMQ para eventos/resultados já existentes no fluxo.
- **Observabilidade**: Actuator, Micrometer/Prometheus e identificador de correlação por requisição.

## Decisões e trade-offs

### Unicidade do voto no banco

Uma consulta prévia melhora a mensagem para o caso usual, mas não protege contra duas requisições simultâneas. O índice único `uk_vote_survey_elector` é a garantia final; violações de unicidade são convertidas em conflito HTTP 409. A garantia é local ao PostgreSQL, e o teste H2 não comprova a semântica concorrente exata do banco de produção.

### Sessão de votação como entidade própria

`voting_session` registra abertura, prazo, status e histórico, enquanto `survey` mantém um espelho de status/prazo para compatibilidade com clientes antigos. Uma restrição única parcial permite apenas uma sessão `OPEN` por pauta; o fechamento usa bloqueio de linha com `SKIP LOCKED` para múltiplas instâncias não processarem simultaneamente a mesma sessão expirada.

O espelho legado requer que atualizações futuras de pauta continuem sincronizando os dois registros. Rotas versionadas criam pauta fechada e abrem sessão em uma ação separada; a rota legada preserva a criação com sessão automática.

### Migrations explícitas e conservadoras

Flyway é a fonte única de verdade para índices e restrições nomeados do schema; as entidades JPA descrevem o mapeamento, sem repetir instruções de DDL. A aplicação usa Hibernate em modo `validate`, não para criar ou atualizar o schema. A migration de integridade não apaga dados: ela interrompe a implantação se encontrar duplicatas, referências órfãs ou valores incompatíveis. Isso exige uma etapa operacional de diagnóstico/limpeza com backup em sistemas existentes.

O SQL usa extensões do PostgreSQL, como índices parciais e bloqueios `SKIP LOCKED`. Isso simplifica a garantia de concorrência e o processamento distribuído, mas limita a validação a testes reais em PostgreSQL; H2 é apenas uma conveniência para os testes de unidade/contexto.

### Elegibilidade externa com falha explícita

A integração pode ser ligada por configuração, aplica timeouts e diferencia eleitor inexistente, indisponibilidade e resposta inválida. Ela fica desabilitada por padrão para preservar execução local sem um serviço externo. Não há fallback para “elegível” em caso de falha, pois isso permitiria contornar uma regra de negócio.

### Compatibilidade e contratos

As rotas legadas permanecem disponíveis para evitar migração abrupta dos clientes, mas o contrato de entrada de voto é estrito e aceita somente o enum `SIM`/`NAO`. O descritor de telas móveis segue o formato do Anexo 1 para que o cliente monte a interface sem lógica específica de domínio.

O contrato mobile foi alinhado ao Anexo 1: telas usam `tipo`, `titulo` e `itens`; formulários expõem campos com `tipo`, `id`, `titulo` e `valor`; ações usam `texto`, `url` e `body`; seleções expõem itens acionáveis com a mesma estrutura de ação. A tela de seleção é montada a partir das pautas persistidas para que o aplicativo possa abrir diretamente o resultado escolhido.

### Resultados e custo das consultas

O resultado individual usa uma única consulta agregada para os dois totais. A listagem de resultados busca as pautas e executa uma agregação agrupada por seus IDs, evitando uma consulta por pauta. Índices sobre votação e estado/prazo apoiam as operações mais comuns; medições de latência e volume reais ainda são necessárias antes de ajustar índices adicionais.

### Outbox e idempotência de resultados

O encerramento da sessão e a criação do evento de resultado são realizados na mesma transação. A tabela `voting_result_outbox` possui uma chave única por sessão encerrada, impedindo que múltiplas réplicas criem eventos duplicados. Um processador publica eventos pendentes no RabbitMQ e registra `published_at`; o `event_key` também é enviado como `messageId` para que consumidores implementem deduplicação.

Essa abordagem garante entrega at-least-once: se a aplicação cair depois da publicação e antes da confirmação no outbox, uma retransmissão pode ocorrer. Consumidores devem tratar `messageId` de forma idempotente, persistindo os eventos processados com uma chave única.

### Segurança e dados pessoais

CPF é dado pessoal: os logs de fluxo não registram seu valor e as respostas de erro omitem detalhes internos. Os segredos do Compose vêm de `.env`, ignorado no contexto Docker, e o exemplo contém apenas marcadores. A API não implementa autenticação/autorização nem rate limiting; não deve ser exposta publicamente sem esses controles.

## Verificação

O conjunto de testes executa em Java 25 e usa H2 em memória. Ele verifica regras de serviço, contratos JSON, handlers de erro, proteção única concorrente no mecanismo H2 e idempotência do outbox. A validação real adicional executada com Docker confirmou PostgreSQL 17.11, as migrations V1 a V4, os índices, o startup da aplicação e um fluxo HTTP completo de pauta, sessão, eleitor, voto, conflito e resultado. Para uma cobertura ainda mais próxima de produção, recomenda-se:

1. atualização de uma cópia representativa de schema legado com dados;
2. teste de duas requisições simultâneas no PostgreSQL via Testcontainers;
3. execução periódica do `docker compose up --build` em CI e verificação dos health checks.

O teste de concorrência PostgreSQL via Testcontainers permanece uma melhoria recomendada, não uma pré-condição para a API funcional.