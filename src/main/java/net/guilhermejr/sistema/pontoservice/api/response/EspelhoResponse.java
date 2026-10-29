package net.guilhermejr.sistema.pontoservice.api.response;

import lombok.Builder;
import lombok.Getter;
import net.guilhermejr.sistema.pontoservice.domain.enums.TipoAjuste;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** O mês completo: totais, dias e marcações incluídas/descartadas. */
@Getter
@Builder
public class EspelhoResponse {

    private EspelhoResumoResponse resumo;
    private List<DiaResponse> dias;
    private List<AjusteResponse> ajustes;

    @Getter
    @Builder
    public static class DiaResponse {
        private LocalDate data;
        private String marcacoes;
        private String ocorrencia;
        private Integer abono;
        private Integer trabalhadas;
        private Integer compensaveis;
        private Integer acumulo;
        private Integer naoCompensaveis;
    }

    @Getter
    @Builder
    public static class AjusteResponse {
        private TipoAjuste tipo;
        private LocalDateTime dataHora;
        private String motivo;
    }

}
