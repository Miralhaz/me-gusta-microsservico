# Me Gusta — Consumer de alertas para WhatsApp

Projeto independente **Java 21 / Spring Boot 3.5.13**. Inclui Maven Wrapper da base e Dockerfile com Java 21. 

O consumer não precisa de MySQL, JPA, endpoints HTTP nem JWT. 
As dependências incluem somente mensageria, JSON e testes. 

## Como funciona

RabbitAlertaEstoquePublisher do backend → topic exchange → fila deste consumer → AlertaEstoqueConsumer → ObjectMapper → WhatsAppService → API oficial da Meta.

O producer já serializa AlertaEstoqueMensagem para JSON. Não existe arquivo PDF, Excel, URL ou anexo nesse contrato. Este consumer recebe os bytes JSON da mensagem e os desserializa; não lê um arquivo do disco nem envia documentos ao WhatsApp.

Classes:
- RabbitConfig: declara exchange durável, fila, binding e fila de falhas (DLQ).
- AlertaEstoqueMensagem: mesmo contrato do producer, com telefone e alertas.
- AlertaEstoqueConsumer: recebe Message e usa ObjectMapper.readValue. Ignora o cabeçalho __TypeId__ do producer, evitando depender do pacote Java dele.
- MensagemFormatter: valida os campos, normaliza telefone e monta resumo.
- WhatsAppService: POST HTTPS para /PHONE_NUMBER_ID/messages com Bearer token e template.

## 1. Configurar RabbitMQ

Use o MESMO host, porta, virtual host, usuário e senha do producer. O usuário precisa de permissões para declarar exchange/fila/binding e consumir.

Defaults confirmados na base: exchange `megusta.estoque`, routing key `alerta.estoque`, host `localhost`, porta `5672` e virtual host `/`. Se sobrescrever as variáveis no backend, use os mesmos valores no consumer:

| Backend | Variável do consumer |
|---|---|
| megusta.rabbitmq.exchange | RABBITMQ_EXCHANGE |
| megusta.rabbitmq.routing-key | RABBITMQ_ROUTING_KEY |

RABBITMQ_QUEUE é opcional; padrão: megusta.whatsapp.alertas. Use uma fila nova se já houver uma com argumentos diferentes; RabbitMQ rejeita redeclarações incompatíveis. Não apague uma fila com mensagens para resolver esse erro.

Inicie o consumer ANTES de disparar o producer pela primeira vez, para criar a fila e binding. Um exchange sem fila vinculada não guarda as mensagens anteriores. O producer fornecido não configura aqui confirmação de publicação/retorno de mensagens não roteadas; isso é uma melhoria separada.

## 2. Preparar WhatsApp Cloud API

Este projeto assume a API oficial da Meta; Twilio, Z-API e Evolution têm contratos distintos e exigem adaptar WhatsAppService.

No painel Meta, configure o aplicativo WhatsApp, número remetente, token válido com permissão de envio e destinatário autorizado para o teste. WHATSAPP_PHONE_NUMBER_ID é o ID do número REMETENTE no painel, não o telefone do cliente e não o WABA ID. Use somente destinatários que autorizaram as notificações.

Crie e obtenha aprovação de um template em pt_BR com UMA variável posicional no corpo, sem cabeçalho/botões obrigatórios. Sugestão de nome: alerta_estoque_megusta. Sugestão de corpo:

> Alerta de estoque Me Gusta: {{1}}. Confira os insumos e programe a reposição.

Exemplo para aprovação da variável: Farinha (INS001): atual 2, mínimo 5

O código envia o resumo completo nessa variável. O nome e idioma precisam coincidir com o template aprovado. A Meta decide a aprovação e categoria. Fora da janela de atendimento de 24 horas, utilize templates aprovados; este consumer usa template em todos os envios. Escolha uma versão da Graph API suportada pelo seu app e informe WHATSAPP_API_VERSION (formato vNN.0).

## 3. Executar no Windows PowerShell

Instale JDK 21. O Maven Wrapper baixa a distribuição configurada na base, sem exigir Maven instalado. Dentro desta pasta, defina:

```powershell
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USERNAME="guest"
$env:RABBITMQ_PASSWORD="guest"
$env:RABBITMQ_VHOST="/"
$env:RABBITMQ_EXCHANGE="megusta.estoque"
$env:RABBITMQ_ROUTING_KEY="alerta.estoque"
$env:WHATSAPP_TOKEN="TOKEN_DA_META"
$env:WHATSAPP_PHONE_NUMBER_ID="ID_NUMERICO_DO_REMETENTE"
$env:WHATSAPP_API_VERSION="VERSAO_SUPORTADA_NO_SEU_APP"
$env:WHATSAPP_TEMPLATE="alerta_estoque_megusta"
$env:WHATSAPP_LANGUAGE="pt_BR"
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

Os placeholders devem ser substituídos. Não envie seu token no chat nem o versione. Também pode configurar as variáveis no Run Configuration da IDE. Não há carregamento automático de arquivo .env.

O guest normalmente só funciona localmente no broker. Para acesso remoto, use usuário próprio. Em produção, habilite TLS conforme sua infraestrutura (`spring.rabbitmq.ssl.enabled=true` e certificados/porta apropriados).

## 4. Testar o fluxo

1. Inicie RabbitMQ e o consumer com credenciais válidas.
2. Confirme a fila e o binding no painel RabbitMQ.
3. No backend, dispare o fluxo existente de estoque abaixo do mínimo para um destinatário de teste autorizado. Ou publique o conteúdo de exemplo-mensagem.json no exchange, com a routing key configurada e content_type application/json. Troque o telefone fictício por um número de teste com DDI + DDD + número, apenas dígitos.
4. A variável de template receberá: `Farinha (INS001): atual 2, mínimo 5`.
5. O log informa o ID retornado pela Meta. Isso significa ACEITO pela API, não confirmação de entrega no aparelho. Confirmação de entrega/leitura requer webhook da Meta, não incluído.
6. Para exercitar falha sem enviar nada, publique JSON inválido. Após três tentativas, a mensagem deve ir para megusta.whatsapp.alertas.dlq. Corrija o problema antes de republicar manualmente; não há reenvio automático da DLQ.

## Erros e limites

- ACK automático somente após o listener retornar com sucesso; não é `autoAck` do protocolo RabbitMQ.
- Até três tentativas totais, com intervalos de 2 e 4 segundos. A rejeição final encaminha à DLQ declarada. Erros permanentes também usam essas três tentativas limitadas.
- Timeouts de conexão de 10 s e requisição de 30 s. Não imprime token, telefone nem corpo da resposta de erro. HTTP 400: revise template e parâmetros; 401/403: token/permissões; 429: limite; 5xx: indisponibilidade.
- Quantidades inválidas e listas vazias falham. Telefone deve conter DDI; o código não adivinha nem acrescenta 55. A validação sintática não confirma existência do número.
- Resumo limitado a 800 caracteres para o template sugerido. Excedentes vão à DLQ, sem truncar itens. Para muitos insumos, implemente divisão no producer, observando os limites do template aprovado.
- Pode haver DUPLICIDADE se a Meta aceitar o envio e houver timeout ou queda antes do ACK. O contrato original não tem ID de evento e esta implementação não promete exactly-once. Produção pode exigir eventId, registro persistente e reconciliação; deduplicação local sozinha não elimina a ambiguidade de uma chamada HTTP aceita sem resposta.
- DLQ durável auxilia inspeção, mas não substitui políticas de alta disponibilidade, backup e monitoramento do broker.

## Validação desta entrega

Contrato e código revisados contra os arquivos do producer anexado. Foram incluídos testes JUnit/Mockito para conversão do contrato (inclusive __TypeId__), erro de envio, JSON inválido e lista vazia. A tentativa de `./mvnw -B test` iniciou o Maven, mas falhou antes da compilação por erro de DNS ao acessar repo.maven.apache.org para resolver o parent Spring Boot 3.5.13. Portanto os testes NÃO foram executados. O ambiente também não possui JDK 21 configurado. Nenhuma mensagem real foi enviada e não houve teste integrado com RabbitMQ ou Meta. Execute `./mvnw.cmd test` e o roteiro acima no seu ambiente antes do uso.

## Referências

- https://docs.spring.io/spring-boot/reference/messaging/amqp.html
- https://docs.spring.io/spring-amqp/reference/amqp/resilience-recovering-from-errors-and-broker-failures.html
- https://developers.facebook.com/documentation/business-messaging/whatsapp/get-started
- https://developers.facebook.com/documentation/business-messaging/whatsapp/messages/send-messages
- https://whatsapp.github.io/WhatsApp-Nodejs-SDK/api-reference/messages/template/

## Comportamento do backend identificado

`VerificacaoEstoqueScheduler` tem cron `0 0 7 * * *`: dispara às 7h no fuso da aplicação. Obtém somente o usuário mais recente (`findTopByOrderByIdDesc`) e publica para esse telefone.

## Docker e Linux/macOS

No Linux/macOS, use `./mvnw test` e `./mvnw spring-boot:run`, depois de exportar as variáveis equivalentes.

Para compilar e executar via Docker (a construção também executa os testes):

```sh
docker build -t megusta-consumer .
docker run --rm --env-file .env megusta-consumer
```

Crie `.env` localmente com as mesmas variáveis da seção PowerShell, em linhas `CHAVE=valor`, sem `$env:`. Não o versione. Dentro de um container, `localhost` aponta para o próprio container: use o nome do serviço RabbitMQ na mesma rede Docker (`--network NOME_DA_REDE`) ou o endereço do broker. No Docker Desktop, um broker no computador host pode ser acessado por `host.docker.internal`. Não é necessário publicar porta para o consumer.
