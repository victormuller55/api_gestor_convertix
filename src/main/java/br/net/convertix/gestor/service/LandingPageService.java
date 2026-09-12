package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.LandingPageRequest;
import br.net.convertix.gestor.dto.response.LandingPageCampoResponse;
import br.net.convertix.gestor.dto.response.LandingPageFormularioPublicoResponse;
import br.net.convertix.gestor.dto.response.LandingPagePublicoResponse;
import br.net.convertix.gestor.dto.response.LandingPageResponse;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.entity.LandingPage;
import br.net.convertix.gestor.entity.LandingPageCampo;
import br.net.convertix.gestor.entity.LandingPageFormulario;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.enums.TipoLandingPageCampo;
import br.net.convertix.gestor.enums.TipoSite;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.LandingPageCampoRepository;
import br.net.convertix.gestor.repository.LandingPageFormularioRepository;
import br.net.convertix.gestor.repository.LandingPageLeadRepository;
import br.net.convertix.gestor.repository.LandingPageRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.spec.LandingPageSpecification;
import br.net.convertix.gestor.util.MapperUtil;
import br.net.convertix.gestor.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LandingPageService {

    private final LandingPageRepository landingPageRepository;
    private final LandingPageFormularioRepository landingPageFormularioRepository;
    private final LandingPageCampoRepository landingPageCampoRepository;
    private final LandingPageLeadRepository landingPageLeadRepository;
    private final SiteRepository siteRepository;
    private final AutorizacaoService autorizacaoService;

    @Transactional(readOnly = true)
    public PageResponse<LandingPageResponse> buscar(Long id, String query, int page, int size) {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        Page<LandingPage> resultado = landingPageRepository.findAll(
                LandingPageSpecification.comFiltros(id, query, clienteIdFiltro),
                PaginationUtil.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PaginationUtil.toResponse(resultado, this::toResponse);
    }

    @Transactional(readOnly = true)
    public LandingPagePublicoResponse buscarPublicoPorSlug(String slug) {
        LandingPage landingPage = buscarPublica(slug);
        List<LandingPageFormularioPublicoResponse> formularios = landingPageFormularioRepository
                .findByLandingPageIdOrderByNomeAsc(landingPage.getId()).stream()
                .filter(formulario -> Boolean.TRUE.equals(formulario.getAtivo()))
                .map(formulario -> {
                    List<LandingPageCampoResponse> campos = landingPageCampoRepository
                            .findByFormularioIdAndAtivoTrueOrderByOrdemAsc(formulario.getId()).stream()
                            .map(MapperUtil::toResponse)
                            .toList();
                    return LandingPageFormularioPublicoResponse.builder()
                            .id(formulario.getId())
                            .nome(formulario.getNome())
                            .titulo(formulario.getTitulo())
                            .descricao(formulario.getDescricao())
                            .textoBotao(formulario.getTextoBotao())
                            .campos(campos)
                            .build();
                })
                .toList();

        return LandingPagePublicoResponse.builder()
                .slug(landingPage.getSlug())
                .siteNome(landingPage.getSite().getNome())
                .formularios(formularios)
                .build();
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

        String slug = normalizarSlug(request.getSlug());
        validarSlugUnico(slug, null);

        LandingPage landingPage = landingPageRepository.save(LandingPage.builder()
                .site(site)
                .slug(slug)
                .build());

        criarFormularioPadrao(landingPage);
        return toResponse(landingPage);
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

        String slug = normalizarSlug(request.getSlug());
        validarSlugUnico(slug, id);
        landingPage.setSlug(slug);

        return toResponse(landingPageRepository.save(landingPage));
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

    LandingPage buscarPublica(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new BusinessException("O slug da Landing Page é obrigatório");
        }

        LandingPage landingPage = landingPageRepository.findBySlug(normalizarSlug(slug))
                .orElseThrow(() -> new ResourceNotFoundException("Landing Page não encontrada"));

        if (landingPage.getSite().getTipo() != TipoSite.LANDING_PAGE
                || landingPage.getSite().getStatus() != StatusSite.ATIVO) {
            throw new ResourceNotFoundException("Landing Page não encontrada");
        }

        return landingPage;
    }

    private void criarFormularioPadrao(LandingPage landingPage) {
        LandingPageFormulario formulario = landingPageFormularioRepository.save(LandingPageFormulario.builder()
                .landingPage(landingPage)
                .nome("Contato")
                .titulo("Fale conosco")
                .descricao("Deixe seus dados que retornamos.")
                .textoBotao("Enviar")
                .ativo(true)
                .build());

        criarCampo(formulario, "nome", "Nome", TipoLandingPageCampo.TEXT, "Seu nome", true, 1);
        criarCampo(formulario, "email", "E-mail", TipoLandingPageCampo.EMAIL, "voce@email.com", true, 2);
        criarCampo(formulario, "telefone", "Telefone", TipoLandingPageCampo.PHONE, "(11) 99999-9999", false, 3);
        criarCampo(formulario, "mensagem", "Mensagem", TipoLandingPageCampo.TEXTAREA, "Como podemos ajudar?", false, 4);
    }

    private void criarCampo(
            LandingPageFormulario formulario,
            String nomeInterno,
            String label,
            TipoLandingPageCampo tipo,
            String placeholder,
            boolean obrigatorio,
            int ordem) {
        landingPageCampoRepository.save(LandingPageCampo.builder()
                .formulario(formulario)
                .nomeInterno(nomeInterno)
                .label(label)
                .tipo(tipo)
                .placeholder(placeholder)
                .obrigatorio(obrigatorio)
                .ordem(ordem)
                .ativo(true)
                .build());
    }

    private LandingPageResponse toResponse(LandingPage landingPage) {
        LandingPageResponse response = MapperUtil.toResponse(landingPage);
        response.setQuantidadeFormularios(
                (int) landingPageFormularioRepository.countByLandingPageId(landingPage.getId()));
        response.setQuantidadeLeads((int) landingPageLeadRepository.countByLandingPageId(landingPage.getId()));
        return response;
    }

    private String normalizarSlug(String slug) {
        return slug == null ? null : slug.trim().toLowerCase();
    }

    private void validarSlugUnico(String slug, Long idExcluir) {
        if (slug == null || slug.isBlank()) {
            return;
        }

        boolean existe = idExcluir == null
                ? landingPageRepository.findBySlug(slug).isPresent()
                : landingPageRepository.findBySlug(slug)
                        .map(lp -> !lp.getId().equals(idExcluir))
                        .orElse(false);

        if (existe) {
            throw new BusinessException("Já existe uma Landing Page com o slug: " + slug);
        }
    }
}
