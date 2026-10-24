package school.sptech.megusta.notificacao;

public record AlertaEstoque(
        String nome,
        String codigoInsumo,
        Double qtdAtual,
        Double estoqueMinimo
) {
}
