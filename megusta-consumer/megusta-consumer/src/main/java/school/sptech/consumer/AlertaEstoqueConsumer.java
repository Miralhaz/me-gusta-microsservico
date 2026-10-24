package school.sptech.consumer;
import school.sptech.megusta.notificacao.AlertaEstoqueMensagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.slf4j.*;
@Component
public class AlertaEstoqueConsumer {
 private static final Logger log=LoggerFactory.getLogger(AlertaEstoqueConsumer.class);
 private final ObjectMapper mapper;
 private final WhatsAppService whatsapp;
 public AlertaEstoqueConsumer(ObjectMapper mapper, WhatsAppService whatsapp) {
  this.mapper=mapper; this.whatsapp=whatsapp;
 }
 @RabbitListener(queues="${megusta.rabbitmq.queue}")
 public void consumir(Message message) throws Exception {
  // Lê diretamente os bytes JSON; não depende do __TypeId__ do pacote do producer.
  AlertaEstoqueMensagem dto=mapper.readValue(message.getBody(), AlertaEstoqueMensagem.class);
  String resumo=MensagemFormatter.resumo(dto);
  String telefone=MensagemFormatter.telefone(dto.telefone());
  String id=whatsapp.enviar(telefone, resumo);
  log.info("Notificação aceita pela API do WhatsApp. ID: {}", id);
  // AUTO confirma no RabbitMQ somente depois do retorno sem exceção.
  // Não capturar e suprimir erros aqui: eles precisam chegar ao retry/DLQ.
 }
}
