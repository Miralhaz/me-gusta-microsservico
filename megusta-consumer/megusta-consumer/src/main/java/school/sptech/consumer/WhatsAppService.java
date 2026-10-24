package school.sptech.consumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
@Service
public class WhatsAppService {
 private final ObjectMapper mapper;
 private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 private final String token, template, language;
 private final URI endpoint;
 public WhatsAppService(ObjectMapper mapper, @Value("${whatsapp.token}") String token,
     @Value("${whatsapp.phone-number-id}") String phoneId, @Value("${whatsapp.api-version}") String version,
     @Value("${whatsapp.template}") String template, @Value("${whatsapp.language}") String language) {
  if (token.isBlank() || template.isBlank() || language.isBlank()
      || !phoneId.matches("[0-9]+") || !version.matches("v[0-9]+\\.[0-9]+"))
   throw new IllegalArgumentException("Configure as variáveis WHATSAPP_* corretamente");
  this.mapper=mapper; this.token=token; this.template=template; this.language=language;
  endpoint=URI.create("https://graph.facebook.com/" + version + "/" + phoneId + "/messages");
 }
 public String enviar(String telefone, String resumo) throws Exception {
  var payload = Map.of("messaging_product", "whatsapp", "to", telefone, "type", "template",
      "template", Map.of("name", template, "language", Map.of("code", language),
          "components", List.of(Map.of("type", "body", "parameters",
              List.of(Map.of("type", "text", "text", resumo))))));
  var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(30))
      .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
  HttpResponse<String> response;
  try { response = client.send(request, HttpResponse.BodyHandlers.ofString()); }
  catch (InterruptedException e) { Thread.currentThread().interrupt(); throw e; }
  if (response.statusCode() < 200 || response.statusCode() >= 300)
   throw new IllegalStateException("WhatsApp recusou envio. HTTP " + response.statusCode());
  String id=mapper.readTree(response.body()).path("messages").path(0).path("id").asText();
  if (id.isBlank()) throw new IllegalStateException("WhatsApp não retornou ID de mensagem");
  return id;
 }
}
