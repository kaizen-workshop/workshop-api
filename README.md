# workshop_api

API backend do projeto de workshops da ARWEG.

O sistema centraliza descoberta, publicação, gerenciamento e participação em workshops, incluindo autenticação, feed personalizado, inscrições, pagamentos, grupos, chat, avaliações, notificações e recursos administrativos.

A API foi pensada principalmente para consumo por uma aplicação mobile.

---

# Status

Projeto em planejamento e implementação inicial.

O desenvolvimento é orientado por:

- `AGENTS.md`: regras obrigatórias para desenvolvimento humano e por agentes de IA;
- `TASKS.md`: backlog técnico e ordem de implementação;
- documentação funcional do projeto: requisitos de negócio.

Antes de iniciar qualquer feature, leia esses arquivos.

---

# Objetivos

A API deve permitir que participantes:

- realizem login;
- configurem preferências;
- visualizem feed personalizado;
- consultem workshops;
- realizem e cancelem inscrições;
- acompanhem histórico e calendário;
- participem de grupos;
- utilizem chat;
- recebam notificações;
- avaliem workshops.

Usuários ARWEG devem poder:

- criar e gerenciar workshops;
- publicar conteúdo;
- gerenciar participantes;
- acompanhar inscrições;
- controlar presença;
- gerenciar pagamentos;
- moderar grupos;
- enviar comunicações;
- consultar avaliações;
- consultar métricas.

Administradores devem poder gerenciar usuários e operações administrativas do sistema.

---

# Stack

Stack planejada:

```text
Java 21
Spring Boot 3.x
Spring Web
Spring Data JPA
Spring Security
Bean Validation
PostgreSQL
Flyway
JWT
Spring Mail
Spring Boot Actuator
OpenAPI / Swagger
JUnit 5
Mockito
Testcontainers
```

---

# Arquitetura

A aplicação será inicialmente um monólito modular.

Não serão utilizados microserviços na primeira versão.

Estrutura de domínio esperada:

```text
src/main/java/br/com/weg/workshop
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

Cada módulo pode conter:

```text
controller
service
repository
domain
dto
mapper
validation
```

A separação deve seguir a necessidade do módulo, sem criar camadas vazias apenas por convenção.

---

# API

Base path:

```text
/api/v1
```

Exemplo:

```text
GET  /api/v1/workshops
POST /api/v1/workshops
GET  /api/v1/workshops/{id}
```

A documentação interativa será disponibilizada via OpenAPI/Swagger.

---

# Perfis

Perfis iniciais:

```text
PARTICIPANT
ARWEG
ADMIN
```

## PARTICIPANT

Pode:

- consultar workshops;
- editar informações pessoais permitidas;
- configurar preferências;
- realizar inscrição;
- cancelar inscrição;
- entrar em lista de espera;
- acessar seus grupos;
- participar do chat;
- interagir com posts;
- avaliar workshops elegíveis.

## ARWEG

Pode, conforme autorização:

- criar workshops;
- editar workshops;
- publicar;
- agendar publicação;
- cancelar workshops;
- criar posts;
- editar posts;
- gerenciar participantes;
- gerenciar pagamentos;
- controlar presença;
- moderar chats;
- enviar notificações;
- visualizar avaliações;
- visualizar métricas.

## ADMIN

Responsável por operações administrativas do sistema, incluindo gerenciamento de usuários.

---

# Autenticação

O login aceitará:

```text
username
ou
e-mail
```

Fluxo de criação de conta:

```text
ADMIN cria usuário
        ↓
sistema gera senha temporária
        ↓
senha é enviada ao e-mail do usuário
        ↓
usuário realiza login
        ↓
API exige alteração de senha
        ↓
usuário cria senha definitiva
```

A senha nunca é armazenada em texto puro.

A autenticação utilizará access token e refresh token.

---

# Workshops

Estados planejados:

```text
DRAFT
SCHEDULED
PUBLISHED
CLOSED
CANCELLED
ARCHIVED
```

Um workshop poderá possuir informações como:

```text
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
```

A publicação deve validar se o workshop possui os dados obrigatórios.

---

# Inscrições

Antes de confirmar uma inscrição, a API deve validar:

- workshop existente;
- workshop disponível;
- período de inscrição;
- vagas;
- inscrição anterior;
- requisitos do workshop;
- situação de pagamento quando aplicável.

A API deve impedir que duas pessoas ocupem simultaneamente uma única vaga restante.

Esse comportamento será coberto por teste de concorrência.

---

# Lista de espera

Quando o workshop estiver lotado, o participante poderá entrar em lista de espera quando permitido.

A ordem deve ser preservada.

Quando uma vaga for liberada, o próximo participante elegível poderá ser promovido.

---

# Pagamentos

A API deve manter o domínio de pagamentos desacoplado do gateway externo.

Estados iniciais:

```text
PENDING
PAID
DECLINED
CANCELLED
REFUNDED
EXEMPT
```

Cancelamentos podem gerar processo de reembolso conforme as regras do workshop.

---

# Feed

A aplicação possuirá feed personalizado.

Conteúdos podem considerar:

- preferências;
- workshops futuros;
- inscrições abertas;
- conteúdo em destaque;
- recência.

Somente usuários autorizados da ARWEG poderão criar posts.

Participantes poderão:

- visualizar;
- curtir;
- comentar.

A primeira implementação do feed deve utilizar regras determinísticas.

---

# Grupos

Regra inicial:

```text
1 Workshop -> 1 Grupo
```

O grupo é vinculado ao ciclo de vida do workshop.

Participantes obtêm acesso ao grupo conforme a situação válida da inscrição e pagamento.

Cancelamentos ou encerramentos devem refletir corretamente no acesso ao grupo.

---

# Chat

Cada grupo possui chat.

Participantes autorizados podem enviar e consultar mensagens.

O histórico deve ser paginado.

A API deverá suportar comunicação em tempo real em uma fase posterior através de WebSocket.

---

# Avaliações

Após a participação, usuários elegíveis poderão avaliar o workshop.

A avaliação pode conter:

- nota geral;
- comentário;
- conteúdo;
- instrutor;
- organização.

---

# Notificações

Eventos que podem gerar notificações:

- inscrição confirmada;
- inscrição cancelada;
- workshop alterado;
- workshop cancelado;
- vaga liberada;
- pagamento atualizado;
- nova mensagem;
- novo conteúdo;
- workshop próximo.

A implementação deve separar a regra de negócio do provedor de push notification.

---

# Arquivos

Imagens e anexos não devem ser armazenados diretamente no banco como Base64 por padrão.

Será utilizada uma abstração de armazenamento.

O banco mantém metadados e referência do arquivo.

Uploads devem validar:

- tamanho;
- extensão;
- MIME type;
- integridade.

---

# Mobile e offline

Como o cliente principal será mobile, a API deve considerar:

- conexão instável;
- retries;
- sincronização incremental;
- idempotência;
- payload reduzido;
- paginação;
- cache.

Operações críticas podem utilizar:

```http
Idempotency-Key: <uuid>
```

Recursos sincronizáveis devem possuir:

```text
createdAt
updatedAt
```

---

# Banco de dados

Banco:

```text
PostgreSQL
```

Versionamento de schema:

```text
Flyway
```

IDs principais:

```text
UUID
```

Não utilizar alteração automática de schema como estratégia de produção.

---

# Ambientes

Perfis planejados:

```text
dev
test
prod
```

Credenciais, URLs, segredos e chaves devem ser configurados externamente.

Exemplo:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
SMTP_HOST
SMTP_USERNAME
SMTP_PASSWORD
```

Nunca adicionar segredos ao repositório.

---

# Rodando localmente

O perfil `dev` é o padrão e usa PostgreSQL. Inicie o banco local com Docker Compose:

```bash
docker compose up -d
```

Depois, inicie a API:

```bash
./mvnw spring-boot:run
```

Testes:

```bash
./mvnw clean verify
```

As configurações locais podem ser sobrescritas por `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME` e `SMTP_PASSWORD`.
Não use credenciais reais nos arquivos versionados.

O Flyway aplica automaticamente as migrations de `src/main/resources/db/migration`
na inicialização. O Hibernate apenas valida o schema e não o altera.

Com a API em execução, a documentação OpenAPI está disponível em
`/swagger-ui.html`; o documento JSON está em `/v3/api-docs`.

---

# Qualidade

Toda feature relevante deve possuir testes.

Tipos esperados:

- unitários;
- integração;
- segurança;
- concorrência;
- contrato.

Integrações com PostgreSQL devem preferir Testcontainers.

---

# Regras de contribuição

Antes de implementar:

1. Ler `AGENTS.md`.
2. Encontrar a task correspondente em `TASKS.md`.
3. Entender o domínio afetado.
4. Implementar apenas o escopo necessário.
5. Adicionar ou atualizar testes.
6. Executar a suíte.
7. Atualizar documentação quando necessário.

Commits recomendados:

```text
feat: add workshop creation
fix: prevent duplicate registrations
test: cover concurrent registrations
docs: update API setup
```

---

# Ordem de implementação

A sequência inicial planejada é:

```text
Foundation
↓
Authentication
↓
Users
↓
Preferences
↓
Workshops
↓
Registrations
↓
Payments
↓
Feed
↓
Groups
↓
Chat
↓
Notifications
↓
Administration
↓
Metrics
```

O backlog detalhado está em `TASKS.md`.

---

# Documentação

Arquivos principais:

```text
AGENTS.md
README.md
TASKS.md
```

Documentação da API:

```text
OpenAPI / Swagger
```

Regras de negócio deverão permanecer explícitas e testáveis.

---

# Repositório

Organização do projeto:

```text
https://github.com/kaizen-workshop
```

O repositório esperado para esta API é:

```text
workshop_api
```
