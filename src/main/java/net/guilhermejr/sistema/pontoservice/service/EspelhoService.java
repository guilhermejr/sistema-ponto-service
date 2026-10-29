package net.guilhermejr.sistema.pontoservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.guilhermejr.seguranca.jwt.AuthenticationCurrentUserService;
import net.guilhermejr.sistema.pontoservice.api.response.EspelhoResponse;
import net.guilhermejr.sistema.pontoservice.api.response.EspelhoResumoResponse;
import net.guilhermejr.sistema.pontoservice.api.response.ImportacaoResponse;
import net.guilhermejr.sistema.pontoservice.domain.entity.Ajuste;
import net.guilhermejr.sistema.pontoservice.domain.entity.Dia;
import net.guilhermejr.sistema.pontoservice.domain.entity.Espelho;
import net.guilhermejr.sistema.pontoservice.domain.repository.EspelhoRepository;
import net.guilhermejr.sistema.pontoservice.exception.ExceptionDefault;
import net.guilhermejr.sistema.pontoservice.exception.ExceptionNotFound;
import net.guilhermejr.sistema.pontoservice.leitor.EspelhoLido;
import net.guilhermejr.sistema.pontoservice.leitor.LeitorEspelho;
import net.guilhermejr.sistema.pontoservice.leitor.LeituraEspelhoException;
import net.guilhermejr.sistema.pontoservice.util.Resumo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Service
public class EspelhoService {

    private final EspelhoRepository espelhoRepository;
    private final AuthenticationCurrentUserService authenticationCurrentUserService;

    /**
     * Lê o PDF e grava o mês. Um mês que já existia é substituído por inteiro (o RH
     * pode reemitir o espelho corrigido); o mês vem do início do período.
     */
    @Transactional
    public ImportacaoResponse importar(MultipartFile arquivo) {

        UUID usuario = usuario();
        EspelhoLido lido = ler(arquivo);
        int ano = lido.inicio().getYear();
        int mes = lido.inicio().getMonthValue();

        Optional<Espelho> existente = espelhoRepository.findByUsuarioAndAnoAndMes(usuario, ano, mes);
        existente.ifPresent(e -> {
            espelhoRepository.delete(e);
            espelhoRepository.flush();
        });

        Espelho espelho = novoEspelho(lido, ano, mes, arquivo.getOriginalFilename());
        espelho.setUsuario(usuario);
        espelhoRepository.save(espelho);

        log.info("Espelho {}/{} {} ({} dias, {} ajustes)", mes, ano,
                existente.isPresent() ? "substituído" : "importado", lido.dias().size(), lido.ajustes().size());
        return new ImportacaoResponse(resumo(espelho), existente.isPresent());

    }

    @Transactional(readOnly = true)
    public List<EspelhoResumoResponse> listar() {
        return espelhoRepository.findAllByUsuarioOrderByAnoAscMesAsc(usuario()).stream()
                .map(EspelhoService::resumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public EspelhoResponse detalhar(Integer ano, Integer mes) {

        Espelho espelho = espelhoDoUsuario(ano, mes);
        return EspelhoResponse.builder()
                .resumo(resumo(espelho))
                .dias(espelho.getDias().stream().map(d -> EspelhoResponse.DiaResponse.builder()
                        .data(d.getData())
                        .marcacoes(d.getMarcacoes())
                        .ocorrencia(d.getOcorrencia())
                        .abono(d.getAbono())
                        .trabalhadas(d.getTrabalhadas())
                        .compensaveis(d.getCompensaveis())
                        .acumulo(d.getAcumulo())
                        .naoCompensaveis(d.getNaoCompensaveis())
                        .build()).toList())
                .ajustes(espelho.getAjustes().stream().map(a -> EspelhoResponse.AjusteResponse.builder()
                        .tipo(a.getTipo())
                        .dataHora(a.getDataHora())
                        .motivo(a.getMotivo())
                        .build()).toList())
                .build();

    }

    @Transactional
    public void apagar(Integer ano, Integer mes) {
        espelhoRepository.delete(espelhoDoUsuario(ano, mes));
        log.info("Espelho {}/{} apagado", mes, ano);
    }

    private EspelhoLido ler(MultipartFile arquivo) {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new ExceptionDefault("Envie o PDF do espelho de ponto.");
        }
        try {
            return LeitorEspelho.ler(arquivo.getBytes());
        } catch (LeituraEspelhoException e) {
            log.error("Espelho não reconhecido: {}", e.getMessage(), e);
            throw new ExceptionDefault(e.getMessage());
        } catch (IOException | RuntimeException e) {
            log.error("Erro ao ler o espelho", e);
            throw new ExceptionDefault("Não foi possível ler o espelho de ponto.");
        }

    }

    private static Espelho novoEspelho(EspelhoLido lido, int ano, int mes, String arquivo) {

        Espelho espelho = new Espelho();
        espelho.setAno(ano);
        espelho.setMes(mes);
        espelho.setInicio(lido.inicio());
        espelho.setFim(lido.fim());
        espelho.setHorarioContratual(lido.horarioContratual());
        espelho.setFolhaPagamento(lido.folhaPagamento());
        espelho.setCargo(lido.cargo());
        espelho.setLotacao(lido.lotacao());
        espelho.setEmitido(lido.emitido());
        espelho.setAssinado(lido.assinado());
        espelho.setArquivo(arquivo);

        for (EspelhoLido.Dia d : lido.dias()) {
            Dia dia = new Dia();
            dia.setEspelho(espelho);
            dia.setData(d.data());
            dia.setMarcacoes(d.marcacoes());
            dia.setOcorrencia(d.ocorrencia());
            dia.setAbono(d.abono());
            dia.setTrabalhadas(d.trabalhadas());
            dia.setCompensaveis(d.compensaveis());
            dia.setAcumulo(d.acumulo());
            dia.setNaoCompensaveis(d.naoCompensaveis());
            espelho.getDias().add(dia);
        }
        for (EspelhoLido.Ajuste a : lido.ajustes()) {
            Ajuste ajuste = new Ajuste();
            ajuste.setEspelho(espelho);
            ajuste.setTipo(a.tipo());
            ajuste.setDataHora(a.dataHora());
            ajuste.setMotivo(a.motivo());
            espelho.getAjustes().add(ajuste);
        }
        return espelho;

    }

    private static EspelhoResumoResponse resumo(Espelho espelho) {

        List<EspelhoLido.Dia> dias = espelho.getDias().stream()
                .map(d -> new EspelhoLido.Dia(d.getData(), d.getMarcacoes(), d.getOcorrencia(), d.getAbono(),
                        d.getTrabalhadas(), d.getCompensaveis(), d.getAcumulo(), d.getNaoCompensaveis()))
                .toList();
        Resumo r = Resumo.calcular(espelho.getHorarioContratual(), dias);

        return EspelhoResumoResponse.builder()
                .ano(espelho.getAno())
                .mes(espelho.getMes())
                .inicio(espelho.getInicio())
                .fim(espelho.getFim())
                .horarioContratual(espelho.getHorarioContratual())
                .folhaPagamento(espelho.getFolhaPagamento())
                .cargo(espelho.getCargo())
                .lotacao(espelho.getLotacao())
                .emitido(espelho.getEmitido())
                .assinado(espelho.getAssinado())
                .jornadaDiaria(r.jornadaDiaria())
                .diasUteis(r.diasUteis())
                .diasTrabalhados(r.diasTrabalhados())
                .previstas(r.previstas())
                .trabalhadas(r.trabalhadas())
                .abonos(r.abonos())
                .extras(r.extras())
                .faltas(r.faltas())
                .compensaveis(r.compensaveis())
                .naoCompensaveis(r.naoCompensaveis())
                .saldo(r.saldo())
                .ocorrencias(r.ocorrencias())
                .build();

    }

    /** Mês de outro usuário responde 404, como nos demais serviços. */
    private Espelho espelhoDoUsuario(Integer ano, Integer mes) {
        return espelhoRepository.findByUsuarioAndAnoAndMes(usuario(), ano, mes)
                .orElseThrow(() -> {
                    log.error("Espelho {}/{} não encontrado", mes, ano);
                    return new ExceptionNotFound("Espelho não encontrado.");
                });
    }

    private UUID usuario() {
        return authenticationCurrentUserService.getCurrentUser().getId();
    }

}
