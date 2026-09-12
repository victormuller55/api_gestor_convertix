package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.ProjetoEtapaRequest;
import br.net.convertix.gestor.dto.request.ProjetoRequest;
import br.net.convertix.gestor.dto.response.HistoricoEtapaProjetoResponse;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.dto.response.ProjetoResponse;
import br.net.convertix.gestor.entity.AplicativoMobile;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.HistoricoEtapaProjeto;
import br.net.convertix.gestor.entity.Projeto;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.enums.EtapaProjeto;
import br.net.convertix.gestor.enums.OrigemAlteracaoStatus;
import br.net.convertix.gestor.enums.TipoProjeto;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.AplicativoMobileRepository;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.ProjetoRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.spec.ProjetoSpecification;
import br.net.convertix.gestor.security.SecurityUtil;
import br.net.convertix.gestor.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final ClienteRepository clienteRepository;
    private final SiteRepository siteRepository;
    private final AplicativoMobileRepository aplicativoMobileRepository;
    private final AutorizacaoService autorizacaoService;

    @Transactional(readOnly = true)
    public PageResponse<ProjetoResponse> buscar(
            Long id,
            String query,
            Long clienteId,
            EtapaProjeto etapa,
            TipoProjeto tipo,
            int page,
            int size) {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        if (clienteIdFiltro == null) {
            clienteIdFiltro = clienteId;
        } else {
            autorizacaoService.forcarClienteId(clienteId);
        }

        Page<Projeto> resultado = projetoRepository.findAll(
                ProjetoSpecification.comFiltros(id, query, clienteIdFiltro, etapa, tipo),
                PaginationUtil.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        boolean incluirHistorico = id != null;
        if (incluirHistorico) {
            resultado.getContent().forEach(projeto -> Hibernate.initialize(projeto.getHistoricoEtapa()));
        }

        return PaginationUtil.toResponse(resultado, projeto -> toResponse(projeto, incluirHistorico));
    }

    @Transactional
    public ProjetoResponse criar(ProjetoRequest request) {
        SecurityUtil.exigirAdmin();
        Long clienteId = autorizacaoService.resolverClienteId(request.getClienteId());
        Cliente cliente = buscarCliente(clienteId);
        Site site = buscarSite(request.getSiteId());
        AplicativoMobile aplicativo = buscarAplicativo(request.getAplicativoMobileId());
        validarVinculo(clienteId, site, aplicativo);

        Projeto projeto = Projeto.builder()
                .cliente(cliente)
                .titulo(request.getTitulo().trim())
                .tipo(request.getTipo())
                .etapa(request.getEtapa())
                .site(site)
                .aplicativoMobile(aplicativo)
                .prazo(request.getPrazo())
                .descricao(blankToNull(request.getDescricao()))
                .observacaoInterna(blankToNull(request.getObservacaoInterna()))
                .build();
        registrarHistorico(projeto, null, request.getEtapa(), OrigemAlteracaoStatus.CRIACAO, "Projeto criado");

        return toResponse(projetoRepository.save(projeto), true);
    }

    @Transactional
    public ProjetoResponse atualizar(Long id, ProjetoRequest request) {
        SecurityUtil.exigirAdmin();
        Projeto projeto = buscarEntidade(id);
        Long clienteId = autorizacaoService.resolverClienteId(request.getClienteId());
        Cliente cliente = buscarCliente(clienteId);
        Site site = buscarSite(request.getSiteId());
        AplicativoMobile aplicativo = buscarAplicativo(request.getAplicativoMobileId());
        validarVinculo(clienteId, site, aplicativo);

        EtapaProjeto etapaAnterior = projeto.getEtapa();
        projeto.setCliente(cliente);
        projeto.setTitulo(request.getTitulo().trim());
        projeto.setTipo(request.getTipo());
        projeto.setEtapa(request.getEtapa());
        projeto.setSite(site);
        projeto.setAplicativoMobile(aplicativo);
        projeto.setPrazo(request.getPrazo());
        projeto.setDescricao(blankToNull(request.getDescricao()));
        projeto.setObservacaoInterna(blankToNull(request.getObservacaoInterna()));

        if (etapaAnterior != request.getEtapa()) {
            registrarHistorico(
                    projeto,
                    etapaAnterior,
                    request.getEtapa(),
                    OrigemAlteracaoStatus.ATUALIZACAO,
                    "Etapa alterada");
        }

        return toResponse(projetoRepository.save(projeto), true);
    }

    @Transactional
    public ProjetoResponse alterarEtapa(Long id, ProjetoEtapaRequest request) {
        SecurityUtil.exigirAdmin();
        Projeto projeto = buscarEntidade(id);
        EtapaProjeto etapaAnterior = projeto.getEtapa();
        if (etapaAnterior == request.getEtapa()) {
            return toResponse(projeto, true);
        }

        projeto.setEtapa(request.getEtapa());
        registrarHistorico(
                projeto,
                etapaAnterior,
                request.getEtapa(),
                OrigemAlteracaoStatus.ATUALIZACAO,
                "Etapa alterada no quadro");
        return toResponse(projetoRepository.save(projeto), true);
    }

    @Transactional
    public void excluir(Long id) {
        SecurityUtil.exigirAdmin();
        Projeto projeto = buscarEntidade(id);
        projetoRepository.delete(projeto);
    }

    @Transactional
    public void excluirPorCliente(Long clienteId) {
        projetoRepository.findByClienteId(clienteId).forEach(projetoRepository::delete);
    }

    @Transactional
    public void desvincularSite(Long siteId) {
        projetoRepository.findBySiteId(siteId).forEach(projeto -> projeto.setSite(null));
    }

    @Transactional
    public void desvincularAplicativo(Long aplicativoMobileId) {
        projetoRepository.findByAplicativoMobileId(aplicativoMobileId)
                .forEach(projeto -> projeto.setAplicativoMobile(null));
    }

    private Projeto buscarEntidade(Long id) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com id: " + id));
        autorizacaoService.validarAcessoCliente(projeto.getCliente().getId());
        Hibernate.initialize(projeto.getHistoricoEtapa());
        return projeto;
    }

    private Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));
    }

    private Site buscarSite(Long siteId) {
        if (siteId == null) {
            return null;
        }
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + siteId));
    }

    private AplicativoMobile buscarAplicativo(Long aplicativoId) {
        if (aplicativoId == null) {
            return null;
        }
        return aplicativoMobileRepository.findById(aplicativoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aplicativo mobile não encontrado com id: " + aplicativoId));
    }

    private void validarVinculo(Long clienteId, Site site, AplicativoMobile aplicativo) {
        if (site != null && aplicativo != null) {
            throw new BusinessException("Informe somente o site ou o aplicativo mobile");
        }
        if (site != null && site.getCliente() != null && !site.getCliente().getId().equals(clienteId)) {
            throw new BusinessException("O site não pertence ao cliente informado");
        }
        if (aplicativo != null && aplicativo.getCliente() != null
                && !aplicativo.getCliente().getId().equals(clienteId)) {
            throw new BusinessException("O aplicativo não pertence ao cliente informado");
        }
    }

    private void registrarHistorico(
            Projeto projeto,
            EtapaProjeto anterior,
            EtapaProjeto nova,
            OrigemAlteracaoStatus origem,
            String mensagem) {
        HistoricoEtapaProjeto historico = HistoricoEtapaProjeto.builder()
                .projeto(projeto)
                .etapaAnterior(anterior)
                .etapaNova(nova)
                .origem(origem)
                .mensagem(mensagem)
                .build();
        projeto.getHistoricoEtapa().add(historico);
    }

    private ProjetoResponse toResponse(Projeto entity, boolean incluirHistorico) {
        boolean admin = SecurityUtil.getUsuarioLogado().isAdmin();
        List<HistoricoEtapaProjetoResponse> historico = List.of();
        if (incluirHistorico && entity.getHistoricoEtapa() != null) {
            historico = entity.getHistoricoEtapa().stream().map(this::toHistoricoResponse).toList();
        }

        return ProjetoResponse.builder()
                .id(entity.getId())
                .clienteId(entity.getCliente().getId())
                .clienteNomeEmpresa(entity.getCliente().getNomeEmpresa())
                .titulo(entity.getTitulo())
                .tipo(entity.getTipo())
                .etapa(entity.getEtapa())
                .siteId(entity.getSite() != null ? entity.getSite().getId() : null)
                .siteNome(entity.getSite() != null ? entity.getSite().getNome() : null)
                .aplicativoMobileId(entity.getAplicativoMobile() != null ? entity.getAplicativoMobile().getId() : null)
                .aplicativoMobileNome(
                        entity.getAplicativoMobile() != null ? entity.getAplicativoMobile().getNome() : null)
                .prazo(entity.getPrazo())
                .descricao(entity.getDescricao())
                .observacaoInterna(admin ? entity.getObservacaoInterna() : null)
                .historicoEtapa(historico)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private HistoricoEtapaProjetoResponse toHistoricoResponse(HistoricoEtapaProjeto historico) {
        return HistoricoEtapaProjetoResponse.builder()
                .id(historico.getId())
                .etapaAnterior(historico.getEtapaAnterior())
                .etapaNova(historico.getEtapaNova())
                .origem(historico.getOrigem())
                .mensagem(historico.getMensagem())
                .createdAt(historico.getCreatedAt())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
