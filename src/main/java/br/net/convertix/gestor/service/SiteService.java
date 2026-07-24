package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.SiteDominioRequest;
import br.net.convertix.gestor.dto.request.SiteRequest;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.dto.response.SiteResponse;
import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.entity.SiteDominio;
import br.net.convertix.gestor.enums.SituacaoAssinaturaSite;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.StatusPagamento;
import br.net.convertix.gestor.enums.TipoSite;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.AssinaturaRepository;
import br.net.convertix.gestor.repository.BioLinkRepository;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.LandingPageRepository;
import br.net.convertix.gestor.repository.PagamentoRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.spec.SiteSpecification;
import br.net.convertix.gestor.util.MapperUtil;
import br.net.convertix.gestor.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;
    private final ClienteRepository clienteRepository;
    private final BioLinkRepository bioLinkRepository;
    private final LandingPageRepository landingPageRepository;
    private final BioLinkService bioLinkService;
    private final LandingPageService landingPageService;
    private final AutorizacaoService autorizacaoService;
    private final AssinaturaRepository assinaturaRepository;
    private final PagamentoRepository pagamentoRepository;

    @Transactional(readOnly = true)
    public PageResponse<SiteResponse> buscar(Long id, String query, int page, int size) {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        Page<Site> resultado = siteRepository.findAll(
                SiteSpecification.comFiltros(id, query, clienteIdFiltro),
                PaginationUtil.of(page, size));
        Map<Long, Assinatura> assinaturaPorSite = carregarAssinaturasPorSite(resultado.getContent());

        List<SiteResponse> content = resultado.getContent().stream()
                .map(site -> {
                    SiteResponse response = MapperUtil.toResponse(site);
                    response.setSituacaoAssinatura(
                            resolverSituacaoAssinatura(assinaturaPorSite.get(site.getId())));
                    return response;
                })
                .toList();

        return PaginationUtil.toResponse(resultado, content);
    }

    @Transactional
    public SiteResponse criar(SiteRequest request) {
        Long clienteId = autorizacaoService.resolverClienteId(request.getClienteId());

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));

        validarSubdominioUnico(request.getSubdominio(), null);
        validarDominioInfo(request);
        validarTipoSitePermitido(request.getTipo());

        Site site = Site.builder()
                .cliente(cliente)
                .nome(request.getNome())
                .tipo(request.getTipo())
                .dominio(request.getDominio())
                .subdominio(request.getSubdominio())
                .status(request.getStatus())
                .build();

        sincronizarDominioInfo(site, request.getDominioInfo());

        SiteResponse response = MapperUtil.toResponse(siteRepository.save(site));
        response.setSituacaoAssinatura(SituacaoAssinaturaSite.DESATIVADO);
        return response;
    }

    @Transactional
    public SiteResponse atualizar(Long id, SiteRequest request) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + id));

        autorizacaoService.validarAcessoSite(id);

        Long clienteId = autorizacaoService.resolverClienteId(request.getClienteId());

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));

        validarSubdominioUnico(request.getSubdominio(), id);
        validarDominioInfo(request);
        validarTipoSitePermitido(request.getTipo());

        site.setCliente(cliente);
        MapperUtil.updateEntity(site, request);
        sincronizarDominioInfo(site, request.getDominioInfo());

        Site salvo = siteRepository.save(site);
        SiteResponse response = MapperUtil.toResponse(salvo);
        Assinatura assinatura = carregarAssinaturasPorSite(List.of(salvo)).get(salvo.getId());
        response.setSituacaoAssinatura(resolverSituacaoAssinatura(assinatura));
        return response;
    }

    @Transactional
    public void excluir(Long id) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + id));

        autorizacaoService.validarAcessoSite(id);

        bioLinkRepository.findBySiteId(id).ifPresent(bioLink -> bioLinkService.excluir(bioLink.getId()));
        landingPageRepository.findBySiteId(id).ifPresent(landingPage -> landingPageService.excluir(landingPage.getId()));

        site.setBioLink(null);
        site.setLandingPage(null);
        siteRepository.delete(site);
    }

    private void sincronizarDominioInfo(Site site, SiteDominioRequest dominioInfo) {
        if (dominioInfo == null) {
            site.setSiteDominio(null);
            return;
        }

        SiteDominio siteDominio = site.getSiteDominio();
        if (siteDominio == null) {
            site.setSiteDominio(MapperUtil.toDominioEntity(dominioInfo, site));
            return;
        }

        MapperUtil.updateDominioEntity(siteDominio, dominioInfo);
    }

    private void validarDominioInfo(SiteRequest request) {
        SiteDominioRequest dominioInfo = request.getDominioInfo();
        if (dominioInfo == null) {
            return;
        }

        if (request.getDominio() == null || request.getDominio().isBlank()) {
            throw new BusinessException(
                    "Informações de domínio só podem ser cadastradas quando o campo dominio estiver preenchido");
        }

        if (dominioInfo.getDataFimDominio().isBefore(dominioInfo.getDataCompraDominio())) {
            throw new BusinessException("A data de fim do domínio não pode ser anterior à data de compra");
        }

        if (dominioInfo.getDataRenovacao() != null
                && dominioInfo.getDataRenovacao().isBefore(dominioInfo.getDataCompraDominio())) {
            throw new BusinessException("A data de renovação não pode ser anterior à data de compra");
        }
    }

    private void validarTipoSitePermitido(TipoSite tipo) {
        if (tipo != TipoSite.BIOLINK) {
            throw new BusinessException("Tipo de site não disponível no momento: " + tipo);
        }
    }

    private void validarSubdominioUnico(String subdominio, Long idExcluir) {
        if (subdominio == null || subdominio.isBlank()) {
            return;
        }

        boolean existe = idExcluir == null
                ? siteRepository.existsBySubdominio(subdominio)
                : siteRepository.existsBySubdominioAndIdNot(subdominio, idExcluir);

        if (existe) {
            throw new BusinessException("Já existe um site com o subdomínio: " + subdominio);
        }
    }

    private Map<Long, Assinatura> carregarAssinaturasPorSite(List<Site> sites) {
        Set<Long> siteIds = sites.stream()
                .map(Site::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (siteIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Assinatura> porSite = new HashMap<>();
        for (Assinatura assinatura : assinaturaRepository.findBySiteIdIn(siteIds)) {
            if (assinatura.getSite() == null || assinatura.getSite().getId() == null) {
                continue;
            }
            Long siteId = assinatura.getSite().getId();
            Assinatura atual = porSite.get(siteId);
            if (atual == null || preferirAssinatura(assinatura, atual)) {
                porSite.put(siteId, assinatura);
            }
        }
        return porSite;
    }

    private boolean preferirAssinatura(Assinatura candidata, Assinatura atual) {
        int rankCandidata = rankStatusAssinatura(candidata.getStatus());
        int rankAtual = rankStatusAssinatura(atual.getStatus());
        if (rankCandidata != rankAtual) {
            return rankCandidata < rankAtual;
        }
        return Comparator.comparing(Assinatura::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .compare(candidata, atual) < 0;
    }

    private int rankStatusAssinatura(StatusAssinatura status) {
        if (status == StatusAssinatura.ACTIVE) {
            return 0;
        }
        if (status == StatusAssinatura.EXPIRED) {
            return 1;
        }
        return 2;
    }

    private SituacaoAssinaturaSite resolverSituacaoAssinatura(Assinatura assinatura) {
        if (assinatura == null || assinatura.getStatus() == StatusAssinatura.INACTIVE) {
            return SituacaoAssinaturaSite.DESATIVADO;
        }
        if (assinatura.getStatus() == StatusAssinatura.EXPIRED) {
            return SituacaoAssinaturaSite.VENCIDO;
        }
        boolean inadimplente = pagamentoRepository.existsByAssinaturaIdAndStatusIn(
                assinatura.getId(),
                List.of(StatusPagamento.OVERDUE));
        return inadimplente ? SituacaoAssinaturaSite.VENCIDO : SituacaoAssinaturaSite.EM_DIA;
    }
}
