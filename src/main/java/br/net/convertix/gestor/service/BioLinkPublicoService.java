package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.response.BioLinkItemPublicoResponse;
import br.net.convertix.gestor.dto.response.BioLinkPublicoResponse;
import br.net.convertix.gestor.entity.BioLink;
import br.net.convertix.gestor.entity.BioLinkItem;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.enums.StatusSite;
import br.net.convertix.gestor.enums.TipoSite;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.BioLinkItemRepository;
import br.net.convertix.gestor.repository.BioLinkRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BioLinkPublicoService {

    private final SiteRepository siteRepository;
    private final BioLinkRepository bioLinkRepository;
    private final BioLinkItemRepository bioLinkItemRepository;

    @Transactional(readOnly = true)
    public BioLinkPublicoResponse buscarPorSiteId(Long siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("BioLink não encontrado"));

        if (site.getTipo() != TipoSite.BIOLINK || site.getStatus() != StatusSite.ATIVO) {
            throw new ResourceNotFoundException("BioLink não encontrado");
        }

        BioLink bioLink = bioLinkRepository.findBySiteId(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("BioLink não encontrado"));

        List<BioLinkItemPublicoResponse> itens = bioLinkItemRepository
                .findByBioLinkIdAndAtivoTrueOrderByOrdemAsc(bioLink.getId())
                .stream()
                .map(this::toItemPublico)
                .toList();

        return BioLinkPublicoResponse.builder()
                .siteId(site.getId())
                .nomeUsuario(bioLink.getNomeUsuario())
                .descricao(bioLink.getDescricao())
                .fotoPerfil(bioLink.getFotoPerfil())
                .itens(itens)
                .build();
    }

    private BioLinkItemPublicoResponse toItemPublico(BioLinkItem item) {
        return BioLinkItemPublicoResponse.builder()
                .titulo(item.getTitulo())
                .url(item.getUrl())
                .icone(item.getIcone())
                .ordem(item.getOrdem())
                .build();
    }
}
