package net.guilhermejr.sistema.pontoservice.util;

import net.guilhermejr.sistema.pontoservice.leitor.EspelhoLido;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Dias tirados do espelho real de 09/2026 (e de outros meses, para os casos raros). */
class ResumoTest {

    private static final String HORARIO = "09:00-12:00-13:00-18:00";

    @Test
    void somaOQueOEspelhoTrazEContaDiasUteis() {

        List<EspelhoLido.Dia> dias = List.of(
                dia(1, "05:27-12:46 13:58-15:45", null, null, 546, 33, 269, 33),   // extra e não compensável
                dia(2, "08:53-12:48 13:56-17:59", null, null, 478, null, 269, null), // dentro da tolerância
                dia(9, "09:45-11:11 15:20-18:12", null, null, 258, -222, 47, null),  // saiu cedo
                dia(5, null, null, null, null, null, 47, null),                      // sábado
                dia(19, "08:38-12:59 14:11-14:27", null, null, 277, 415, 607, null), // sábado trabalhado
                dia(20, null, "Férias", 480, null, null, 607, null),
                dia(21, "08:15-12:00", null, 154, 326, null, 607, null));          // abono parcial

        Resumo r = Resumo.calcular(HORARIO, dias);

        assertThat(r.jornadaDiaria()).isEqualTo(480);
        assertThat(r.diasUteis()).isEqualTo(5);
        assertThat(r.previstas()).isEqualTo(5 * 480);
        assertThat(r.diasTrabalhados()).isEqualTo(5);
        assertThat(r.trabalhadas()).isEqualTo(546 + 478 + 258 + 277 + 326);
        assertThat(r.abonos()).isEqualTo(634);
        assertThat(r.extras()).isEqualTo(33 + 415);
        assertThat(r.faltas()).isEqualTo(-222);
        assertThat(r.compensaveis()).isEqualTo(33 - 222 + 415);
        assertThat(r.naoCompensaveis()).isEqualTo(33);
        assertThat(r.saldo()).isEqualTo(607);
        assertThat(r.ocorrencias()).isEqualTo(Map.of("Férias", 1L));

    }

    @Test
    void saldoEhOUltimoAcumuloComValor() {

        List<EspelhoLido.Dia> dias = List.of(
                dia(1, null, null, null, null, null, 92, null),
                dia(2, null, null, null, null, null, null, null));

        assertThat(Resumo.calcular(HORARIO, dias).saldo()).isEqualTo(92);
        assertThat(Resumo.calcular(HORARIO, List.of(dia(1, null, null, null, null, null, null, null))).saldo()).isNull();

    }

    private static EspelhoLido.Dia dia(int dia, String marcacoes, String ocorrencia, Integer abono, Integer trabalhadas,
                                       Integer compensaveis, Integer acumulo, Integer naoCompensaveis) {
        return new EspelhoLido.Dia(LocalDate.of(2026, 9, dia), marcacoes, ocorrencia, abono, trabalhadas,
                compensaveis, acumulo, naoCompensaveis);
    }

}
