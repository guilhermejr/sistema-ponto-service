package net.guilhermejr.sistema.pontoservice.leitor;

import net.guilhermejr.sistema.pontoservice.domain.enums.TipoAjuste;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** O que foi lido de um espelho, antes de virar entidade. Horas em minutos, com sinal. */
public record EspelhoLido(
        LocalDate inicio,
        LocalDate fim,
        String horarioContratual,
        String folhaPagamento,
        String cargo,
        String lotacao,
        LocalDate emitido,
        LocalDateTime assinado,
        List<Dia> dias,
        List<Ajuste> ajustes) {

    public record Dia(
            LocalDate data,
            String marcacoes,
            String ocorrencia,
            Integer abono,
            Integer trabalhadas,
            Integer compensaveis,
            Integer acumulo,
            Integer naoCompensaveis) {
    }

    public record Ajuste(TipoAjuste tipo, LocalDateTime dataHora, String motivo) {
    }

}
