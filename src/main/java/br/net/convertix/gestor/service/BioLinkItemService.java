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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Transactional
    public List<BioLinkItemResponse> reordenar(Long bioLinkId, List<Long> ids) {
        autorizacaoService.validarAcessoBioLink(bioLinkId);

        List<BioLinkItem> itens = bioLinkItemRepository.findByBioLinkIdOrderByOrdemAsc(bioLinkId);
        Set<Long> atuais = itens.stream().map(BioLinkItem::getId).collect(Collectors.toSet());
        List<Long> informados = ids == null ? List.of() : ids.stream().filter(Objects::nonNull).toList();

        if (informados.size() != atuais.size() || !atuais.equals(new HashSet<>(informados))) {
            throw new BusinessException("A lista de itens para reordenar é inválida");
        }

        Map<Long, BioLinkItem> porId = itens.stream()
                .collect(Collectors.toMap(BioLinkItem::getId, Function.identity()));

        for (int i = 0; i < informados.size(); i++) {
            porId.get(informados.get(i)).setOrdem(i + 1);
        }

        return bioLinkItemRepository.saveAll(itens).stream()
                .sorted((a, b) -> Integer.compare(a.getOrdem(), b.getOrdem()))
                .map(MapperUtil::toResponse)
                .toList();
    }
}
