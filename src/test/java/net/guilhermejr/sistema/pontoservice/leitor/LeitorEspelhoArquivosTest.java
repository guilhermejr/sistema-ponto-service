package net.guilhermejr.sistema.pontoservice.leitor;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confere o leitor com os espelhos reais, que não ficam no repositório (têm dados
 * pessoais). Roda só quando a pasta é informada:
 *
 * <pre>./mvnw test -Dtest=LeitorEspelhoArquivosTest -Dponto.pasta=/caminho/Ponto -Dponto.esperado=/caminho/esperado.json</pre>
 *
 * O JSON esperado foi gerado à parte, com o pdfplumber lendo a tabela pelas bordas
 * desenhadas: [data, marcação, ocorrência, abono, trabalhadas, compensáveis, acúmulo,
 * não compensáveis] por dia e [tipo, "dd/MM/yyyy HH:mm", motivo sem espaços] por ajuste.
 */
@EnabledIfSystemProperty(named = "ponto.pasta", matches = "/.+")
class LeitorEspelhoArquivosTest {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static Stream<Path> arquivos() throws IOException {
        return Files.list(Path.of(System.getProperty("ponto.pasta")))
                .filter(p -> p.toString().endsWith(".pdf"))
                .sorted();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arquivos")
    void leComoOEsperado(Path arquivo) throws IOException {

        EspelhoLido espelho = LeitorEspelho.ler(Files.readAllBytes(arquivo));

        JsonNode esperado = JsonMapper.builder().build()
                .readTree(Path.of(System.getProperty("ponto.esperado")).toFile())
                .get(arquivo.getFileName().toString());

        List<List<String>> dias = new ArrayList<>();
        for (EspelhoLido.Dia d : espelho.dias()) {
            dias.add(List.of(d.data().format(DATA), texto(d.marcacoes()), texto(d.ocorrencia()), horas(d.abono()),
                    horas(d.trabalhadas()), horas(d.compensaveis()), horas(d.acumulo()), horas(d.naoCompensaveis())));
        }
        assertThat(dias).isEqualTo(lista(esperado.get("dias")));

        List<List<String>> ajustes = new ArrayList<>();
        for (EspelhoLido.Ajuste a : espelho.ajustes()) {
            ajustes.add(List.of(a.tipo().name(), a.dataHora().format(DATA_HORA), texto(a.motivo()).replace(" ", "")));
        }
        assertThat(ajustes).isEqualTo(lista(esperado.get("ajustes")));

        assertThat(espelho.inicio()).isEqualTo(espelho.dias().getFirst().data());
        assertThat(espelho.horarioContratual()).isEqualTo("09:00-12:00-13:00-18:00");
        assertThat(espelho.folhaPagamento()).matches("\\d{4}_\\d{2}");
        assertThat(espelho.cargo()).isNotBlank();
        assertThat(espelho.lotacao()).isNotBlank();
        assertThat(espelho.emitido()).isNotNull();
        assertThat(espelho.assinado()).isNotNull();

    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private static String horas(Integer minutos) {
        if (minutos == null) {
            return "";
        }
        int abs = Math.abs(minutos);
        return (minutos < 0 ? "-" : "") + String.format("%02d:%02d", abs / 60, abs % 60);
    }

    private static List<List<String>> lista(JsonNode node) {
        List<List<String>> lista = new ArrayList<>();
        node.forEach(linha -> {
            List<String> valores = new ArrayList<>();
            linha.forEach(v -> valores.add(v.asString()));
            lista.add(valores);
        });
        return lista;
    }

}
