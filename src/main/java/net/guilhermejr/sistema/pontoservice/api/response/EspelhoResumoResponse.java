package net.guilhermejr.sistema.pontoservice.api.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/** Um mês com os totais. Horas em minutos, com sinal. */
@Getter
@Builder
public class EspelhoResumoResponse {

    private Integer ano;
    private Integer mes;
    private LocalDate inicio;
    private LocalDate fim;
    private String horarioContratual;
    private String folhaPagamento;
    private String cargo;
    private String lotacao;
    private LocalDate emitido;
    private LocalDateTime assinado;

    private Integer jornadaDiaria;
    private Integer diasUteis;
    private Integer diasTrabalhados;
    private Integer previstas;
    private Integer trabalhadas;
    private Integer abonos;
    private Integer extras;
    private Integer faltas;
    private Integer compensaveis;
    private Integer naoCompensaveis;
    /** Acúmulo do último dia com valor; null num mês sem nenhum. */
    private Integer saldo;
    /** Dias por ocorrência ("Férias" → 10). */
    private Map<String, Long> ocorrencias;

}
