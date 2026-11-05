# Me Gusta — Consumer de alertas de estoque por e-mail

Projeto independente **Java 21 / Spring Boot 3.5.13**. Inclui Maven Wrapper e Dockerfile com Java 21.

O consumer não precisa de MySQL, JPA, endpoints HTTP nem JWT. As dependências incluem mensageria RabbitMQ, Jackson e Spring Mail (SMTP).

## Como funciona

```
VerificacaoEstoqueScheduler (backend, 07:00)
  → NotificacaoEstoqueService
  → RabbitAlertaEstoquePublisher
  → exchange megusta.estoque (routing key alerta.estoque)
  → fila megusta.email.alertas
  → AlertaEstoqueConsumer
  → MensagemFormatter (validação e resumo)
  → EmailService
  → servidor SMTP
```

O producer serializa `AlertaEstoqueMensagem` em JSON com o campo `email` no nível raiz e uma lista `alertas`. Este consumer recebe os bytes JSON e os desserializa; não lê arquivo do disco, não usa anexos e não depende do pacote Java do producer.

Classes principais:

- `RabbitConfig`: declara exchange durável, fila durável, binding e DLQ.
- `AlertaEstoqueMensagem`: contrato do producer, com `email` e `alertas`.
- `AlertaEstoqueConsumer`: recebe a mensagem, ignora o cabeçalho `__TypeId__` do producer, valida o contrato e aciona o envio.
- `MensagemFormatter`: valida e normaliza o e-mail, valida os itens e monta o resumo em texto puro.
- `EmailService`: envia o resumo por SMTP com o assunto `Alerta de estoque MeGusta`.
- `MailConfigurationValidator`: falha rápido no startup se a configuração SMTP obrigatória estiver ausente ou inválida.

## 1. Configurar RabbitMQ

Use o **mesmo** host, porta, virtual host, usuário e senha do producer. O usuário precisa de permissão para declarar exchange/fila/binding e consumir.

| Variável | Propriedade | Padrão | Obrigatória |
|---|---|---|---|
| `RABBITMQ_HOST` | `spring.rabbitmq.host` | `localhost` | não |
| `RABBITMQ_PORT` | `spring.rabbitmq.port` | `5672` | não |
| `RABBITMQ_USERNAME` | `spring.rabbitmq.username` | `guest` | não |
| `RABBITMQ_PASSWORD` | `spring.rabbitmq.password` | `guest` | não |
| `RABBITMQ_VHOST` | `spring.rabbitmq.virtual-host` | `/` | não |
| `RABBITMQ_EXCHANGE` | `megusta.rabbitmq.exchange` | `megusta.estoque` | não |
| `RABBITMQ_ROUTING_KEY` | `megusta.rabbitmq.routing-key` | `alerta.estoque` | não |
| `RABBITMQ_QUEUE` | `megusta.rabbitmq.queue` | `megusta.email.alertas` | não |

A fila e a DLQ são derivadas do nome configurado em `RABBITMQ_QUEUE`:

- fila: `megusta.email.alertas`
- DLX: `megusta.email.alertas.dlx`
- DLQ: `megusta.email.alertas.dlq`

Use uma fila nova se já existir uma fila com o mesmo nome e argumentos diferentes. O RabbitMQ rejeita redeclarações incompatíveis; não apague uma fila com mensagens para contornar esse erro.

Inicie o consumer **antes** de disparar o producer pela primeira vez, para criar a fila e o binding. Um exchange sem fila vinculada não guarda mensagens anteriores. O producer não configura confirmação de publicação/retorno de mensagens não roteadas; isso é uma melhoria separada.

## 2. Configurar o servidor SMTP

O consumer usa Spring Mail com autenticação e STARTTLS habilitados. Informe as credenciais do provedor SMTP por variáveis de ambiente.

| Variável | Propriedade | Padrão | Obrigatória |
|---|---|---|---|
| `MAIL_HOST` | `spring.mail.host` | — | sim |
| `MAIL_PORT` | `spring.mail.port` | `0` (inválido) | sim |
| `MAIL_USERNAME` | `spring.mail.username` | — | sim |
| `MAIL_PASSWORD` | `spring.mail.password` | — | sim |
| `MAIL_FROM` | `megusta.mail.from` | — | sim |

O startup falha com erro de configuração quando `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD` ou `MAIL_FROM` estão em branco, ou quando `MAIL_PORT` não está entre `1` e `65535`.

Timeouts SMTP:

- conexão: 10 s
- leitura/escrita: 30 s

### Pré-requisitos do provedor

- Use uma conta SMTP autenticada e uma porta aceita pelo provedor (por exemplo, 587 com STARTTLS).
- O endereço de `MAIL_FROM` deve ser autorizado pelo provedor.
- Configure **SPF** e **DKIM** no domínio remetente para reduzir rejeição/spam. Idealmente configure também DMARC.
- Não versione `MAIL_PASSWORD`. Não há carregamento automático de arquivo `.env`.
- Provedores de teste como Mailtrap são úteis para validar o fluxo sem enviar e-mails reais.

## 3. Executar no Windows PowerShell

Instale o JDK 21. O Maven Wrapper baixa a distribuição configurada, sem exigir Maven instalado. Dentro desta pasta, defina as variáveis e execute:

```powershell
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USERNAME="guest"
$env:RABBITMQ_PASSWORD="guest"
$env:RABBITMQ_VHOST="/"
$env:RABBITMQ_EXCHANGE="megusta.estoque"
$env:RABBITMQ_ROUTING_KEY="alerta.estoque"
$env:MAIL_HOST="smtp.seu-provedor.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="usuario-smtp"
$env:MAIL_PASSWORD="senha-smtp"
$env:MAIL_FROM="alertas@seu-dominio.com"
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

Os placeholders devem ser substituídos. Não envie sua senha no chat nem a versione. Também é possível configurar as variáveis no Run Configuration da IDE.

O `guest` normalmente só funciona localmente no broker. Para acesso remoto, use um usuário próprio. Em produção, habilite TLS também na conexão com o RabbitMQ conforme sua infraestrutura (`spring.rabbitmq.ssl.enabled=true` e certificados/porta apropriados).

## 4. Testar o fluxo manualmente

1. Inicie o RabbitMQ e o consumer com as credenciais SMTP e RabbitMQ válidas.
2. Confirme no painel do RabbitMQ a fila `megusta.email.alertas`, a DLQ `megusta.email.alertas.dlq` e o binding no exchange `megusta.estoque` com routing key `alerta.estoque`.
3. Publique o conteúdo de `exemplo-mensagem.json` no exchange `megusta.estoque` com routing key `alerta.estoque` e `content_type` `application/json`. Na interface de administração, use **Publish message**; via linha de comando:

   ```sh
   rabbitmqadmin publish exchange=megusta.estoque routing_key=alerta.estoque \
     payload="$(cat exemplo-mensagem.json)" properties='{"content_type":"application/json"}'
   ```

4. Troque o e-mail fictício do exemplo por um endereço de teste que você controla. O corpo esperado é:

   ```text
   Farinha (INS001): atual 2, mínimo 5
   ```

5. Confira o log `E-mail de alerta de estoque aceito para <email>` e a mensagem na caixa de entrada. O log indica que o SMTP aceitou o envio; ele não confirma entrega/leitura no destinatário.
6. Para exercitar falha sem enviar nada, publique JSON inválido ou um destinatário inválido. Após três tentativas, a mensagem deve ir para `megusta.email.alertas.dlq`. Corrija o problema antes de republicar manualmente; não há reenvio automático da DLQ.

Para o teste com o backend, dispare o fluxo existente de estoque abaixo do mínimo. O backend publica o alerta para o e-mail do usuário mais recente (`findTopByOrderByIdDesc`).

## Retry e DLQ

- ACK automático somente após o listener retornar com sucesso.
- Até três tentativas totais, com intervalos crescentes de aproximadamente 2 e 4 segundos.
- Se todas as tentativas falharem, a mensagem é rejeitada sem requeue e encaminhada para a DLQ durável `megusta.email.alertas.dlq`.
- Erros permanentes (JSON inválido, lista vazia, item inválido, e-mail inválido, resumo acima de 800 caracteres) também passam pelas três tentativas limitadas antes da DLQ.
- Timeouts de SMTP: 10 s para conexão e 30 s para leitura/escrita.
- Falhas de autenticação/autorização SMTP, limite de envio e indisponibilidade do provedor são reportadas pelo log de erro do listener.
- Pode haver **duplicidade**: se o SMTP aceitar o envio e houver timeout ou queda antes do ACK, a mensagem pode ser reenviada. O contrato não possui ID de evento e a implementação não promete exactly-once.
- A DLQ durável auxilia inspeção, mas não substitui políticas de alta disponibilidade, backup e monitoramento do broker.

## Validação

Os testes unitários cobrem o contrato JSON (com `email` e sem `telefone`), o cabeçalho `__TypeId__` do producer, a formatação/validação de e-mail e resumo, o envio via `JavaMailSender` mockado, a falha de envio, JSON inválido, lista vazia, destinatário inválido, a topologia RabbitMQ e a validação de configuração SMTP.

Execute:

```sh
./mvnw test
```

## Docker e Linux/macOS

No Linux/macOS, use `./mvnw test` e `./mvnw spring-boot:run` depois de exportar as variáveis equivalentes. Se o wrapper não estiver executável, use `bash mvnw`.

Para compilar e executar via Docker (a construção também executa os testes):

```sh
docker build -t megusta-consumer .
docker run --rm --env-file .env megusta-consumer
```

Crie `.env` localmente com as mesmas variáveis das tabelas acima, em linhas `CHAVE=valor`, sem `$env:`. Não o versione. Dentro de um container, `localhost` aponta para o próprio container: use o nome do serviço RabbitMQ na mesma rede Docker (`--network NOME_DA_REDE`) ou o endereço do broker. No Docker Desktop, um broker no computador host pode ser acessado por `host.docker.internal`. Não é necessário publicar porta para o consumer.

## Referências

- https://docs.spring.io/spring-boot/reference/io/email.html
- https://docs.spring.io/spring-boot/reference/messaging/amqp.html
- https://docs.spring.io/spring-amqp/reference/amqp/resilience-recovering-from-errors-and-broker-failures.html
