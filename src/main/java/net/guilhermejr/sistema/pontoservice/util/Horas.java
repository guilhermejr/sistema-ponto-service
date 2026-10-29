package net.guilhermejr.sistema.pontoservice.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Horas do espelho ("08:32", "-03:42", "125:10") como minutos com sinal. */
public final class Horas {

    private static final Pattern HORAS = Pattern.compile("(-?)(\\d{2,}):(\\d{2})");
    private static final Pattern INTERVALO = Pattern.compile("(\\d{2}):(\\d{2})-(\\d{2}):(\\d{2})");

    private Horas() {
    }

    public static boolean valida(String texto) {
        return HORAS.matcher(texto).matches();
    }

    /** Vazio vira null: no espelho, célula vazia é diferente de 00:00. */
    public static Integer minutos(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        Matcher m = HORAS.matcher(texto.strip());
        if (!m.matches()) {
            throw new IllegalArgumentException("Horas inválidas: " + texto);
        }
        int minutos = Integer.parseInt(m.group(2)) * 60 + Integer.parseInt(m.group(3));
        return m.group(1).isEmpty() ? minutos : -minutos;
    }

    /**
     * Jornada diária a partir do horário contratual ("09:00-12:00-13:00-18:00" = 480):
     * os horários vêm em pares de entrada e saída.
     */
    public static int jornada(String horarioContratual) {
        String[] partes = horarioContratual.split("-");
        if (partes.length < 2 || partes.length % 2 != 0) {
            throw new IllegalArgumentException("Horário contratual inválido: " + horarioContratual);
        }
        int total = 0;
        for (int i = 0; i + 1 < partes.length; i += 2) {
            Matcher entrada = INTERVALO.matcher(partes[i] + "-" + partes[i + 1]);
            if (!entrada.matches()) {
                throw new IllegalArgumentException("Horário contratual inválido: " + horarioContratual);
            }
            int inicio = Integer.parseInt(entrada.group(1)) * 60 + Integer.parseInt(entrada.group(2));
            int fim = Integer.parseInt(entrada.group(3)) * 60 + Integer.parseInt(entrada.group(4));
            total += fim - inicio;
        }
        return total;
    }

}
