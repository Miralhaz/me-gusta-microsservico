package school.sptech.consumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import school.sptech.megusta.notificacao.AlertaEstoqueMensagem;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ConsumerTest {
 private final ObjectMapper mapper=new ObjectMapper();
 private Message message(String body) { return new Message(body.getBytes(java.nio.charset.StandardCharsets.UTF_8), new MessageProperties()); }
 private final String json="""
 {"email":" responsavel@megusta.com ","alertas":[{"nome":"Farinha","codigoInsumo":"INS001","qtdAtual":2,"estoqueMinimo":5}]}
 """;
 @Test void converteContratoDoProducerEEnvia() throws Exception {
  var service=mock(EmailService.class);
  var msg=message(json);
  msg.getMessageProperties().setHeader("__TypeId__", "school.sptech.megusta.notificacao.AlertaEstoqueMensagem");
  new AlertaEstoqueConsumer(mapper, service).consumir(msg);
  verify(service).enviar("responsavel@megusta.com", "Farinha (INS001): atual 2, mínimo 5");
 }
 @Test void falhaDeEnvioPropagaParaRetry() throws Exception {
  var service=mock(EmailService.class);
  doThrow(new IllegalStateException("SMTP 503")).when(service).enviar(anyString(), anyString());
  assertThrows(IllegalStateException.class, () -> new AlertaEstoqueConsumer(mapper,service).consumir(message(json)));
 }
 @Test void jsonInvalidoNaoEnvia() {
  var service=mock(EmailService.class);
  assertThrows(Exception.class, () -> new AlertaEstoqueConsumer(mapper,service).consumir(message("arquivo inválido")));
  verifyNoInteractions(service);
 }
 @Test void listaVaziaNaoEnvia() {
  var service=mock(EmailService.class);
  assertThrows(IllegalArgumentException.class, () -> new AlertaEstoqueConsumer(mapper,service).consumir(message("{\"email\":\"responsavel@megusta.com\",\"alertas\":[]}")));
  verifyNoInteractions(service);
 }
 @Test void destinatarioInvalidoNaoEnvia() {
  var service=mock(EmailService.class);
  assertThrows(IllegalArgumentException.class, () -> new AlertaEstoqueConsumer(mapper,service).consumir(message("{\"email\":\"sem-arroba\",\"alertas\":[{\"nome\":\"Farinha\",\"codigoInsumo\":\"INS001\",\"qtdAtual\":2,\"estoqueMinimo\":5}]}")));
  verifyNoInteractions(service);
 }
 @Test void exemploMensagemDeserializa() throws Exception {
  String conteudo=Files.readString(Path.of("exemplo-mensagem.json"));
  AlertaEstoqueMensagem mensagem=mapper.readValue(conteudo, AlertaEstoqueMensagem.class);
  assertEquals("responsavel@megusta.com", mensagem.email());
  assertEquals(1, mensagem.alertas().size());
  assertEquals("Farinha", mensagem.alertas().get(0).nome());
  assertEquals("INS001", mensagem.alertas().get(0).codigoInsumo());
  assertEquals(2.0, mensagem.alertas().get(0).qtdAtual());
  assertEquals(5.0, mensagem.alertas().get(0).estoqueMinimo());
 }
}
