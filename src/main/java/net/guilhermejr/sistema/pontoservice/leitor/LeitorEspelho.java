package net.guilhermejr.sistema.pontoservice.leitor;

import net.guilhermejr.sistema.pontoservice.domain.enums.TipoAjuste;
import net.guilhermejr.sistema.pontoservice.util.Horas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lê o espelho de ponto (PDF do sistema de ponto do Itaú) pela posição das palavras.
 *
 * <p>O texto corrido não basta: com uma coluna vazia, os valores da linha deslizam e
 * "07:57 01:42" pode ser compensável ou acúmulo. Cada palavra vai para a coluna onde o
 * seu centro cai, entre as bordas verticais da tabela — as mesmas em todos os espelhos
 * desde 12/2022. Qualquer coisa fora do esperado recusa o arquivo em vez de gravar um
 * valor na coluna errada.
 */
public final class LeitorEspelho {

    /** Bordas verticais da tabela de dias, em pontos a partir da esquerda (página A4 deitada). */
    private static final float[] BORDAS = {20f, 120.5f, 283.5f, 411f, 482f, 546f, 616.7f, 687.5f, 768f};

    private enum Coluna {
        DATA("Data"), MARCACAO("Marcação"), OCORRENCIA("Ocorrências"), ABONO("Abono"),
        TRABALHADAS("Horas Trabalhadas"), COMPENSAVEIS("Horas Compensáveis"),
        ACUMULO("Acúmulo Compensação"), NAO_COMPENSAVEIS("Horas Não Compensáveis");

        private final String titulo;

        Coluna(String titulo) {
            this.titulo = titulo;
        }
    }

    /** Palavras com y até esta distância estão na mesma linha. */
    private static final float MESMA_LINHA = 2f;
    /** Metade da altura de uma linha da tabela: o texto quebrado de uma célula fica dentro dela. */
    private static final float MEIA_LINHA = 6.5f;

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern DATA_PALAVRA = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    private static final Pattern DIA_SEMANA = Pattern.compile("\\(\\p{L}{3}\\)");
    private static final Pattern HORA = Pattern.compile("\\d{2}:\\d{2}");

    private static final Pattern PERIODO = Pattern.compile("Per[ií]odo:\\s*(\\d{2}/\\d{2}/\\d{4})\\s*at[ée]\\s*(\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern HORARIO = Pattern.compile("Hor[áa]rio\\s*Contratual:\\s*([\\d:-]+)");
    private static final Pattern FOLHA = Pattern.compile("Folha\\s*de\\s*Pagamento:\\s*(\\d{4}_\\d{2})");
    private static final Pattern CARGO = Pattern.compile("Cargo:[ \\t]*(.+?)[ \\t]*$", Pattern.MULTILINE);
    private static final Pattern LOTACAO = Pattern.compile("Lota[çc][ãa]o:[ \\t]*(.+?)[ \\t]*$", Pattern.MULTILINE);
    private static final Pattern EMISSAO = Pattern.compile("Data\\s*de\\s*emiss[ãa]o:\\s*(\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern ASSINATURA = Pattern.compile("Assinado\\s*eletronicamente\\s*em\\s*(\\d{2}/\\d{2}/\\d{4})\\s*[àa]s\\s*(\\d{2}:\\d{2})");

    private LeitorEspelho() {
    }

    public static EspelhoLido ler(byte[] pdf) {
        ExtratorPalavras.Conteudo conteudo = ExtratorPalavras.extrair(pdf);
        return ler(conteudo.texto(), conteudo.palavras());
    }

    public static EspelhoLido ler(String texto, List<Palavra> palavras) {

        if (!texto.contains("Espelho de Ponto")) {
            throw new LeituraEspelhoException("O arquivo não é um espelho de ponto.");
        }
        Matcher periodo = PERIODO.matcher(texto);
        Matcher horario = HORARIO.matcher(texto);
        if (!periodo.find() || !horario.find()) {
            throw new LeituraEspelhoException("Período ou horário contratual não encontrado no espelho.");
        }
        LocalDate inicio = data(periodo.group(1));
        LocalDate fim = data(periodo.group(2));
        String horarioContratual = horario.group(1);
        try {
            Horas.jornada(horarioContratual);
        } catch (IllegalArgumentException e) {
            throw new LeituraEspelhoException("Horário contratual não reconhecido: " + horarioContratual + ".");
        }

        List<List<Palavra>> linhas = agruparLinhas(palavras);
        List<DiaEmMontagem> dias = new ArrayList<>();
        List<List<Palavra>> outras = new ArrayList<>();
        for (List<Palavra> linha : linhas) {
            if (ehLinhaDeDia(linha)) {
                dias.add(new DiaEmMontagem(linha));
            } else {
                outras.add(linha);
            }
        }

        // Texto que quebrou dentro de uma célula fica numa linha própria, a menos de meia
        // linha do dia a que pertence. O resto (cabeçalho, rodapé, ajustes) fica de fora.
        List<List<Palavra>> restantes = new ArrayList<>();
        for (List<Palavra> linha : outras) {
            DiaEmMontagem dia = diaMaisProximo(dias, linha.getFirst());
            if (dia != null) {
                linha.forEach(dia::adicionar);
            } else {
                restantes.add(linha);
            }
        }

        List<EspelhoLido.Dia> lidos = dias.stream().map(DiaEmMontagem::montar).toList();
        conferirDias(lidos, inicio, fim);

        return new EspelhoLido(
                inicio,
                fim,
                horarioContratual,
                grupo(FOLHA, texto),
                grupo(CARGO, texto),
                grupo(LOTACAO, texto),
                dataOuNull(grupo(EMISSAO, texto)),
                assinatura(texto),
                lidos,
                ajustes(restantes));

    }

    private static List<List<Palavra>> agruparLinhas(List<Palavra> palavras) {

        List<Palavra> ordenadas = palavras.stream()
                .sorted(Comparator.comparingInt(Palavra::pagina).thenComparingDouble(Palavra::y).thenComparingDouble(Palavra::x))
                .toList();
        List<List<Palavra>> linhas = new ArrayList<>();
        List<Palavra> atual = null;
        for (Palavra p : ordenadas) {
            Palavra primeira = atual == null ? null : atual.getFirst();
            if (primeira == null || primeira.pagina() != p.pagina() || p.y() - primeira.y() > MESMA_LINHA) {
                atual = new ArrayList<>();
                linhas.add(atual);
            }
            atual.add(p);
        }
        linhas.forEach(l -> l.sort(Comparator.comparingDouble(Palavra::x)));
        return linhas;

    }

    /** Linha de dia: "02/09/2026 (qua) ..." na coluna Data. Os ajustes têm data seguida de hora. */
    private static boolean ehLinhaDeDia(List<Palavra> linha) {
        return linha.size() >= 2
                && linha.get(0).xFim() < BORDAS[1]
                && DATA_PALAVRA.matcher(linha.get(0).texto()).matches()
                && DIA_SEMANA.matcher(linha.get(1).texto()).matches();
    }

    private static DiaEmMontagem diaMaisProximo(List<DiaEmMontagem> dias, Palavra palavra) {
        return dias.stream()
                .filter(d -> d.pagina == palavra.pagina() && Math.abs(d.y - palavra.y()) < MEIA_LINHA)
                .min(Comparator.comparingDouble(d -> Math.abs(d.y - palavra.y())))
                .orElse(null);
    }

    private static void conferirDias(List<EspelhoLido.Dia> dias, LocalDate inicio, LocalDate fim) {

        List<LocalDate> esperadas = inicio.datesUntil(fim.plusDays(1)).toList();
        List<LocalDate> lidas = dias.stream().map(EspelhoLido.Dia::data).toList();
        if (!esperadas.equals(lidas)) {
            throw new LeituraEspelhoException("Os dias do espelho não batem com o período de "
                    + inicio.format(DATA) + " a " + fim.format(DATA) + ".");
        }

    }

    /**
     * Marcações Incluídas e Descartadas: cada seção tem um título e fica numa faixa
     * horizontal própria (podem estar lado a lado). Numa página sem título, a seção
     * continua da página anterior.
     */
    private static List<EspelhoLido.Ajuste> ajustes(List<List<Palavra>> linhas) {

        List<EspelhoLido.Ajuste> ajustes = new ArrayList<>();
        List<Secao> secoes = List.of();
        int pagina = -1;
        for (List<Palavra> linha : linhas) {
            List<Secao> titulos = titulos(linha);
            if (!titulos.isEmpty()) {
                secoes = linha.getFirst().pagina() == pagina ? juntar(secoes, titulos) : titulos;
                pagina = linha.getFirst().pagina();
                continue;
            }
            for (int i = 0; i < secoes.size(); i++) {
                float de = secoes.get(i).x - 3;
                float ate = i + 1 < secoes.size() ? secoes.get(i + 1).x - 3 : Float.MAX_VALUE;
                List<Palavra> trecho = linha.stream().filter(p -> p.x() >= de && p.x() < ate).toList();
                if (trecho.size() >= 2
                        && DATA_PALAVRA.matcher(trecho.get(0).texto()).matches()
                        && HORA.matcher(trecho.get(1).texto()).matches()) {
                    LocalDateTime dataHora = LocalDateTime.of(data(trecho.get(0).texto()), hora(trecho.get(1).texto()));
                    String motivo = trecho.stream().skip(2).map(Palavra::texto).collect(Collectors.joining(" "));
                    ajustes.add(new EspelhoLido.Ajuste(secoes.get(i).tipo, dataHora, motivo.isEmpty() ? null : motivo));
                }
            }
        }
        // Lado a lado, a leitura por linha intercala as seções; a ordenação é estável,
        // então cada seção mantém a ordem em que aparece no espelho.
        ajustes.sort(Comparator.comparing(EspelhoLido.Ajuste::tipo));
        return ajustes;

    }

    private record Secao(float x, TipoAjuste tipo) {
    }

    private static List<Secao> titulos(List<Palavra> linha) {

        List<Secao> secoes = new ArrayList<>();
        for (int i = 0; i + 1 < linha.size(); i++) {
            if (!linha.get(i).texto().startsWith("Marca")) {
                continue;
            }
            String seguinte = linha.get(i + 1).texto();
            if (seguinte.startsWith("Inclu")) {
                secoes.add(new Secao(linha.get(i).x(), TipoAjuste.I));
            } else if (seguinte.startsWith("Descart")) {
                secoes.add(new Secao(linha.get(i).x(), TipoAjuste.D));
            }
        }
        return secoes;

    }

    private static List<Secao> juntar(List<Secao> atuais, List<Secao> novas) {
        List<Secao> todas = new ArrayList<>(atuais);
        todas.addAll(novas);
        todas.sort(Comparator.comparingDouble(Secao::x));
        return todas;
    }

    private static class DiaEmMontagem {

        private final int pagina;
        private final float y;
        private final LocalDate data;
        private final Map<Coluna, List<Palavra>> celulas = new EnumMap<>(Coluna.class);

        DiaEmMontagem(List<Palavra> linha) {
            this.pagina = linha.getFirst().pagina();
            this.y = linha.getFirst().y();
            this.data = data(linha.getFirst().texto());
            linha.stream().skip(2).forEach(this::adicionar);
        }

        void adicionar(Palavra palavra) {
            celulas.computeIfAbsent(coluna(palavra), c -> new ArrayList<>()).add(palavra);
        }

        private Coluna coluna(Palavra palavra) {
            float centro = palavra.centro();
            for (int i = 0; i + 1 < BORDAS.length; i++) {
                if (centro >= BORDAS[i] && centro < BORDAS[i + 1]) {
                    return Coluna.values()[i];
                }
            }
            throw new LeituraEspelhoException("Valor fora da tabela em " + data.format(DATA) + ": " + palavra.texto() + ".");
        }

        private String texto(Coluna coluna) {
            List<Palavra> palavras = celulas.getOrDefault(coluna, List.of());
            String texto = palavras.stream()
                    .sorted(Comparator.comparingDouble(Palavra::y).thenComparingDouble(Palavra::x))
                    .map(Palavra::texto)
                    .collect(Collectors.joining(" "));
            return texto.isEmpty() ? null : texto;
        }

        private Integer horas(Coluna coluna) {
            String texto = texto(coluna);
            if (texto != null && !Horas.valida(texto)) {
                throw new LeituraEspelhoException("Valor inesperado em " + coluna.titulo + " no dia "
                        + data.format(DATA) + ": " + texto + ".");
            }
            return Horas.minutos(texto);
        }

        EspelhoLido.Dia montar() {
            if (texto(Coluna.DATA) != null) {
                throw new LeituraEspelhoException("Valor inesperado na coluna Data em " + data.format(DATA) + ".");
            }
            return new EspelhoLido.Dia(
                    data,
                    texto(Coluna.MARCACAO),
                    texto(Coluna.OCORRENCIA),
                    horas(Coluna.ABONO),
                    horas(Coluna.TRABALHADAS),
                    horas(Coluna.COMPENSAVEIS),
                    horas(Coluna.ACUMULO),
                    horas(Coluna.NAO_COMPENSAVEIS));
        }

    }

    private static String grupo(Pattern padrao, String texto) {
        Matcher m = padrao.matcher(texto);
        return m.find() ? m.group(1) : null;
    }

    private static LocalDateTime assinatura(String texto) {
        Matcher m = ASSINATURA.matcher(texto);
        return m.find() ? LocalDateTime.of(data(m.group(1)), hora(m.group(2))) : null;
    }

    private static LocalDate data(String texto) {
        try {
            return LocalDate.parse(texto, DATA);
        } catch (DateTimeParseException e) {
            throw new LeituraEspelhoException("Data inválida no espelho: " + texto + ".");
        }
    }

    private static LocalDate dataOuNull(String texto) {
        return texto == null ? null : data(texto);
    }

    private static LocalTime hora(String texto) {
        try {
            return LocalTime.parse(texto);
        } catch (DateTimeParseException e) {
            throw new LeituraEspelhoException("Hora inválida no espelho: " + texto + ".");
        }
    }

}
