package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.LandingPageRequest;
import br.net.convertix.gestor.dto.response.LandingPageResponse;
import br.net.convertix.gestor.entity.LandingPage;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.enums.TipoSite;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.LandingPageFormularioRepository;
import br.net.convertix.gestor.repository.LandingPageLeadRepository;
import br.net.convertix.gestor.repository.LandingPageRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.spec.LandingPageSpecification;
import br.net.convertix.gestor.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LandingPageService {

    private final LandingPageRepository landingPageRepository;
    private final LandingPageFormularioRepository landingPageFormularioRepository;
    private final LandingPageLeadRepository landingPageLeadRepository;
    private final SiteRepository siteRepository;
    private final AutorizacaoService autorizacaoService;

    @Transactional(readOnly = true)
    public List<LandingPageResponse> buscar(Long id) {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        return landingPageRepository.findAll(LandingPageSpecification.comFiltros(id, clienteIdFiltro)).stream()
                .map(MapperUtil::toResponse)
                .toList();
    }

    @Transactional
    public LandingPageResponse criar(LandingPageRequest request) {
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + request.getSiteId()));

        autorizacaoService.validarAcessoSite(site.getId());

        if (site.getTipo() != TipoSite.LANDING_PAGE) {
            throw new BusinessException("O site informado não é do tipo LANDING_PAGE");
        }

        if (landingPageRepository.existsBySiteId(request.getSiteId())) {
            throw new BusinessException("Já existe uma Landing Page vinculada ao site: " + request.getSiteId());
        }

        validarSlugUnico(request.getSlug(), null);

        LandingPage landingPage = LandingPage.builder()
                .site(site)
                .slug(request.getSlug().trim())
                .build();

        return MapperUtil.toResponse(landingPageRepository.save(landingPage));
    }

    @Transactional
    public LandingPageResponse atualizar(Long id, LandingPageRequest request) {
        LandingPage landingPage = landingPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landing Page não encontrada com id: " + id));

        autorizacaoService.validarAcessoLandingPage(id);

        if (!landingPage.getSite().getId().equals(request.getSiteId())) {
            Site site = siteRepository.findById(request.getSiteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + request.getSiteId()));

            autorizacaoService.validarAcessoSite(site.getId());

            if (site.getTipo() != TipoSite.LANDING_PAGE) {
                throw new BusinessException("O site informado não é do tipo LANDING_PAGE");
            }

            if (landingPageRepository.existsBySiteIdAndIdNot(request.getSiteId(), id)) {
                throw new BusinessException("Já existe uma Landing Page vinculada ao site: " + request.getSiteId());
            }

            landingPage.setSite(site);
        }

        validarSlugUnico(request.getSlug(), id);
        landingPage.setSlug(request.getSlug().trim());

        return MapperUtil.toResponse(landingPageRepository.save(landingPage));
    }

    @Transactional
    public void excluir(Long id) {
        LandingPage landingPage = landingPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landing Page não encontrada com id: " + id));

        autorizacaoService.validarAcessoLandingPage(id);

        excluirDependencias(landingPage);

        Site site = landingPage.getSite();
        if (site != null) {
            site.setLandingPage(null);
            landingPage.setSite(null);
        }

        landingPageRepository.delete(landingPage);
    }

    void excluirDependencias(LandingPage landingPage) {
        Long landingPageId = landingPage.getId();
        landingPageLeadRepository.findByLandingPageIdOrderByCreatedAtDesc(landingPageId)
                .forEach(landingPageLeadRepository::delete);
        landingPageFormularioRepository.findByLandingPageIdOrderByNomeAsc(landingPageId)
                .forEach(landingPageFormularioRepository::delete);
    }

    private void validarSlugUnico(String slug, Long idExcluir) {
        if (slug == null || slug.isBlank()) {
            return;
        }

        String slugNormalizado = slug.trim();
        boolean existe = idExcluir == null
                ? landingPageRepository.findBySlug(slugNormalizado).isPresent()
                : landingPageRepository.findBySlug(slugNormalizado)
                        .map(lp -> !lp.getId().equals(idExcluir))
                        .orElse(false);

        if (existe) {
            throw new BusinessException("Já existe uma Landing Page com o slug: " + slugNormalizado);
        }
    }
}
