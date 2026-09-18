# TASKS.md

# workshop_api

Backlog técnico inicial da API.

Este arquivo define a ordem recomendada de implementação.

Uma task só deve ser marcada como concluída após atender à Definition of Done descrita em `AGENTS.md`.

Legenda:

```text
[ ] pendente
[~] em andamento
[x] concluída
[!] bloqueada
```

---

# Milestone 0 — Foundation

Objetivo: criar uma base estável para todas as funcionalidades seguintes.

## TASK-001 — Criar projeto Spring Boot

Status:

```text
[x]
```

Implementar:

- Java 21;
- Spring Boot 3.x;
- Maven;
- Spring Web;
- Spring Data JPA;
- Spring Security;
- Bean Validation;
- PostgreSQL Driver;
- Flyway;
- Spring Mail;
- Actuator;
- OpenAPI;
- biblioteca JWT;
- JUnit 5;
- Mockito;
- Testcontainers.

Critérios de aceite:

- aplicação inicia;
- build passa;
- endpoint de health responde;
- estrutura inicial está criada;
- nenhum segredo está no repositório.

---

## TASK-002 — Criar estrutura modular inicial

Status:

```text
[x]
```

Criar módulos:

```text
auth
user
preference
workshop
registration
payment
feed
post
group
chat
notification
evaluation
audit
file
shared
config
```

Critérios de aceite:

- packages compilam;
- não criar classes vazias sem utilidade;
- estrutura documentada no README.

Dependência:

```text
TASK-001
```

---

## TASK-003 — Configurar PostgreSQL

Status:

```text
[x]
```

Implementar:

- datasource;
- profile de desenvolvimento;
- configuração externa;
- Docker Compose para banco local, se adotado.

Critérios de aceite:

- aplicação conecta ao PostgreSQL;
- nenhuma credencial real está commitada;
- configuração pode ser sobrescrita por variáveis de ambiente.

Dependência:

```text
TASK-001
```

---

## TASK-004 — Configurar Flyway

Status:

```text
[x]
```

Implementar:

- diretório de migrations;
- migration inicial;
- validação automática na inicialização.

Critérios de aceite:

- banco vazio inicializa corretamente;
- migration é aplicada automaticamente;
- Hibernate não cria schema de produção automaticamente.

Dependências:

```text
TASK-003
```

---

## TASK-005 — Configurar tratamento global de erros

Status:

```text
[x]
```

Criar:

- modelo de erro;
- exception handler global;
- códigos de erro;
- tratamento de validação.

Contrato base:

```json
{
  "timestamp": "2026-09-11T15:42:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Invalid request.",
  "path": "/api/v1/example",
  "errors": []
}
```

Critérios de aceite:

- stack trace não é exposta;
- erros de validação possuem resposta consistente;
- 404, 400, 401, 403 e 409 possuem tratamento adequado.

---

## TASK-006 — Configurar OpenAPI

Status:

```text
[x]
```

Critérios de aceite:

- Swagger acessível em desenvolvimento;
- autenticação JWT configurada na documentação;
- endpoints futuros podem documentar códigos e contratos.

---

# Milestone 1 — Authentication and Users

Objetivo: criar autenticação e gerenciamento inicial de usuários.

## TASK-007 — Criar domínio de usuário

Status:

```text
[ ]
```

Criar entidade equivalente a:

```text
User
    id
    name
    username
    email
    wegRegistration
    phone
    profileImage
    passwordHash
    role
    status
    mustChangePassword
    lastLoginAt
    createdAt
    updatedAt
```

Enums iniciais:

```text
Role
    PARTICIPANT
    ARWEG
    ADMIN
```

```text
UserStatus
    PENDING
    ACTIVE
    BLOCKED
    INACTIVE
```

Critérios de aceite:

- UUID;
- username único;
- e-mail único;
- migration criada;
- repository testado.

Dependência:

```text
TASK-004
```

---

## TASK-008 — Implementar criação administrativa de usuário

Status:

```text
[ ]
```

Endpoint planejado:

```http
POST /api/v1/admin/users
```

Fluxo:

```text
ADMIN cria usuário
→ senha temporária é gerada
→ hash é persistido
→ mustChangePassword = true
→ e-mail é enviado
```

Critérios de aceite:

- somente perfil autorizado;
- senha nunca aparece em log;
- duplicidade de username/e-mail retorna conflito;
- teste de integração.

Dependência:

```text
TASK-007
```

---

## TASK-009 — Implementar serviço de e-mail

Status:

```text
[ ]
```

Implementar:

- interface de envio;
- implementação SMTP;
- template de acesso inicial;
- configuração externa.

Critérios de aceite:

- senha SMTP não fica no código;
- falha de envio é tratada;
- testes não dependem de SMTP real.

---

## TASK-010 — Implementar login por username ou e-mail

Status:

```text
[ ]
```

Endpoint:

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "login": "user@example.com",
  "password": "secret"
}
```

Critérios de aceite:

- login funciona por username;
- login funciona por e-mail;
- senha inválida não informa qual parte da credencial está errada;
- usuário bloqueado não autentica;
- lastLoginAt é atualizado conforme regra definida.

Dependências:

```text
TASK-007
TASK-009
```

---

## TASK-011 — Implementar access token JWT

Status:

```text
[ ]
```

Critérios de aceite:

- token possui expiração;
- não contém informação sensível;
- endpoints protegidos recusam token inválido;
- segredo vem de configuração externa.

---

## TASK-012 — Implementar refresh token

Status:

```text
[ ]
```

Endpoints:

```http
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

Critérios de aceite:

- refresh token pode ser invalidado;
- logout invalida sessão correspondente;
- token expirado não pode renovar sessão indevidamente.

---

## TASK-013 — Implementar troca obrigatória da senha temporária

Status:

```text
[ ]
```

Endpoint:

```http
POST /api/v1/auth/change-password
```

Critérios de aceite:

- usuário com mustChangePassword=true não acessa funcionalidades normais;
- senha antiga é validada quando aplicável;
- nova senha é armazenada somente como hash;
- flag é removida após sucesso.

---

## TASK-014 — Implementar recuperação de senha

Status:

```text
[ ]
```

Endpoints:

```http
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
```

Critérios de aceite:

- token temporário possui expiração;
- token é de uso único;
- resposta não revela se determinado e-mail existe quando isso gerar risco de enumeração.

---

## TASK-015 — Configurar autorização por perfil

Status:

```text
[ ]
```

Regras base:

```text
/api/v1/admin/** -> ADMIN
/api/v1/arweg/** -> ARWEG ou ADMIN
demais endpoints protegidos -> usuário autenticado
```

Critérios de aceite:

- testes para 401;
- testes para 403;
- participante não acessa endpoints ARWEG;
- ARWEG não acessa funções exclusivas de ADMIN sem permissão.

---

# Milestone 2 — Profile, Themes and Preferences

## TASK-016 — Implementar perfil do usuário

Status:

```text
[ ]
```

Endpoints:

```http
GET   /api/v1/users/me
PATCH /api/v1/users/me
```

Critérios de aceite:

- usuário consulta apenas o próprio perfil;
- campos administrativos não podem ser alterados pelo participante.

---

## TASK-017 — Implementar temas

Status:

```text
[ ]
```

Modelo:

```text
Theme
    id
    name
    description
    active
```

Implementar gerenciamento administrativo.

---

## TASK-018 — Implementar categorias

Status:

```text
[ ]
```

Modelo:

```text
Category
    id
    name
    description
    active
```

Implementar gerenciamento administrativo.

---

## TASK-019 — Implementar preferências do usuário

Status:

```text
[ ]
```

Endpoints:

```http
GET /api/v1/users/me/preferences
PUT /api/v1/users/me/preferences
```

Critérios de aceite:

- usuário substitui sua seleção de temas;
- somente temas válidos podem ser vinculados;
- alteração afeta posteriormente o feed.

---

# Milestone 3 — Workshops

## TASK-020 — Criar domínio de workshop

Status:

```text
[ ]
```

Campos iniciais:

```text
Workshop
    id
    title
    description
    image
    theme
    category
    startDate
    endDate
    startTime
    endTime
    location
    modality
    price
    registrationStart
    registrationEnd
    maximumParticipants
    paymentMethod
    additionalInformation
    status
    scheduledPublishAt
    publishedAt
    createdBy
    createdAt
    updatedAt
```

Estados:

```text
DRAFT
SCHEDULED
PUBLISHED
CLOSED
CANCELLED
ARCHIVED
```

Critérios de aceite:

- migration;
- repository;
- validações básicas;
- testes.

---

## TASK-021 — Criar workshop

Status:

```text
[ ]
```

Endpoint:

```http
POST /api/v1/workshops
```

Autorização:

```text
ARWEG
ADMIN
```

Critérios de aceite:

- workshop inicia como DRAFT;
- datas são validadas;
- horários são validados;
- período de inscrição é validado;
- capacidade não pode ser inválida.

---

## TASK-022 — Consultar workshop por ID

Status:

```text
[ ]
```

Endpoint:

```http
GET /api/v1/workshops/{id}
```

Critérios de aceite:

- participante não visualiza rascunho não autorizado;
- 404 para ID inexistente;
- DTO não expõe dados internos.

---

## TASK-023 — Listar workshops

Status:

```text
[ ]
```

Endpoint:

```http
GET /api/v1/workshops
```

Filtros iniciais:

```text
date
theme
category
location
modality
status
availability
```

Critérios de aceite:

- paginação;
- ordenação;
- filtros combináveis;
- queries sem N+1 óbvio.

---

## TASK-024 — Editar workshop

Status:

```text
[ ]
```

Endpoints:

```http
PUT   /api/v1/workshops/{id}
PATCH /api/v1/workshops/{id}
```

Critérios de aceite:

- autorização;
- validação por estado;
- alterações relevantes podem gerar evento/notificação em fase posterior;
- updatedAt é alterado.

---

## TASK-025 — Publicar workshop

Status:

```text
[ ]
```

Endpoint:

```http
PATCH /api/v1/workshops/{id}/publish
```

Critérios de aceite:

- somente DRAFT/SCHEDULED permitido conforme regra;
- workshop incompleto não publica;
- publishedAt registrado;
- testes de transição.

---

## TASK-026 — Agendar publicação de workshop

Status:

```text
[ ]
```

Critérios de aceite:

- workshop pode ir para SCHEDULED;
- data futura obrigatória;
- job publica automaticamente;
- publicação é idempotente.

---

## TASK-027 — Cancelar workshop

Status:

```text
[ ]
```

Endpoint:

```http
PATCH /api/v1/workshops/{id}/cancel
```

Registrar:

```text
reason
cancelledBy
cancelledAt
```

Critérios de aceite:

- transição válida;
- cancelamento não apaga histórico;
- integrações com inscrições/grupo/notificações serão adicionadas nas tasks correspondentes.

---

## TASK-028 — Encerrar workshop

Status:

```text
[ ]
```

Critérios de aceite:

- workshop realizado pode chegar a CLOSED;
- transição inválida é recusada.

---

## TASK-029 — Arquivar workshop

Status:

```text
[ ]
```

Endpoint:

```http
PATCH /api/v1/workshops/{id}/archive
```

Critérios de aceite:

- workshop arquivado não aparece no feed principal;
- histórico permanece disponível.

---

## TASK-030 — Duplicar workshop

Status:

```text
[ ]
```

Endpoint:

```http
POST /api/v1/workshops/{id}/duplicate
```

Não copiar:

- participantes;
- inscrições;
- pagamentos;
- mensagens;
- avaliações.

Novo workshop:

```text
DRAFT
```

---

# Milestone 4 — Files

## TASK-031 — Criar abstração de armazenamento

Status:

```text
[ ]
```

Criar interface equivalente a:

```text
FileStorageService
```

Critérios de aceite:

- regra de negócio não depende de S3/MinIO diretamente;
- banco armazena somente metadados e referência.

---

## TASK-032 — Implementar upload de imagem de workshop

Status:

```text
[ ]
```

Validar:

- tamanho;
- extensão;
- MIME;
- integridade.

---

## TASK-033 — Implementar anexos

Status:

```text
[ ]
```

Cobrir workshop e posteriormente posts.

---

# Milestone 5 — Registrations and Waiting List

## TASK-034 — Criar domínio de inscrição

Status:

```text
[ ]
```

Modelo:

```text
Registration
    id
    user
    workshop
    status
    paymentStatus
    registeredAt
    cancelledAt
```

Estados iniciais:

```text
PENDING
CONFIRMED
WAITING_LIST
CANCELLED
REFUNDED
```

Critérios de aceite:

- constraint de usuário/workshop;
- migration;
- repository;
- testes.

---

## TASK-035 — Implementar inscrição em workshop

Status:

```text
[ ]
```

Endpoint:

```http
POST /api/v1/workshops/{id}/registrations
```

Validar:

- workshop publicado;
- período aberto;
- inscrição anterior;
- vagas;
- requisitos.

Critérios de aceite:

- operação transacional;
- duplicidade retorna conflito;
- testes de integração.

---

## TASK-036 — Implementar proteção de concorrência de vagas

Status:

```text
[ ]
```

Cenário obrigatório:

```text
1 vaga restante
10 inscrições simultâneas
1 confirmação
```

Critérios de aceite:

- teste automatizado;
- nenhuma sobrelotação;
- solução documentada no código ou ADR quando necessário.

---

## TASK-037 — Implementar lista de espera

Status:

```text
[ ]
```

Critérios de aceite:

- inscrição entra como WAITING_LIST quando aplicável;
- ordem é preservada;
- posição pode ser consultada.

---

## TASK-038 — Promover participante da lista de espera

Status:

```text
[ ]
```

Critérios de aceite:

- primeira pessoa elegível é promovida;
- operação é transacional;
- não cria sobrelotação;
- futura notificação possui ponto de integração.

---

## TASK-039 — Gerenciar lista de espera como ARWEG

Status:

```text
[ ]
```

Endpoints planejados:

```http
GET    /api/v1/workshops/{id}/waiting-list
DELETE /api/v1/workshops/{id}/waiting-list/{userId}
PATCH  /api/v1/workshops/{id}/waiting-list/{userId}/promote
```

---

# Milestone 6 — Payments and Cancellation

## TASK-040 — Criar domínio de pagamento

Status:

```text
[ ]
```

Modelo:

```text
Payment
    id
    registration
    amount
    status
    method
    externalReference
    createdAt
    updatedAt
```

Estados:

```text
PENDING
PAID
DECLINED
CANCELLED
REFUNDED
EXEMPT
```

---

## TASK-041 — Criar abstração de pagamento

Status:

```text
[ ]
```

Criar serviço de domínio desacoplado de gateway.

Critérios de aceite:

- integração externa substituível;
- testes utilizam fake/mock.

---

## TASK-042 — Atualizar status de pagamento

Status:

```text
[ ]
```

Critérios de aceite:

- transições válidas;
- autorização administrativa;
- histórico preservado quando necessário.

---

## TASK-043 — Cancelar inscrição

Status:

```text
[ ]
```

Endpoint sugerido:

```http
PATCH /api/v1/registrations/{id}/cancel
```

Critérios de aceite:

- somente proprietário ou perfil autorizado;
- prazo validado;
- vaga é liberada;
- promoção da lista é executada quando aplicável.

---

## TASK-044 — Solicitar/processar reembolso

Status:

```text
[ ]
```

Critérios de aceite:

- valida elegibilidade;
- atualiza estados de inscrição e pagamento corretamente;
- idempotência;
- auditoria futura possui ponto de integração.

---

# Milestone 7 — History and Calendar

## TASK-045 — Implementar histórico do usuário

Status:

```text
[ ]
```

Endpoint:

```http
GET /api/v1/users/me/workshops
```

Filtros:

```text
future
ongoing
completed
cancelled
waiting-list
attended
absent
```

---

## TASK-046 — Implementar calendário

Status:

```text
[ ]
```

Endpoint:

```http
GET /api/v1/calendar
```

Filtros:

```text
startDate
endDate
type
```

Tipos:

```text
ALL
REGISTERED
PREFERENCES
PAST
FUTURE
```

---

# Milestone 8 — Posts and Feed

## TASK-047 — Criar domínio de post

Status:

```text
[ ]
```

Modelo:

```text
Post
    id
    title
    content
    image
    workshop
    category
    status
    highlight
    scheduledAt
    publishedAt
    createdBy
    createdAt
    updatedAt
```

---

## TASK-048 — Criar post

Status:

```text
[ ]
```

Autorização:

```text
ARWEG
ADMIN
```

---

## TASK-049 — Editar e excluir post

Status:

```text
[ ]
```

Critérios de aceite:

- autorização;
- updatedAt;
- exclusão conforme política definida.

---

## TASK-050 — Publicar/agendar post

Status:

```text
[ ]
```

Critérios de aceite:

- DRAFT;
- SCHEDULED;
- PUBLISHED;
- publicação automática.

---

## TASK-051 — Implementar feed personalizado

Status:

```text
[ ]
```

Endpoint:

```http
GET /api/v1/feed
```

Ordenação inicial:

```text
highlight
preferences
upcoming
registrationOpen
recency
```

Critérios de aceite:

- resultado paginado;
- regras determinísticas;
- somente conteúdo permitido ao usuário.

---

## TASK-052 — Implementar paginação por cursor no feed

Status:

```text
[ ]
```

Resposta sugerida:

```json
{
  "items": [],
  "nextCursor": null,
  "hasMore": false
}
```

---

## TASK-053 — Implementar curtidas

Status:

```text
[ ]
```

Endpoints:

```http
POST   /api/v1/posts/{id}/likes
DELETE /api/v1/posts/{id}/likes
```

Critérios de aceite:

- um like por usuário;
- operação idempotente.

---

## TASK-054 — Implementar comentários

Status:

```text
[ ]
```

Endpoints:

```http
POST   /api/v1/posts/{id}/comments
GET    /api/v1/posts/{id}/comments
PATCH  /api/v1/comments/{id}
DELETE /api/v1/comments/{id}
```

Critérios de aceite:

- paginação;
- autorização de edição/exclusão;
- ponto de moderação.

---

# Milestone 9 — Groups and Chat

## TASK-055 — Criar grupo associado ao workshop

Status:

```text
[ ]
```

Regra:

```text
1 Workshop -> 1 Group
```

Critérios de aceite:

- relacionamento único;
- criação integrada ao ciclo definido do workshop.

---

## TASK-056 — Adicionar participante ao grupo

Status:

```text
[ ]
```

Critérios de aceite:

- somente inscrição válida;
- pagamento respeitado quando necessário;
- operação idempotente.

---

## TASK-057 — Remover acesso após cancelamento

Status:

```text
[ ]
```

Critérios de aceite:

- usuário perde acesso quando a inscrição deixa de ser válida;
- histórico de inscrição permanece.

---

## TASK-058 — Encerrar grupo com workshop

Status:

```text
[ ]
```

Critérios de aceite:

- cancelamento/encerramento segue regra definida;
- usuários deixam de enviar mensagens;
- histórico segue política definida.

---

## TASK-059 — Criar domínio de mensagem

Status:

```text
[ ]
```

Modelo:

```text
Message
    id
    group
    author
    content
    sentAt
    editedAt
    deletedAt
```

---

## TASK-060 — Enviar mensagem

Status:

```text
[ ]
```

Endpoint:

```http
POST /api/v1/groups/{id}/messages
```

Critérios de aceite:

- somente membro autorizado;
- grupo precisa estar ativo;
- conteúdo validado.

---

## TASK-061 — Consultar mensagens

Status:

```text
[ ]
```

Endpoint:

```http
GET /api/v1/groups/{id}/messages
```

Paginação:

```text
cursor
limit
```

---

## TASK-062 — Implementar WebSocket

Status:

```text
[ ]
```

Critérios de aceite:

- autenticação;
- autorização por grupo;
- mensagens REST continuam sendo fonte persistente.

---

## TASK-063 — Implementar moderação de mensagens

Status:

```text
[ ]
```

Permitir:

- autor remover própria mensagem quando permitido;
- moderador remover mensagem;
- registrar ação.

---

# Milestone 10 — Evaluations

## TASK-064 — Criar domínio de avaliação

Status:

```text
[ ]
```

Campos:

```text
rating
comment
contentRating
instructorRating
organizationRating
```

---

## TASK-065 — Avaliar workshop

Status:

```text
[ ]
```

Endpoint:

```http
POST /api/v1/workshops/{id}/evaluations
```

Critérios de aceite:

- workshop concluído;
- usuário elegível;
- uma avaliação por usuário/workshop.

---

## TASK-066 — Consultar avaliações como ARWEG

Status:

```text
[ ]
```

---

## TASK-067 — Métricas de avaliação

Status:

```text
[ ]
```

Calcular:

- média;
- quantidade;
- distribuição.

---

# Milestone 11 — Notifications

## TASK-068 — Criar domínio de notificação

Status:

```text
[ ]
```

Modelo:

```text
Notification
    id
    user
    type
    title
    message
    read
    data
    createdAt
```

---

## TASK-069 — Criar central de notificações

Status:

```text
[ ]
```

Endpoints:

```http
GET   /api/v1/notifications
PATCH /api/v1/notifications/{id}/read
PATCH /api/v1/notifications/read-all
```

---

## TASK-070 — Disparar notificações automáticas

Status:

```text
[ ]
```

Eventos:

- inscrição;
- cancelamento;
- vaga;
- alteração do workshop;
- cancelamento do workshop;
- pagamento;
- mensagem;
- post;
- workshop próximo.

---

## TASK-071 — Implementar push notification

Status:

```text
[ ]
```

Criar abstração de provedor.

Registrar dispositivos/tokens de forma segura.

---

## TASK-072 — Notificações manuais ARWEG

Status:

```text
[ ]
```

---

## TASK-073 — Agendamento de notificações

Status:

```text
[ ]
```

---

# Milestone 12 — Attendance and Administration

## TASK-074 — Registrar presença

Status:

```text
[ ]
```

Estados:

```text
ATTENDED
NOT_ATTENDED
JUSTIFIED_ABSENCE
ABSENT
```

---

## TASK-075 — Atualização de presença em massa

Status:

```text
[ ]
```

---

## TASK-076 — Listar participantes do workshop

Status:

```text
[ ]
```

Filtros:

```text
name
email
wegRegistration
registrationStatus
paymentStatus
attendanceStatus
```

---

## TASK-077 — Exportar participantes CSV

Status:

```text
[ ]
```

---

## TASK-078 — Exportar participantes XLSX

Status:

```text
[ ]
```

---

## TASK-079 — Dashboard ARWEG

Status:

```text
[ ]
```

Métricas iniciais:

- workshops ativos;
- workshops futuros;
- encerrados;
- inscrições;
- ocupação;
- cancelamentos;
- avaliações.

---

# Milestone 13 — Metrics and Audit

## TASK-080 — Registrar visualizações

Status:

```text
[ ]
```

Recursos:

- workshop;
- post.

---

## TASK-081 — Métricas de workshop

Status:

```text
[ ]
```

Incluir:

- visualizações;
- inscrições;
- conversão;
- cancelamentos;
- presença;
- ausência;
- ocupação;
- avaliação.

---

## TASK-082 — Métricas de post

Status:

```text
[ ]
```

Incluir:

- visualizações;
- likes;
- comentários;
- cliques;
- acessos ao workshop.

---

## TASK-083 — Implementar auditoria administrativa

Status:

```text
[ ]
```

Registrar:

```text
userId
action
entity
entityId
previousValue
newValue
timestamp
ip
```

---

## TASK-084 — Consultar auditoria

Status:

```text
[ ]
```

Critérios de aceite:

- paginação;
- filtros;
- acesso administrativo.

---

# Milestone 14 — Mobile and Offline

## TASK-085 — Adicionar sincronização incremental

Status:

```text
[ ]
```

Recursos relevantes devem possuir:

```text
createdAt
updatedAt
```

Adicionar filtros equivalentes a:

```text
updatedAfter
```

quando necessários.

---

## TASK-086 — Implementar cache HTTP

Status:

```text
[ ]
```

Avaliar:

```text
ETag
If-None-Match
Cache-Control
```

Aplicar apenas em endpoints apropriados.

---

## TASK-087 — Implementar idempotência

Status:

```text
[ ]
```

Prioridade:

- inscrição;
- cancelamento;
- pagamento;
- reembolso.

Header:

```http
Idempotency-Key: <uuid>
```

---

# Milestone 15 — Security and Observability

## TASK-088 — Hardening de autenticação

Status:

```text
[ ]
```

Implementar conforme necessário:

- política de senha;
- rate limiting;
- bloqueio por tentativas;
- expiração;
- proteção contra brute force.

---

## TASK-089 — Revisão de autorização

Status:

```text
[ ]
```

Criar matriz de acesso e testes.

---

## TASK-090 — Logging estruturado

Status:

```text
[ ]
```

Campos:

```text
requestId
userId
endpoint
status
duration
```

---

## TASK-091 — Health checks

Status:

```text
[ ]
```

---

## TASK-092 — Métricas técnicas

Status:

```text
[ ]
```

Monitorar:

- requests;
- latência;
- erros;
- banco;
- jobs;
- recursos.

---

# Milestone 16 — Test Quality

## TASK-093 — Consolidar suíte unitária

Status:

```text
[ ]
```

---

## TASK-094 — Consolidar integração com Testcontainers

Status:

```text
[ ]
```

Banco principal dos testes de integração:

```text
PostgreSQL
```

---

## TASK-095 — Testes de segurança

Status:

```text
[ ]
```

Cobrir:

- sem token;
- token inválido;
- token expirado;
- usuário bloqueado;
- troca obrigatória;
- acesso entre roles.

---

## TASK-096 — Testes de concorrência

Status:

```text
[ ]
```

Cobrir obrigatoriamente:

- última vaga;
- promoção da lista;
- pagamento idempotente;
- cancelamento concorrente quando aplicável.

---

## TASK-097 — Testes de contrato

Status:

```text
[ ]
```

Validar DTOs e OpenAPI.

---

# Milestone 17 — Documentation and Release

## TASK-098 — Completar OpenAPI

Status:

```text
[ ]
```

Todos os endpoints devem possuir:

- descrição;
- autenticação;
- request;
- response;
- status;
- erros relevantes.

---

## TASK-099 — Documentar regras de negócio

Status:

```text
[ ]
```

Criar documento ou seção estruturada de regras.

Exemplo:

```text
BR-001 — A user cannot have two active registrations for the same workshop.
BR-002 — Registration is only allowed during the configured registration period.
BR-003 — Only ARWEG/ADMIN can publish content.
BR-004 — Group access requires a valid registration.
BR-005 — Workshop cancellation affects registrations and group lifecycle.
```

---

## TASK-100 — Preparar primeira release

Status:

```text
[ ]
```

Critérios de aceite:

- build verde;
- migrations validadas;
- documentação atualizada;
- configuração externa;
- health check;
- segurança revisada;
- nenhuma credencial;
- versão definida.

---

# MVP

Para a primeira versão funcional, priorizar:

```text
TASK-001 até TASK-019
TASK-020 até TASK-030
TASK-034 até TASK-044
TASK-047 até TASK-054
TASK-055 até TASK-063
TASK-068 até TASK-071
TASK-085
TASK-087
TASK-088
TASK-089
TASK-095
TASK-096
TASK-098
```

O MVP deve permitir o fluxo:

```text
ADMIN cria usuário
↓
usuário recebe senha temporária
↓
login
↓
troca obrigatória de senha
↓
seleção de preferências
↓
consulta/feed de workshops
↓
inscrição
↓
pagamento quando necessário
↓
entrada no grupo
↓
chat
↓
notificações
```

---

# Ordem imediata de execução

Começar por:

```text
TASK-001
TASK-002
TASK-003
TASK-004
TASK-005
TASK-006
TASK-007
```

Não iniciar feed, chat ou pagamento antes de existir uma base funcional de autenticação, usuários, banco e workshops.

Isso evita construir metade do sistema em cima de entidades que ainda mudam todo dia, uma tradição bastante popular e bastante ruim.
