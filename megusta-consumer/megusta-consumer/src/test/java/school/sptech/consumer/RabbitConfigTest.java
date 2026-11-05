package school.sptech.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes da topologia RabbitMQ")
class RabbitConfigTest {

    private static final String EXCHANGE = "megusta.estoque";
    private static final String ROUTING_KEY = "alerta.estoque";
    private static final String QUEUE = "megusta.email.alertas";

    @Test
    @DisplayName("deve derivar fila e DLQ do nome configurado, mantendo exchange e routing key")
    void deveDerivarTopologiaDoNomeDaFila() {
        Declarables declarables = new RabbitConfig().topologia(EXCHANGE, ROUTING_KEY, QUEUE);

        List<Queue> filas = new ArrayList<>(declarables.getDeclarablesByType(Queue.class));
        assertThat(filas).extracting(Queue::getName)
                .containsExactlyInAnyOrder(QUEUE, QUEUE + ".dlq");

        Queue fila = filas.stream().filter(q -> q.getName().equals(QUEUE)).findFirst().orElseThrow();
        assertThat(fila.isDurable()).isTrue();
        assertThat(fila.getArguments())
                .containsEntry("x-dead-letter-exchange", QUEUE + ".dlx")
                .containsEntry("x-dead-letter-routing-key", QUEUE + ".dlq");

        Queue dlq = filas.stream().filter(q -> q.getName().equals(QUEUE + ".dlq")).findFirst().orElseThrow();
        assertThat(dlq.isDurable()).isTrue();

        List<TopicExchange> topicExchanges = new ArrayList<>(
                declarables.getDeclarablesByType(TopicExchange.class));
        assertThat(topicExchanges).extracting(TopicExchange::getName).containsExactly(EXCHANGE);
        assertThat(topicExchanges.get(0).isDurable()).isTrue();

        List<DirectExchange> directExchanges = new ArrayList<>(
                declarables.getDeclarablesByType(DirectExchange.class));
        assertThat(directExchanges).extracting(DirectExchange::getName).containsExactly(QUEUE + ".dlx");

        List<Binding> bindings = new ArrayList<>(declarables.getDeclarablesByType(Binding.class));
        assertThat(bindings).anySatisfy(binding -> {
            assertThat(binding.getExchange()).isEqualTo(EXCHANGE);
            assertThat(binding.getDestination()).isEqualTo(QUEUE);
            assertThat(binding.getRoutingKey()).isEqualTo(ROUTING_KEY);
        });
        assertThat(bindings).anySatisfy(binding -> {
            assertThat(binding.getExchange()).isEqualTo(QUEUE + ".dlx");
            assertThat(binding.getDestination()).isEqualTo(QUEUE + ".dlq");
            assertThat(binding.getRoutingKey()).isEqualTo(QUEUE + ".dlq");
        });
    }
}
