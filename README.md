<div align="center">
  <img src="https://raw.githubusercontent.com/GabrielPedroSOFTWARE/Nexus-Frontend/main/public/nexus-logo.png" alt="NEXUS Logo" width="150" onerror="this.style.display='none'"/>

  # NEXUS - API Backend

  Sistema de gestão de manutenção de equipamentos de TI

  [Acessar Repositório Frontend](https://github.com/GabrielPedroSOFTWARE/Nexus-Frontend)

  ![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-brightgreen) ![MySQL](https://img.shields.io/badge/MySQL-8.0-blue) ![Gradle](https://img.shields.io/badge/Gradle-Build-02303A)
</div>

## Sobre o projeto

O **NEXUS - API Backend** é o backend que é reponsável pela lógica do sistema, por exemplo: gerenciamento de usuáiros e solicitações, leitura automática do hardware, gerenciamento de ordens de serviço (O.S.), notificações no site e por email, monitoramento dos prazos e indicadores MTBF e MTTR.

A API RESTful foi desenvolvida em **Java 21** com **Spring Boot**, ela roda local na porta 8080 para se conectar ao frontend React. A API REESTful tem integração com o MySQL, autenticação JWT stateless (com revogação da sessão, caso haja alteração no usuário), criptografia de CPFs e senhas, também responsável por rotinas em segundo plano.

## Captura de tela

Neste repositório, toda a comunicação ocorre via JSON (e `multipart/form-data` no envio de imagens). Para visualizar telas e gráficos, deve-se ir para o respositório [frontend] ([https://github.com/usuario/outro-repositorio](https://github.com/GabrielPedroSOFTWARE/Nexus-Frontend))

## Funcionalidades demonstradas

### Gestão de Equipamentos e Hardware (OSHI)
* **Leitura automática do hardware:** identifica Sistema Operacional, fabricante, modelo, processador, memória RAM, discos, placas de vídeo e se a máquina é notebook ou desktop.
* **Identificação do número de série do ativo:** tenta ler o serial do sistema e, caso seja genérico ou inválido, busca o serial da placa-mãe e, em último caso, o do hardware.
* **Atualização do ativo e impedimento para outro usuário acessar o PC de outro usuário:** atualiza componentes do ativo caso haja alteração nos componentes, impede que outro usuário cadastre ou altere o ativo de outro usuário, ele também impede a exclusão de ativos com O.S. em aberto.
* **Consulta de situação:** verifica em tempo real se há manutenção aberta para o ativo e informa a data da última manutenção concluída.

### Gestão de Ordens de Serviço (O.S.)
* **Abertura com anexo nativo:** suporta envio de fotos via `multipart/form-data` (armazenando bytes, nome original e `ContentType`) e gera identificadores únicos das O.S. no padrão `OS-ANO-XXXXXXXX`.
* **Fluxo completo de status:** gerencia as etapas da O.S. (`0 - Recebida`, `1 - Em diagnóstico`, `2 - Em manutenção`, `3 - Aguardando peças`, `4 - Pronto para retirada`, `5 - Concluída` e `6 - Cancelada`), registrando data e hora de cada etapa em um histórico.
* **Prioridade automática:** Há uma pontuação de prioridade para cada O.S, elas são: `0` (Baixa), `1` (Média) ou `2` (Alta). Essa pontuação leva em conta palavras-chave descritas no problema com a reincidência de chamados na máquina nos últimos 30 dias.
* **Operações do Técnico/Gestor:** Ele consegue trocar técnicos, devolver O.S à fila tendo uma justificativa, registrar observações técnicas, reajustar prazos em caso de atraso e também pode cancelar chamados.

### Monitoramento Automático, Notificações e E-mails
* **Alertas agendados (`@EnableScheduling`):** No código há rotinas que executam a cada 1 hora para:
  * Avisar o técnico responsável pela O.S quando o prazo de uma O.S. está em até **3 dias** do vencimento.
  * Alertar o técnico quando uma O.S. fica **72 horas** sem nenhuma alteração.
  * Avisar o ADM quando o técnico passa mais um dia sem atualizar a O.S mesmo depois do mesmo sendo avisado.
* **Notificações:** cada tipo de notificação é separada entre: usuários (`NotificacaoUsuario`) e equipe técnica/gestão (`NotificacaoBackOffice`), elas podem ser marcadas como lida.
* **E-mails:** envio automático via SMTP (Gmail) ao aprovar ou negar solicitações de cadastro e no processo de recuperação da senha do usuário.

### Dashboard e Indicadores de Desempenho
* **MTBF (*Mean Time Between Failures*):** calcula o tempo médio entre as falhas das O.S..
* **MTTR (*Mean Time To Repair*):** calcula o tempo médio de reparo entre o início do diagnóstico e a conclusão da O.S., permitindo que haja uma forma de analisar como está indo a equipe técnica.
* **Estatísticas gerais:** quantidade de usuários cadastrados e o total de máquinas cadastradas.

### Área Administrativa, Segurança e Privacidade
* **Controle de acesso em camadas:** no projeto existem três perfis: `USER`, `SUPPORT_MANAGER` e `PENDENT`. A flag `ehAdmin` (`ROLE_ADMIN`) não é considerada um perfil, mas um acesso a mais que um ADM pode receber. O sistema divide os endpoints nas rotas: `/user/**`, `/backoffice/**` e `/adm/**`.
* **JWT com invalidação em tempo real:** o sistema possui tokens assinados em HMAC256 com validade de até 2 horas vinculados a `versaoToken`. Qualquer alteração de perfil, status ou privilégio administrativo causa invalidação das sessões antigas.
* **Criptografia e mascaramento de CPF:** CPFs são salvos no banco usando AES/CBC determinístico com vetor de inicialização derivado por HMAC-SHA256 na classe `CriptografiaCpfService` e são retornados utilizando a seguinte máscara: `***.XXX.XXX-**`. Senhas são protegidas com `BCrypt`.
* **Recuperação de senha em dois passos:** primeiro, o sistema solicita o CPF do usuário, depois envia um link temporário por email para confirmar o usuário e se utiliza da variável `códigoEspera` para verificar se o usuário clicou no link do e-mail, quando o usuário clicar no link do email, um método novo definirá a senha.
* **Aprovação, Batch Update e Auditoria:** o sistema faz análise de novos cadastros (`Aguardando análise`), edição de vários usuários em uma única requisição e também guarda um histórico imutável de alterações tanto de O.S. (`HistoricoPedido`) quanto de usuários (`HistoricoUsuario`).

## Tecnologias

* [Java 21](https://www.oracle.com/java/) como linguagem principal.
* [Spring Boot 4.1.1](https://spring.io/projects/spring-boot) (Web/WebMVC, Data JPA, Security, Validation, Mail e Scheduling).
* [Auth0 Java-JWT 4.4.0](https://github.com/auth0/java-jwt) para emissão e validação de tokens JWT.
* [OSHI Core 6.6.5](https://github.com/oshi/oshi) com [JNA / JNA-Platform 5.16.0](https://github.com/java-native-access/jna) para leitura nativa de hardware.
* [MySQL 8.0](https://www.mysql.com/) (`mysql-connector-j`) e [Hibernate](https://hibernate.org/) para persistência relacional.
* [Lombok](https://projectlombok.org/) para redução de código boilerplate em entidades e DTOs.
* [Gradle](https://gradle.org/) e [JUnit Platform](https://junit.org/) para build, gerenciamento de dependências e testes.

## Instalação e execução

### Pré-requisitos

* Java Development Kit (JDK) versão `21` instalada.
* Servidor MySQL rodando localmente na porta `3306` com o banco `banco_teste` criado.
* Conta SMTP configurada (ex.: Gmail com Senha de App) para disparo de e-mails.

### Preparar o projeto

```bash
git clone https://github.com/GabrielPedroSOFTWARE/Nexus.git
cd Nexus-Backend
```

Configure um banco mysql com username: root e senha 12345, crie um banco chamado "banco_teste".

O projeto usa o Gradle Wrapper, dispensando a instalação prévia do Gradle na máquina. Inicie o servidor com:

```bash
# No Windows
gradlew bootRun

# No Linux/Mac
./gradlew bootRun
```

Após a inicialização, a API ficará disponível em `http://127.0.0.1:8080`.

### Comandos disponíveis

| Comando | O que faz |
| :--- | :--- |
| `./gradlew bootRun` | Inicia o servidor de desenvolvimento local. |
| `./gradlew build` | Baixa dependências, compila o código e gera o pacote executável `.jar`. |
| `./gradlew test` | Executa a suíte de testes automatizados da aplicação. |

## Rotas principais da API

### Autenticação e Recuperação de Senha (Públicas)

| Rota | Método | O que faz |
| :--- | :--- | :--- |
| `/login` | `POST` | Autentica o usuário e retorna o token JWT, perfil e flag de admin (caso haja). |
| `/cadastrar` | `POST` | Envia solicitação de cadastro. |
| `/esqueci-senha` | `POST` | Gera o link de recuperação por e-mail e retorna o `codigoEspera`. |
| `/confirmar-recuperacao` | `GET` | Valida o clique no link enviado por e-mail (`?token=...`). |
| `/status-recuperacao` | `GET` | Consulta se o link já foi confirmado (`?codigo=...`). |
| `/redefinir-senha` | `POST` | Salva a nova senha após a confirmação e invalida os códigos de uso único. |

### Equipamentos e Hardware (`/user/**` e consultas)

| Rota | Método | O que faz |
| :--- | :--- | :--- |
| `/user/pecas` | `GET` | Lê os componentes físicos da máquina local via classe OSHI. |
| `/user/adicionar-equipamento` | `POST` | Cadastra ou atualiza o equipamento vinculado ao usuário. |
| `/user/alterar-equipamento/{id}` | `PUT` | Atualiza os dados de um equipamento existente. |
| `/user/remover-equipamento/{id}` | `DELETE` | Remove o equipamento, caso não exista O.S. em aberto. |
| `/user/listar-equipamentos-usuario` | `GET` | Lista os equipamentos do usuário logado. |
| `/user/equipamentos-para-pedidos` | `GET` | Lista equipamentos que podem estar vinculados a uma O.S. |
| `/existe-pedido-em-aberto/{idPC}` | `GET` | Verifica se o equipamento possui manutenção em andamento. |
| `/retornar-ultima-manutencao/{idPC}` | `GET` | Retorna `"Em manutenção"`, a data da última conclusão ou `"—"`. |

### Ordens de Serviço e Histórico

| Rota | Método | O que faz |
| :--- | :--- | :--- |
| `/user/criar-pedido` | `POST` | Abre uma nova O.S. com suporte a `multipart/form-data`, juntamente com cálculo de prioridade. |
| `/user/listar-pedidos-usuario` | `GET` | Lista todas as O.S. vinculadas ao usuário. |
| `/listar-pedidos-equipamento/{id}` | `GET` | Lista o histórico de O.S. de um equipamento específico. |
| `/backoffice/listar-pedidos` | `GET` | Lista todas as O.S. do sistema. |
| `/backoffice/listar-pedidos-atribuidos` | `GET` | Lista as O.S. em aberto atribuídas ao técnico logado. |
| `/backoffice/listar-pedidos-frequencia` | `GET` | Filtra O.S. por frequência (`?frequencia=...`). |
| `/backoffice/atribuir-tecnico/{id}` | `PATCH` | Atribui ou transfere um técnico para a O.S. |
| `/backoffice/alterar-status-pedido/{id}` | `PATCH` | Atualiza a etapa da O.S., registra prazos/justificativas e notifica o usuário. |
| `/backoffice/realizar-observacoes/{id}` | `PATCH` | Adiciona observações técnicas à O.S. |
| `/backoffice/definir-justificativa-pedido/{id}` | `PATCH` | Define ou altera a justificativa registrada na O.S. |
| `/backoffice/devolver-a-fila/{id}` | `PATCH` | Remove o técnico atribuído e devolve a O.S. para a fila inicial. |
| `/backoffice/cancelar-pedido/{id}` | `PATCH` | Cancela a O.S., tendo o técnico/gestor que escrever uma justificativa obrigatória. |
| `/backoffice/retornar-historico-pedido-alt/{id}` | `GET` | Retorna o histórico de todas as alterações da O.S. |

### Dashboard e Notificações

| Rota | Método | O que faz |
| :--- | :--- | :--- |
| `/backoffice/dashboard/mtbf` | `GET` | Retorna o indicador MTBF (`?ano=...&mes=...`). |
| `/backoffice/dashboard/mttr` | `GET` | Retorna o indicador MTTR (`?ano=...&mes=...`). |
| `/backoffice/totalUsuarios` | `GET` | Retorna o total de usuários ativos e máquinas cadastradas. |
| `/user/retornar-notificacoes-usuario` | `GET` | Lista as notificações enviadas ao usuário. |
| `/user/marcar-notificacao-usuario/{id}` | `PATCH` | Marca uma notificação do usuário como lida. |
| `/backoffice/retornar-notificacoes-backoffice` | `GET` | Lista as notificações não lidas do técnico/gestor (também o ADM) logado. |
| `/backoffice/marcar-notificacao-backoffice/{id}` | `PATCH` | Marca notificação de backoffice como lida. |

### Administração de Usuários (`/adm/**`)

| Rota | Método | O que faz |
| :--- | :--- | :--- |
| `/adm/usuarios-pendentes` | `GET` | Lista solicitações de cadastro aguardando aprovação. |
| `/adm/aceitar-solicitacao` | `PUT` | Aprova um usuário pendente, define seu perfil e envia email de confirmação. |
| `/adm/negar-solicitacao-usuario` | `DELETE` | Recusa e remove a solicitação pendente e envia um email de confirmação. |
| `/adm/salvar-alteracoes-usuarios` | `PUT` | Atualiza perfis, status e permissões de admin de vários usuários de uma vez. |
| `/adm/retornar-todos-menos-atual` | `GET` | Lista todos os usuários cadastrados, exceto o administrador logado. |

## Estrutura do projeto

```text
.
├─ src/
│  ├─ main/java/com/example/demo/
│  │  ├─ NexusApplication.java        # Classe principal com @EnableScheduling
│  │  ├─ controller/                  # Endpoints REST (Autenticacao, Dashboard, Equipamento,
│  │  │                               # HistoricoPedido, Notificacao, Pedido, Usuario)
|  |  |
│  │  ├─ model/                       # Entidades JPA, Enums e DTOs (Usuario, Equipamento, Pedido,
│  │  │                               # HistoricoPedido, HistoricoUsuario, NotificacaoUsuario,
│  │  │                               # NotificacaoBackOffice, PerfilUsuario e DTOs)
|  |  |
│  │  ├─ repository/                  # Interfaces Spring Data JPA e consultas JPQL customizadas
│  │  ├─ security/                    # SecurityConfig (RBAC/CORS) e SecurityFilter (JWT + versaoToken)
│  │  └─ service/                     # Regras de negócio (Autenticacao, CriptografiaCpf, Dashboard,
│  │                                  # Email, Equipamento, HistoricoPedido, NotificacaoBackOffice,
│  │                                  # NotificacaoUsuario, Pedido, Prioridade, Token, Usuario)
│  └─ main/resources/
│     └─ application.properties       # Configurações de MySQL, Hibernate, JWT, SMTP e criptografia
├─ build.gradle                       # Dependências e configuração do Gradle
└─ README.md                          # Documentação do projeto
```

* **`controller`:** mostra as rotas HTTP, ele quem lida com as requisições.
* **`service`:** contém regras de negócio, leitura OSHI, criptografia de CPF, cálculo de prioridade, indicadores MTBF/MTTR, envio de e-mails e agendamentos automáticos.
* **`repository`:** onde está localizado o acesso ao MySQL e as consultas customizadas para reincidência, inatividade, prazos e históricos.
* **`model`:** tabelas no banco de dados, enum de perfis e DTOs de entrada e saída.
* **`security`:** isola as permissões por prefixo de rota, regras de CORS, criptografia `BCrypt` e validação de tokens JWT.

## Escopo atual

O backend é uma espécie de servidor local conectado ao MySQL (`banco_teste`). Com ele em execução, todas as funcionalidades do NEXUS — cadastro com aprovação por e-mail, login com CPF criptografado, recuperação de senha, inventário automático de hardware, gestão de O.S. com upload de fotos, alertas de prazo, auditoria e dashboard de MTBF/MTTR — operam com dados reais.

## Execução integrada local

Para que o frontend rode no vs code localmente ou via GitHub Pages, deve-se manter este backend ativo na mesma máquina. É necessário utuilizar a estrutura abaixo no arquivo `src/main/resources/application.properties`:

```properties
spring.application.name=Nexus
spring.datasource.url=jdbc:mysql://localhost:3306/banco_teste
spring.datasource.username=root
spring.datasource.password=12345
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Segurança JWT e Criptografia de CPF
api.security.token.secret=12345678
app.criptografia.chave=K7x9mP2vQ8nL4rT6yB1wZ5cF0hJ3dG8sA2eU7iO9pX4

# Envio de e-mails (SMTP Gmail)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=seu-email@gmail.com
spring.mail.password=ylqisaxsphvohpdc
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
```

A API escuta em `http://127.0.0.1:8080`. A classe `SecurityConfig.java` libera os links `http://localhost:5173` e `https://gabrielpedrosoftware.github.io`, permitindo os métodos `GET`, `POST`, `PUT`, `DELETE` e `PATCH` e os cabeçalhos `Authorization` e `Content-Type`.

## Evoluções e correções de integração

* **Segmentação de rotas por perfil:** a maioria dos endpoints do sistema possuem os prefixos: `/user/**`, `/backoffice/**` e `/adm/**`, prefixos estes que são alinhados aos perfis `USER`, `SUPPORT_MANAGER`, e `ADMIN`.
* **Proteção de dados sensíveis:** cpfs passaram a ser criptografados com AES/HMAC no banco de dados e mascarados antes de serem enviados ao frontend.
* **Monitoramento proativo e indicadores:** o sistema agora possui agendamentos de horários para alertas de prazo/inatividade e endpoints para os gráficos de MTBF e MTTR.
* **Tipagem de frequência e arquivos nativos:** o campo `frequencia` em `Pedido.java` opera como `Integer` e o upload via `MultipartFile` preserva o nome e o tipo original (`ContentType`) das imagens anexadas.
* **Atualização de vários usuários de uma vez:** o endpoint `/adm/salvar-alteracoes-usuarios` altera vários usuários de uma vez, invalidando tokens antigos e gravando o log em `HistoricoUsuario`.

## Dúvidas e sugestões

Registre dúvidas, sugestões ou problemas na aba de *Issues* do repositório.
