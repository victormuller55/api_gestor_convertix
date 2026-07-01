package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.BioLinkItemRequest;
import br.net.convertix.gestor.dto.response.BioLinkItemResponse;
import br.net.convertix.gestor.entity.BioLink;
import br.net.convertix.gestor.entity.BioLinkItem;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.BioLinkItemRepository;
import br.net.convertix.gestor.repository.BioLinkRepository;
import br.net.convertix.gestor.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BioLinkItemService {

    private final BioLinkItemRepository bioLinkItemRepository;
    private final BioLinkRepository bioLinkRepository;
    private final AutorizacaoService autorizacaoService;

    @Transactional(readOnly = true)
    public List<BioLinkItemResponse> listarPorBioLink(Long bioLinkId) {
        autorizacaoService.validarAcessoBioLink(bioLinkId);
        return bioLinkItemRepository.findByBioLinkIdOrderByOrdemAsc(bioLinkId).stream()
                .map(MapperUtil::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BioLinkItemResponse buscarPorId(Long bioLinkId, Long id) {
        autorizacaoService.validarAcessoBioLink(bioLinkId);
        return bioLinkItemRepository.findByIdAndBioLinkId(id, bioLinkId)
                .map(MapperUtil::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item não encontrado com id: " + id + " para o BioLink: " + bioLinkId));
    }

    @Transactional
    public BioLinkItemResponse criar(Long bioLinkId, BioLinkItemRequest request) {
        if (bioLinkId == null) {
            throw new BusinessException("O biolink é obrigatório");
        }

        autorizacaoService.validarAcessoBioLink(bioLinkId);

        BioLink bioLink = bioLinkRepository.findById(bioLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("BioLink não encontrado com id: " + bioLinkId));

        BioLinkItem item = MapperUtil.toEntity(request, bioLink);
        return MapperUtil.toResponse(bioLinkItemRepository.save(item));
    }

    @Transactional
    public BioLinkItemResponse atualizar(Long bioLinkId, Long id, BioLinkItemRequest request) {
        autorizacaoService.validarAcessoBioLink(bioLinkId);

        BioLinkItem item = bioLinkItemRepository.findByIdAndBioLinkId(id, bioLinkId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item não encontrado com id: " + id + " para o BioLink: " + bioLinkId));

        MapperUtil.updateEntity(item, request);
        return MapperUtil.toResponse(bioLinkItemRepository.save(item));
    }

    @Transactional
    public void excluir(Long bioLinkId, Long id) {
        autorizacaoService.validarAcessoBioLink(bioLinkId);

        BioLinkItem item = bioLinkItemRepository.findByIdAndBioLinkId(id, bioLinkId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item não encontrado com id: " + id + " para o BioLink: " + bioLinkId));

        BioLink bioLink = item.getBioLink();
        if (bioLink != null) {
            bioLink.getItens().remove(item);
            item.setBioLink(null);
        }

        bioLinkItemRepository.delete(item);
    }
}
