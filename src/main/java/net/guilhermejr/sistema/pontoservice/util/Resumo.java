package net.guilhermejr.sistema.pontoservice.util;

import net.guilhermejr.sistema.pontoservice.leitor.EspelhoLido;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Totais de um mês a partir dos dias do espelho. Só soma o que a empresa calculou;
 * a única conta própria é a de dias úteis (ver {@link #diaUtil}).
 */
public record Resumo(
        int jornadaDiaria,
        int diasUteis,
        int diasTrabalhados,
        int previstas,
        int trabalhadas,
        int abonos,
        int extras,
        int faltas,
        int compensaveis,
        int naoCompensaveis,
        Integer saldo,
        Map<String, Long> ocorrencias) {

    public static Resumo calcular(String horarioContratual, List<EspelhoLido.Dia> dias) {

        int jornada = Horas.jornada(horarioContratual);
        int diasUteis = (int) dias.stream().filter(d -> diaUtil(d, jornada)).count();

        Integer saldo = null;
        for (EspelhoLido.Dia d : dias) {
            if (d.acumulo() != null) {
                saldo = d.acumulo();
            }
        }

        Map<String, Long> ocorrencias = dias.stream()
                .map(EspelhoLido.Dia::ocorrencia)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));

        return new Resumo(
                jornada,
                diasUteis,
                (int) dias.stream().filter(d -> valor(d.trabalhadas()) > 0).count(),
                diasUteis * jornada,
                somar(dias, EspelhoLido.Dia::trabalhadas),
                somar(dias, EspelhoLido.Dia::abono),
                dias.stream().mapToInt(d -> Math.max(valor(d.compensaveis()), 0)).sum(),
                dias.stream().mapToInt(d -> Math.min(valor(d.compensaveis()), 0)).sum(),
                somar(dias, EspelhoLido.Dia::compensaveis),
                somar(dias, EspelhoLido.Dia::naoCompensaveis),
                saldo,
                ocorrencias);

    }

    /**
     * Dia em que havia jornada a cumprir. O espelho não diz isso, mas fecha a conta:
     * trabalhadas + abono − compensáveis − não compensáveis é a jornada (8h) num dia
     * útil e zero num fim de semana ou feriado, trabalhado ou não. A empresa ignora
     * diferenças de poucos minutos (tolerância), daí comparar com meia jornada.
     */
    static boolean diaUtil(EspelhoLido.Dia d, int jornada) {
        int devidas = valor(d.trabalhadas()) + valor(d.abono()) - valor(d.compensaveis()) - valor(d.naoCompensaveis());
        return devidas * 2 >= jornada;
    }

    private static int somar(List<EspelhoLido.Dia> dias, Function<EspelhoLido.Dia, Integer> campo) {
        return dias.stream().mapToInt(d -> valor(campo.apply(d))).sum();
    }

    private static int valor(Integer minutos) {
        return minutos == null ? 0 : minutos;
    }

}
