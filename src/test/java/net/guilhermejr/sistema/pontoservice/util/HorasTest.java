package net.guilhermejr.sistema.pontoservice.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HorasTest {

    @Test
    void minutosComSinalEVazioComoNull() {
        assertThat(Horas.minutos("08:32")).isEqualTo(512);
        assertThat(Horas.minutos("-03:42")).isEqualTo(-222);
        assertThat(Horas.minutos("-00:16")).isEqualTo(-16);
        assertThat(Horas.minutos("125:10")).isEqualTo(7510);
        assertThat(Horas.minutos("")).isNull();
        assertThat(Horas.minutos(null)).isNull();
        assertThatThrownBy(() -> Horas.minutos("8h")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jornadaDoHorarioContratual() {
        assertThat(Horas.jornada("09:00-12:00-13:00-18:00")).isEqualTo(480);
        assertThat(Horas.jornada("08:00-12:00")).isEqualTo(240);
        assertThatThrownBy(() -> Horas.jornada("comercial")).isInstanceOf(IllegalArgumentException.class);
    }

}
