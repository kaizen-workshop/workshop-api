# AGENTS.md

## Projeto

Nome: `workshop_api`

API backend do sistema de workshops da ARWEG, desenvolvida com Spring Boot.

Este projeto será desenvolvido com forte uso de agentes de IA e vibe coding.  
Por isso, este arquivo define regras obrigatórias para qualquer agente que altere o código.

O objetivo principal não é gerar código rapidamente.  
O objetivo é gerar código correto, simples, testável, rastreável e coerente com os requisitos do projeto.

---

# 1. Fonte de verdade

Antes de implementar qualquer funcionalidade, considere esta ordem de prioridade:

1. `TASKS.md`
2. Documentação funcional do projeto
3. Regras descritas no `README.md`
4. Código e testes existentes
5. Este `AGENTS.md`

Se houver conflito entre uma task e o código existente, não adapte silenciosamente a regra de negócio ao código antigo.

Pare, identifique o conflito e preserve a regra definida na documentação mais recente.

Não invente requisitos.

---

# 2. Stack principal

Utilizar preferencialmente:

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Security
- Bean Validation
- PostgreSQL
- Flyway
- JWT
- Spring Mail
- Spring Boot Actuator
- OpenAPI / Swagger
- JUnit 5
- Mockito
- Testcontainers

Não adicionar novas dependências sem necessidade clara.

Antes de adicionar uma biblioteca, verificar se a funcionalidade já pode ser resolvida pela stack existente.

---

# 3. Arquitetura

O projeto deve iniciar como um monólito modular.

Não criar microserviços.

Não criar comunicação distribuída sem requisito explícito.

Organizar o código por domínio sempre que possível.

Estrutura recomendada:

```text
br.com.weg.workshop
├── auth
├── user
├── preference
├── workshop
├── registration
├── payment
├── feed
├── post
├── group
├── chat
├── notification
├── evaluation
├── audit
├── file
├── shared
└── config
```

Dentro de cada módulo, separar responsabilidades quando fizer sentido:

```text
controller
service
repository
domain
dto
mapper
validation
```

Evitar um pacote global gigante contendo todos os controllers, services e repositories do sistema.

---

# 4. Regra de simplicidade

Sempre preferir a solução mais simples que cumpra corretamente o requisito.

Evitar:

- abstrações prematuras;
- heranças desnecessárias;
- factories sem necessidade;
- interfaces com apenas uma implementação sem motivo arquitetural;
- padrões de projeto usados apenas para parecer sofisticado;
- reflection sem necessidade;
- event bus interno para operações simples;
- wrappers de wrappers;
- classes utilitárias genéricas sem domínio claro.

Não criar uma arquitetura para um problema que ainda não existe.

---

# 5. Regras de implementação

Antes de alterar código:

1. Ler a task correspondente em `TASKS.md`.
2. Localizar os arquivos relacionados.
3. Identificar regras de negócio afetadas.
4. Verificar testes existentes.
5. Planejar a menor alteração capaz de cumprir a task.

Durante a implementação:

- alterar somente o necessário;
- preservar comportamento não relacionado;
- evitar refactors grandes junto com novas features;
- manter nomes coerentes com o domínio;
- escrever código legível antes de tentar escrever código "esperto";
- não duplicar regras de negócio em controllers;
- não acessar repository diretamente de controller;
- não expor entidades JPA diretamente na API;
- utilizar DTOs para requests e responses;
- validar entrada na borda da aplicação;
- manter regras de domínio nos services apropriados.

Depois da implementação:

1. Compilar.
2. Executar testes.
3. Adicionar testes da nova regra.
4. Verificar migrations.
5. Verificar contrato OpenAPI quando o endpoint mudar.
6. Atualizar a task quando apropriado.

---

# 6. Convenções REST

Base da API:

```text
/api/v1
```

Utilizar substantivos no plural.

Exemplos:

```text
GET    /api/v1/workshops
GET    /api/v1/workshops/{id}
POST   /api/v1/workshops
PUT    /api/v1/workshops/{id}
PATCH  /api/v1/workshops/{id}
```

Ações de domínio podem usar sub-recursos ou comandos explícitos quando isso deixar a intenção mais clara:

```text
PATCH /api/v1/workshops/{id}/publish
PATCH /api/v1/workshops/{id}/cancel
PATCH /api/v1/workshops/{id}/archive
```

Não criar endpoints como:

```text
POST /doCancelWorkshop
POST /getAllWorkshops
```

---

# 7. Status HTTP

Usar status HTTP coerentes.

Referência:

```text
200 OK
201 Created
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
500 Internal Server Error
```

Utilizar `409 Conflict` para conflitos de estado ou duplicidade quando apropriado.

Não retornar `200 OK` para tudo.

---

# 8. Tratamento de erros

A API deve possuir formato padronizado de erro.

Exemplo:

```json
{
  "timestamp": "2026-09-11T15:42:00Z",
  "status": 400,
  "code": "WORKSHOP_INVALID_PERIOD",
  "message": "A data final deve ser posterior à data inicial.",
  "path": "/api/v1/workshops",
  "errors": []
}
```

Nunca retornar:

- stack trace;
- SQL;
- segredo;
- token;
- senha;
- detalhes internos de infraestrutura.

Criar códigos de erro estáveis quando o cliente mobile puder depender deles.

---

# 9. Banco de dados

Banco principal: PostgreSQL.

IDs principais devem utilizar UUID.

Toda alteração de schema deve ser feita via Flyway.

Nunca depender de:

```properties
spring.jpa.hibernate.ddl-auto=update
```

em ambientes reais.

Preferir:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

quando apropriado.

Toda migration deve ser imutável após aplicada.

Se uma migration publicada estiver errada, criar uma nova migration corretiva.

---

# 10. Entidades JPA

Não expor entidade JPA diretamente pelo controller.

Evitar entidades gigantes com responsabilidade demais.

Relacionamentos devem ser definidos com cuidado.

Evitar `EAGER` por padrão.

Preferir carregamento explícito quando necessário.

Não utilizar `CascadeType.ALL` automaticamente em todos os relacionamentos.

Antes de adicionar cascade, verificar o impacto em exclusão e persistência.

---

# 11. Transações

Operações críticas devem ser transacionais.

Exemplos:

- inscrição;
- cancelamento;
- promoção da lista de espera;
- confirmação de pagamento;
- reembolso;
- criação de workshop e grupo associado;
- cancelamento de workshop e encerramento de recursos relacionados.

Não utilizar `@Transactional` como curativo universal.

Definir claramente a fronteira transacional.

---

# 12. Concorrência

A aplicação deve impedir inconsistências em vagas.

Cenário obrigatório:

```text
Workshop possui 1 vaga.
10 usuários tentam se inscrever simultaneamente.
Somente 1 inscrição pode ocupar a última vaga.
```

Implementar estratégia explícita de concorrência.

Pode envolver:

- locking pessimista;
- optimistic locking;
- constraints;
- transações.

A estratégia escolhida deve possuir teste.

---

# 13. Autenticação

O login deve aceitar:

- username;
- e-mail.

O usuário utiliza senha própria da aplicação.

Fluxo inicial:

```text
Administrador cria usuário
→ sistema gera senha temporária
→ senha temporária é enviada por e-mail
→ usuário realiza login
→ sistema exige troca de senha
→ usuário define senha definitiva
```

Nunca armazenar senha em texto puro.

Nunca registrar senha em logs.

Utilizar algoritmo apropriado de hash, preferencialmente BCrypt ou Argon2.

---

# 14. Autorização

Papéis iniciais:

```text
PARTICIPANT
ARWEG
ADMIN
```

Regras gerais:

```text
PARTICIPANT
- consulta conteúdo;
- gerencia a própria conta;
- realiza e cancela inscrição;
- interage no feed;
- participa dos grupos em que possui acesso;
- avalia workshops elegíveis.

ARWEG
- possui permissões do participante quando aplicável;
- cria e gerencia workshops;
- cria e gerencia posts;
- gerencia participantes;
- modera grupos;
- consulta avaliações e métricas;
- envia comunicações.

ADMIN
- gerencia usuários;
- possui funções administrativas de sistema;
- pode possuir permissões ARWEG conforme regra definida.
```

Nunca confiar somente na interface mobile para restringir ações.

A autorização deve existir na API.

---

# 15. Workshops

Estados previstos:

```text
DRAFT
SCHEDULED
PUBLISHED
CLOSED
CANCELLED
ARCHIVED
```

Não permitir transições de estado arbitrárias.

Criar regras explícitas de transição.

Exemplo:

```text
DRAFT -> PUBLISHED
DRAFT -> SCHEDULED
SCHEDULED -> PUBLISHED
PUBLISHED -> CANCELLED
PUBLISHED -> CLOSED
CLOSED -> ARCHIVED
```

A lista real de transições deve permanecer documentada e testada.

---

# 16. Inscrições

Uma pessoa não pode possuir duas inscrições válidas para o mesmo workshop.

Garantir isso também no banco quando possível.

Antes de confirmar inscrição, validar:

- workshop;
- status;
- período de inscrição;
- capacidade;
- inscrição anterior;
- requisitos;
- pagamento quando aplicável.

A operação precisa ser segura contra concorrência.

---

# 17. Lista de espera

Quando não houver vagas, o usuário poderá entrar na lista de espera quando essa funcionalidade estiver habilitada.

A ordem deve ser preservada.

Quando uma vaga for liberada, a promoção deve seguir a regra definida para o workshop.

Não reordenar usuários silenciosamente.

---

# 18. Pagamentos e reembolsos

A regra de negócio não deve depender diretamente de um gateway específico.

Utilizar abstração clara para integração externa.

Estados iniciais:

```text
PENDING
PAID
DECLINED
CANCELLED
REFUNDED
EXEMPT
```

Toda alteração relevante de pagamento precisa ser rastreável.

Operações financeiras devem ser idempotentes quando houver risco de repetição.

---

# 19. Feed e posts

Somente perfis autorizados da ARWEG podem publicar posts.

Participantes podem:

- visualizar;
- curtir;
- comentar;
- acessar workshop relacionado.

O feed deve ser paginado.

Para mobile, preferir cursor quando houver ganho real de consistência.

O feed inicialmente deve usar regras determinísticas.

Não implementar recomendação baseada em IA sem requisito específico.

---

# 20. Grupos

Regra inicial do projeto:

```text
1 Workshop -> 1 Grupo
```

O grupo pertence ao workshop.

O acesso ao grupo depende da participação válida no workshop conforme regra de inscrição/pagamento.

Quando o workshop for cancelado ou encerrado conforme a regra de ciclo de vida, o grupo deve ser encerrado ou removido de forma consistente.

Não criar múltiplos grupos por workshop sem nova definição funcional.

---

# 21. Chat

Somente integrantes autorizados podem acessar mensagens do grupo.

As mensagens devem ser paginadas.

Para histórico de chat, preferir cursor.

O sistema deve prever moderação por usuários autorizados.

Não retornar todas as mensagens de um grupo em uma única resposta.

---

# 22. Avaliações

Somente usuários elegíveis podem avaliar um workshop.

Uma avaliação deve estar associada ao usuário e workshop.

Evitar múltiplas avaliações do mesmo usuário para o mesmo workshop, salvo nova regra explícita.

---

# 23. Arquivos

Não armazenar imagens ou anexos grandes diretamente como Base64 ou `byte[]` no banco sem motivo explícito.

Utilizar abstração de armazenamento.

O banco deve guardar metadados e chave/localização.

Validar:

- tamanho;
- MIME type;
- extensão;
- integridade.

Nunca confiar apenas no nome do arquivo.

---

# 24. Mobile e offline

A API será consumida principalmente por aplicação mobile.

Projetar endpoints levando em conta:

- conexão instável;
- repetição de requests;
- sincronização;
- paginação;
- payload;
- cache;
- idempotência.

Recursos sincronizáveis devem possuir campos temporais apropriados, como:

```text
createdAt
updatedAt
```

Quando necessário, disponibilizar filtros incrementais:

```text
updatedAfter
```

Operações críticas poderão utilizar:

```http
Idempotency-Key: <uuid>
```

---

# 25. Logs

Logs devem ajudar diagnóstico sem vazar dados.

Incluir quando possível:

- requestId;
- userId;
- endpoint;
- status;
- duração;
- evento relevante.

Nunca registrar:

- senha;
- access token;
- refresh token;
- segredo;
- credencial;
- conteúdo financeiro sensível.

---

# 26. Testes

Toda regra de negócio relevante precisa de teste.

Prioridades:

1. unitários para regras;
2. integração para repository e fluxos;
3. segurança;
4. concorrência;
5. contrato.

Utilizar PostgreSQL real via Testcontainers para testes que dependam do comportamento do banco.

Não assumir que H2 representa PostgreSQL.

---

# 27. Testes obrigatórios para bugs

Todo bug corrigido deve possuir teste de regressão sempre que tecnicamente viável.

Fluxo:

```text
reproduzir bug
→ escrever teste que falha
→ corrigir
→ confirmar teste passando
```

---

# 28. OpenAPI

Endpoints públicos da aplicação devem estar documentados.

Documentar:

- finalidade;
- autenticação;
- parâmetros;
- request;
- response;
- status HTTP;
- erros relevantes.

Quando um contrato mudar, atualizar a documentação no mesmo trabalho.

---

# 29. Segurança

Nunca:

- commitar segredo;
- desabilitar Spring Security para resolver teste;
- utilizar `permitAll()` em endpoint protegido apenas para "fazer funcionar";
- confiar em IDs enviados pelo cliente para determinar proprietário sem validar;
- montar SQL manual com input do usuário;
- retornar stack trace;
- armazenar senha em texto puro.

Toda configuração sensível deve vir de ambiente/configuração externa.

---

# 30. Performance

Não otimizar prematuramente.

Mas evitar erros óbvios:

- N+1;
- carregar listas inteiras sem paginação;
- consultas dentro de loops;
- serialização de grafos JPA;
- joins gigantes sem necessidade;
- contagens repetidas em endpoints de feed.

Medir antes de otimizar além disso.

---

# 31. Nomeação

Código deve ser escrito em inglês.

Exemplos:

```text
Workshop
Registration
WaitingList
Payment
Notification
Evaluation
Participant
```

Mensagens de negócio retornadas ao cliente podem seguir o idioma definido pelo produto.

Não misturar nomes como:

```text
WorkshopService
InscricaoRepository
PaymentDTO
```

no mesmo domínio.

---

# 32. Commits

Commits devem ser pequenos e coerentes.

Formato recomendado:

```text
feat: add workshop creation
fix: prevent duplicate registrations
test: add concurrent registration coverage
refactor: simplify payment status transition
docs: update workshop API contract
```

Não misturar várias funcionalidades independentes no mesmo commit.

---

# 33. Regras para agentes de IA

Um agente não deve:

- inventar endpoint;
- inventar campo;
- mudar regra funcional sem autorização;
- remover validação para fazer teste passar;
- alterar migration antiga aplicada;
- criar dependência sem justificar;
- reescrever módulos inteiros sem necessidade;
- fazer refactor massivo durante uma correção pequena;
- ignorar teste quebrado;
- marcar task como concluída sem validar o resultado.

Se algo não estiver especificado e for necessário para continuar:

1. procurar documentação existente;
2. procurar regra semelhante no código;
3. escolher a solução mais conservadora;
4. registrar a suposição claramente.

Não transformar suposição em regra definitiva silenciosamente.

---

# 34. Definition of Done

Uma task só pode ser considerada concluída quando:

- código implementado;
- projeto compilando;
- testes existentes passando;
- novos testes adicionados quando aplicável;
- migration criada quando necessária;
- autorização revisada;
- erros tratados;
- OpenAPI atualizado quando houver mudança de contrato;
- nenhuma credencial adicionada;
- nenhum TODO crítico deixado sem registro;
- comportamento manualmente verificável ou coberto por teste;
- task atualizada no `TASKS.md`.

Código que "parece certo" não é conclusão.

---

# 35. Comando de verificação

O agente deve utilizar o comando correspondente ao gerenciador escolhido.

Maven:

```bash
./mvnw clean verify
```

Gradle:

```bash
./gradlew clean test
```

Antes de declarar uma alteração concluída, executar a suíte aplicável.

---

## Git conventions

### Branch naming

Branches must follow:

```text
<type>/TASK-<id>-<short-description>
```

Allowed types:

```text
feat
fix
refactor
test
docs
chore
perf
hotfix
```

Examples:

```text
feat/TASK-021-create-workshop
fix/TASK-036-registration-race-condition
docs/TASK-098-openapi
```

Use lowercase kebab-case for descriptions.

Branches must represent one task or one cohesive change.

Do not use generic names such as:

```text
test
new-feature
changes
dev
fix
final
```

# 36 Commit messages

Use Conventional Commits:

```text
<type>(<scope>): <description> [TASK-XXX]
```

Examples:

```text
feat(auth): add login with username or email [TASK-010]
feat(workshop): add workshop creation [TASK-021]
fix(registration): prevent concurrent overbooking [TASK-036]
test(auth): add expired token tests [TASK-095]
docs(api): document workshop endpoints [TASK-098]
```

Commit descriptions must:

* use English;
* use imperative wording;
* be concise;
* describe one cohesive change;
* not end with a period.

Allowed commit types:

```text
feat
fix
refactor
test
docs
chore
perf
build
ci
revert
```

### Pull requests

PR titles must follow:

```text
[TASK-XXX] <type>: <description>
```

Example:

```text
[TASK-021] feat: create workshop endpoint
```

Every PR should:

* address one task or cohesive change;
* include tests when applicable;
* pass CI;
* avoid unrelated refactors;
* update documentation when contracts change.

### Main branch

Direct pushes to `main` are forbidden.

Changes must enter through pull requests.

Recommended protections:

* require pull request;
* require CI checks;
* require at least one approval;
* require resolved conversations;
* block force pushes;
* block branch deletion.

### Merge strategy

Prefer Squash Merge.

Development commits may remain granular inside the feature branch, but the final commit merged into `main` should represent the complete task.

Example final commit:

```text
feat(workshop): add workshop creation [TASK-021]
```


# 37. Princípio final

Quando houver duas soluções corretas:

Escolha a que:

1. possui menos complexidade;
2. é mais fácil de testar;
3. deixa a regra de negócio mais explícita;
4. exige menos conhecimento implícito;
5. será mais fácil para outro desenvolvedor ou agente alterar depois.

Este projeto deve continuar compreensível mesmo sendo desenvolvido com forte apoio de IA.

---

# 38. API-DOCS.md

`API-DOCS.md` é a referência complementar sobre as classes e contratos já implementados.
Antes de alterar um módulo documentado, o agente deve lê-lo. Na mesma task, deve atualizá-lo
sempre que criar, remover ou alterar materialmente endpoint, DTO, entidade, repositório,
service, controller, filtro de segurança, migration ou comportamento público.

A atualização deve informar finalidade, acesso, request/response quando aplicável e estado
real do trabalho. Funcionalidades incompletas devem estar marcadas como `em andamento`; nunca
devem ser documentadas como disponíveis.
