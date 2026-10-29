package net.guilhermejr.sistema.pontoservice.leitor;

import net.guilhermejr.sistema.pontoservice.domain.enums.TipoAjuste;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Palavras montadas nas posições reais do espelho (pontos, página A4 deitada), para
 * testar o leitor sem os PDFs, que têm dados pessoais. Ver LeitorEspelhoArquivosTest.
 */
class LeitorEspelhoTest {

    // Centro de cada coluna e onde começa a Marcação e a Ocorrência (alinhadas à esquerda).
    private static final float MARCACAO = 127.6f;
    private static final float OCORRENCIA = 308.9f;
    private static final float ABONO = 446f;
    private static final float TRABALHADAS = 514f;
    private static final float COMPENSAVEIS = 581f;
    private static final float ACUMULO = 652f;
    private static final float NAO_COMPENSAVEIS = 726f;

    private static final String CABECALHO = """
            Espelho de Ponto
            Funcional: 1 Cargo: ANL ENGENHARIA TI SR
            Nome: FULANO Lotação: CENTRO TECNOLOGICO / TERREO
            Período: 01/09/2026 até 03/09/2026 Horário Contratual: 09:00-12:00-13:00-18:00
            Folha de Pagamento: 2026_10 Empresa: X
            Data de emissão: 29/10/2026
            Assinado eletronicamente em 22/10/2026 às 14:13h
            """;

    private final List<Palavra> palavras = new ArrayList<>();

    @Test
    void colunaVaziaNaoDeslizaOsValores() {

        dia(1, 153f, "08:53-12:48 13:56-17:59", null, null, "07:58", null, "04:29", null);
        dia(2, 166.6f, "05:27-12:46", null, null, "09:06", "00:33", "05:02", "00:33");
        dia(3, 180f, null, null, null, null, null, "05:02", null);

        EspelhoLido espelho = LeitorEspelho.ler(CABECALHO, palavras);

        assertThat(espelho.dias()).containsExactly(
                new EspelhoLido.Dia(LocalDate.of(2026, 9, 1), "08:53-12:48 13:56-17:59", null, null, 478, null, 269, null),
                new EspelhoLido.Dia(LocalDate.of(2026, 9, 2), "05:27-12:46", null, null, 546, 33, 302, 33),
                new EspelhoLido.Dia(LocalDate.of(2026, 9, 3), null, null, null, null, null, 302, null));
        assertThat(espelho.inicio()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(espelho.fim()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(espelho.horarioContratual()).isEqualTo("09:00-12:00-13:00-18:00");
        assertThat(espelho.folhaPagamento()).isEqualTo("2026_10");
        assertThat(espelho.cargo()).isEqualTo("ANL ENGENHARIA TI SR");
        assertThat(espelho.lotacao()).isEqualTo("CENTRO TECNOLOGICO / TERREO");
        assertThat(espelho.emitido()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(espelho.assinado()).isEqualTo(LocalDateTime.of(2026, 10, 22, 14, 13));

    }

    @Test
    void negativoEOcorrenciaComVariasPalavras() {

        dia(1, 153f, null, "Desconta do Saldo de Horas", "08:00", null, "-08:00", "-02:52", null);
        dia(2, 166.6f, null, "Férias", "08:00", null, null, "-02:52", null);
        dia(3, 180f, null, null, null, null, null, "-02:52", null);

        List<EspelhoLido.Dia> dias = LeitorEspelho.ler(CABECALHO, palavras).dias();

        assertThat(dias.get(0).ocorrencia()).isEqualTo("Desconta do Saldo de Horas");
        assertThat(dias.get(0).abono()).isEqualTo(480);
        assertThat(dias.get(0).compensaveis()).isEqualTo(-480);
        assertThat(dias.get(0).acumulo()).isEqualTo(-172);
        assertThat(dias.get(1).ocorrencia()).isEqualTo("Férias");

    }

    @Test
    void textoQuebradoNaCelulaFicaNoDiaDele() {

        dia(1, 153f, null, "Licença", null, null, null, null, null);
        palavras.add(new Palavra(1, OCORRENCIA, OCORRENCIA + 30, 158f, "Médica"));
        dia(2, 166.6f, null, null, null, null, null, null, null);
        dia(3, 180f, null, null, null, null, null, null, null);

        List<EspelhoLido.Dia> dias = LeitorEspelho.ler(CABECALHO, palavras).dias();

        assertThat(dias.get(0).ocorrencia()).isEqualTo("Licença Médica");
        assertThat(dias.get(1).ocorrencia()).isNull();

    }

    @Test
    void ajustesLadoALadoEContinuandoNaPaginaSeguinte() {

        diasVazios();
        linha(1, 470f, 34f, "Marcações", "Incluídas");
        linha(1, 470f, 423.8f, "Marcações", "Descartadas");
        linha(1, 480f, 34f, "Data/Hora", "da", "Marcação", "Motivo");
        linha(1, 490f, 34f, "02/09/2026", "12:20", "Esquecimento", "de", "marcação");
        linha(1, 490f, 423.8f, "02/09/2026", "18:35", "Esquecimento", "de", "marcação");
        linha(2, 21.7f, 34f, "03/09/2026", "17:50");
        linha(2, 77f, 28.4f, "Eu", "DECLARO", "E", "CONCORDO");

        List<EspelhoLido.Ajuste> ajustes = LeitorEspelho.ler(CABECALHO, palavras).ajustes();

        assertThat(ajustes).containsExactly(
                new EspelhoLido.Ajuste(TipoAjuste.I, LocalDateTime.of(2026, 9, 2, 12, 20), "Esquecimento de marcação"),
                new EspelhoLido.Ajuste(TipoAjuste.I, LocalDateTime.of(2026, 9, 3, 17, 50), null),
                new EspelhoLido.Ajuste(TipoAjuste.D, LocalDateTime.of(2026, 9, 2, 18, 35), "Esquecimento de marcação"));

    }

    @Test
    void recusaDiaFaltando() {

        dia(1, 153f, null, null, null, null, null, null, null);
        dia(3, 180f, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> LeitorEspelho.ler(CABECALHO, palavras))
                .isInstanceOf(LeituraEspelhoException.class)
                .hasMessageContaining("não batem com o período");

    }

    @Test
    void recusaTextoOndeDeviaHaverHoras() {

        diasVazios();
        palavras.add(new Palavra(1, ACUMULO - 10, ACUMULO + 10, 166.6f, "abc"));

        assertThatThrownBy(() -> LeitorEspelho.ler(CABECALHO, palavras))
                .isInstanceOf(LeituraEspelhoException.class)
                .hasMessage("Valor inesperado em Acúmulo Compensação no dia 02/09/2026: abc.");

    }

    @Test
    void recusaOutroDocumento() {

        assertThatThrownBy(() -> LeitorEspelho.ler("Nota fiscal", List.of()))
                .isInstanceOf(LeituraEspelhoException.class)
                .hasMessage("O arquivo não é um espelho de ponto.");

    }

    private void diasVazios() {
        dia(1, 153f, null, null, null, null, null, null, null);
        dia(2, 166.6f, null, null, null, null, null, null, null);
        dia(3, 180f, null, null, null, null, null, null, null);
    }

    private void dia(int dia, float y, String marcacoes, String ocorrencia, String abono, String trabalhadas,
                     String compensaveis, String acumulo, String naoCompensaveis) {

        palavras.add(new Palavra(1, 28.4f, 58.4f, y, "%02d/09/2026".formatted(dia)));
        palavras.add(new Palavra(1, 60f, 74f, y, "(qua)"));
        esquerda(y, MARCACAO, marcacoes);
        esquerda(y, OCORRENCIA, ocorrencia);
        centro(y, ABONO, abono);
        centro(y, TRABALHADAS, trabalhadas);
        centro(y, COMPENSAVEIS, compensaveis);
        centro(y, ACUMULO, acumulo);
        centro(y, NAO_COMPENSAVEIS, naoCompensaveis);

    }

    /**
     * Palavras alinhadas à esquerda a partir de x, com 3 pontos por caractere, como no
     * PDF ("Desconta do Saldo de Horas" vai de 308,9 a 385,7).
     */
    private void esquerda(float y, float x, String texto) {
        if (texto != null) {
            for (String p : texto.split(" ")) {
                palavras.add(new Palavra(1, x, x + p.length() * 3, y, p));
                x += p.length() * 3 + 3;
            }
        }
    }

    private void centro(float y, float centro, String texto) {
        if (texto != null) {
            palavras.add(new Palavra(1, centro - 8, centro + 8, y, texto));
        }
    }

    private void linha(int pagina, float y, float x, String... textos) {
        for (String t : textos) {
            palavras.add(new Palavra(pagina, x, x + t.length() * 5, y, t));
            x += t.length() * 5 + 3;
        }
    }

}
