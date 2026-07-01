package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.SiteDominioRequest;
import br.net.convertix.gestor.dto.request.SiteRequest;
import br.net.convertix.gestor.dto.response.SiteResponse;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.entity.SiteDominio;
import br.net.convertix.gestor.enums.TipoSite;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.BioLinkRepository;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.LandingPageRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.spec.SiteSpecification;
import br.net.convertix.gestor.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<SiteResponse> buscar(Long id, String query) {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        return siteRepository.findAll(SiteSpecification.comFiltros(id, query, clienteIdFiltro)).stream()
                .map(MapperUtil::toResponse)
                .toList();
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

        return MapperUtil.toResponse(siteRepository.save(site));
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

        return MapperUtil.toResponse(siteRepository.save(site));
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
}
