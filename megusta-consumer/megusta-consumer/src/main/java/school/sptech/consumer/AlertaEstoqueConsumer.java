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
 private final EmailService emailService;
 public AlertaEstoqueConsumer(ObjectMapper mapper, EmailService emailService) {
  this.mapper=mapper; this.emailService=emailService;
 }
 @RabbitListener(queues="${megusta.rabbitmq.queue}")
 public void consumir(Message message) throws Exception {

  AlertaEstoqueMensagem dto=mapper.readValue(message.getBody(), AlertaEstoqueMensagem.class);
  String resumo=MensagemFormatter.resumo(dto);
  String email=MensagemFormatter.email(dto.email());
  emailService.enviar(email, resumo);
  log.info("E-mail de alerta de estoque aceito para {}.", email);
  
 }
}
