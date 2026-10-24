package school.sptech.consumer;
import school.sptech.megusta.notificacao.AlertaEstoqueMensagem;
import java.math.BigDecimal;
import java.util.stream.Collectors;
public final class MensagemFormatter {
 private MensagemFormatter() {}
 public static String telefone(String value) {
  if (value == null || !value.matches("[+0-9() .-]+")) throw new IllegalArgumentException("Telefone inválido");
  String digits = value.replaceAll("[^0-9]", "");
  if (!digits.matches("[1-9][0-9]{7,14}")) throw new IllegalArgumentException("Use telefone internacional com DDI");
  return digits;
 }
 public static String resumo(AlertaEstoqueMensagem m) {
  if (m == null || m.alertas() == null || m.alertas().isEmpty()) throw new IllegalArgumentException("Alertas vazios");
  for (var a : m.alertas()) {
   if (a == null || a.nome() == null || a.nome().isBlank() || a.codigoInsumo() == null
       || a.qtdAtual() == null || a.estoqueMinimo() == null
       || !Double.isFinite(a.qtdAtual()) || !Double.isFinite(a.estoqueMinimo())
       || a.qtdAtual() < 0 || a.estoqueMinimo() < 0)
    throw new IllegalArgumentException("Alerta inválido");
  }
  String result = m.alertas().stream().map(a -> limpar(a.nome()) + " (" + limpar(a.codigoInsumo())
      + "): atual " + numero(a.qtdAtual()) + ", mínimo " + numero(a.estoqueMinimo()))
      .collect(Collectors.joining("; "));
  if (result.length() > 800) throw new IllegalArgumentException("Resumo excede 800 caracteres; dividir no producer");
  return result;
 }
 private static String limpar(String s) { return s.replaceAll("\\s+", " ").trim(); }
 private static String numero(Double n) { return BigDecimal.valueOf(n).stripTrailingZeros().toPlainString().replace('.', ','); }
}
