package school.sptech.consumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ConsumerTest {
 private final ObjectMapper mapper=new ObjectMapper();
 private Message message(String body) { return new Message(body.getBytes(java.nio.charset.StandardCharsets.UTF_8), new MessageProperties()); }
 private final String json="""
 {"telefone":"+55 (11) 99999-9999","alertas":[{"nome":"Farinha","codigoInsumo":"INS001","qtdAtual":2,"estoqueMinimo":5}]}
 """;
 @Test void converteContratoDoProducerEEnvia() throws Exception {
  var service=mock(WhatsAppService.class);
  var msg=message(json);
  msg.getMessageProperties().setHeader("__TypeId__", "school.sptech.megusta.notificacao.AlertaEstoqueMensagem");
  new AlertaEstoqueConsumer(mapper, service).consumir(msg);
  verify(service).enviar("5511999999999", "Farinha (INS001): atual 2, mínimo 5");
 }
 @Test void falhaDeEnvioPropagaParaRetry() throws Exception {
  var service=mock(WhatsAppService.class);
  doThrow(new IllegalStateException("HTTP 503")).when(service).enviar(anyString(), anyString());
  assertThrows(IllegalStateException.class, () -> new AlertaEstoqueConsumer(mapper,service).consumir(message(json)));
 }
 @Test void jsonInvalidoNaoEnvia() {
  var service=mock(WhatsAppService.class);
  assertThrows(Exception.class, () -> new AlertaEstoqueConsumer(mapper,service).consumir(message("arquivo inválido")));
  verifyNoInteractions(service);
 }
 @Test void listaVaziaNaoEnvia() {
  var service=mock(WhatsAppService.class);
  assertThrows(IllegalArgumentException.class, () -> new AlertaEstoqueConsumer(mapper,service).consumir(message("{\"telefone\":\"5511999999999\",\"alertas\":[]}")));
  verifyNoInteractions(service);
 }
}
