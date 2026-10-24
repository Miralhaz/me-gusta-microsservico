package school.sptech.consumer;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class RabbitConfig {
 @Bean
 public Declarables topologia(@Value("${megusta.rabbitmq.exchange}") String exchange,
                             @Value("${megusta.rabbitmq.routing-key}") String key,
                             @Value("${megusta.rabbitmq.queue}") String queue) {
  TopicExchange principal = new TopicExchange(exchange, true, false);
  DirectExchange erros = new DirectExchange(queue + ".dlx", true, false);
  Queue fila = QueueBuilder.durable(queue).deadLetterExchange(erros.getName())
      .deadLetterRoutingKey(queue + ".dlq").build();
  Queue dlq = QueueBuilder.durable(queue + ".dlq").build();
  return new Declarables(principal, erros, fila, dlq,
      BindingBuilder.bind(fila).to(principal).with(key),
      BindingBuilder.bind(dlq).to(erros).with(queue + ".dlq"));
 }
}
