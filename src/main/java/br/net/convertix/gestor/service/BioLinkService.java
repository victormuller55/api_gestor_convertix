package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.BioLinkRequest;
import br.net.convertix.gestor.dto.response.BioLinkResponse;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.entity.BioLink;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.enums.TipoSite;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.BioLinkRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.spec.BioLinkSpecification;
import br.net.convertix.gestor.util.MapperUtil;
import br.net.convertix.gestor.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BioLinkService {

    private static final String PASTA_FOTOS = "biolinks";

    private final BioLinkRepository bioLinkRepository;
    private final SiteRepository siteRepository;
    private final AutorizacaoService autorizacaoService;
    private final ArquivoService arquivoService;

    @Transactional(readOnly = true)
    public PageResponse<BioLinkResponse> buscar(Long id, int page, int size) {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        Page<BioLink> resultado = bioLinkRepository.findAll(
                BioLinkSpecification.comFiltros(id, clienteIdFiltro),
                PaginationUtil.of(page, size));
        return PaginationUtil.toResponse(resultado, MapperUtil::toResponse);
    }

    @Transactional
    public BioLinkResponse criar(BioLinkRequest request, MultipartFile foto) {
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + request.getSiteId()));

        autorizacaoService.validarAcessoSite(site.getId());

        if (site.getTipo() != TipoSite.BIOLINK) {
            throw new BusinessException("O site informado não é do tipo BIOLINK");
        }

        if (bioLinkRepository.existsBySiteId(request.getSiteId())) {
            throw new BusinessException("Já existe um BioLink vinculado ao site: " + request.getSiteId());
        }

        BioLink bioLink = BioLink.builder()
                .site(site)
                .nomeUsuario(request.getNomeUsuario())
                .descricao(request.getDescricao())
                .fotoPerfil(arquivoService.salvar(foto, PASTA_FOTOS))
                .build();

        return MapperUtil.toResponse(bioLinkRepository.save(bioLink));
    }

    @Transactional
    public BioLinkResponse atualizar(Long id, BioLinkRequest request, MultipartFile foto) {
        BioLink bioLink = bioLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BioLink não encontrado com id: " + id));

        autorizacaoService.validarAcessoBioLink(id);

        if (!bioLink.getSite().getId().equals(request.getSiteId())) {
            Site site = siteRepository.findById(request.getSiteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + request.getSiteId()));

            autorizacaoService.validarAcessoSite(site.getId());

            if (site.getTipo() != TipoSite.BIOLINK) {
                throw new BusinessException("O site informado não é do tipo BIOLINK");
            }

            if (bioLinkRepository.existsBySiteIdAndIdNot(request.getSiteId(), id)) {
                throw new BusinessException("Já existe um BioLink vinculado ao site: " + request.getSiteId());
            }

            bioLink.setSite(site);
        }

        MapperUtil.updateEntity(bioLink, request);
        atualizarFotoPerfil(bioLink, foto);
        return MapperUtil.toResponse(bioLinkRepository.save(bioLink));
    }

    @Transactional
    public void excluir(Long id) {
        BioLink bioLink = bioLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BioLink não encontrado com id: " + id));

        autorizacaoService.validarAcessoBioLink(id);
        arquivoService.excluir(bioLink.getFotoPerfil());

        Site site = bioLink.getSite();
        if (site != null) {
            site.setBioLink(null);
            bioLink.setSite(null);
        }

        bioLinkRepository.delete(bioLink);
    }

    private void atualizarFotoPerfil(BioLink bioLink, MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            return;
        }
        arquivoService.excluir(bioLink.getFotoPerfil());
        bioLink.setFotoPerfil(arquivoService.salvar(foto, PASTA_FOTOS));
    }
}
