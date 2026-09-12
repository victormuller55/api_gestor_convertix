package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.PlanoRequest;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.dto.response.PlanoResponse;
import br.net.convertix.gestor.entity.Plano;
import br.net.convertix.gestor.enums.TipoProjeto;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.repository.AssinaturaRepository;
import br.net.convertix.gestor.repository.PlanoRepository;
import br.net.convertix.gestor.repository.spec.PlanoSpecification;
import br.net.convertix.gestor.security.SecurityUtil;
import br.net.convertix.gestor.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PlanoService {

    private final PlanoRepository planoRepository;
    private final AssinaturaRepository assinaturaRepository;

    @Transactional(readOnly = true)
    public PageResponse<PlanoResponse> buscar(String query, TipoProjeto tipo, Boolean ativo, int page, int size) {
        SecurityUtil.exigirAdmin();
        Page<Plano> resultado = planoRepository.findAll(
                PlanoSpecification.comFiltros(query, tipo, ativo),
                PaginationUtil.of(page, size, Sort.by(Sort.Order.asc("ordem"), Sort.Order.asc("id"))));
        return PaginationUtil.toResponse(resultado, this::toResponse);
    }

    @Transactional
    public PlanoResponse criar(PlanoRequest request) {
        SecurityUtil.exigirAdmin();
        validar(request, null);
        Plano plano = aplicar(new Plano(), request);
        return toResponse(planoRepository.save(plano));
    }

    @Transactional
    public PlanoResponse atualizar(Long id, PlanoRequest request) {
        SecurityUtil.exigirAdmin();
        Plano plano = buscarEntidade(id);
        validar(request, id);
        aplicar(plano, request);
        return toResponse(planoRepository.save(plano));
    }

    @Transactional
    public void excluir(Long id) {
        SecurityUtil.exigirAdmin();
        Plano plano = buscarEntidade(id);
        if (assinaturaRepository.existsByPlanoId(id)) {
            throw new BusinessException("Não é possível excluir: há assinaturas neste plano. Desative-o.");
        }
        planoRepository.delete(plano);
    }

    private Plano buscarEntidade(Long id) {
        return planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado com id: " + id));
    }

    private void validar(PlanoRequest request, Long idAtual) {
        String codigo = blankToNull(request.getCodigo());
        if (codigo != null) {
            boolean duplicado = idAtual == null
                    ? planoRepository.existsByCodigo(codigo)
                    : planoRepository.existsByCodigoAndIdNot(codigo, idAtual);
            if (duplicado) {
                throw new BusinessException("Já existe um plano com este código");
            }
        }

        boolean valorLivre = Boolean.TRUE.equals(request.getValorLivre());
        if (!valorLivre && (request.getValor() == null || request.getValor().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException("Informe um valor maior que zero ou marque valor livre");
        }
    }

    private Plano aplicar(Plano plano, PlanoRequest request) {
        boolean valorLivre = Boolean.TRUE.equals(request.getValorLivre());
        plano.setCodigo(blankToNull(request.getCodigo()));
        plano.setNome(request.getNome().trim());
        plano.setTipo(request.getTipo());
        plano.setVinculo(request.getVinculo());
        plano.setValorLivre(valorLivre);
        plano.setValor(valorLivre ? null : request.getValor());
        plano.setCiclo(request.getCiclo());
        plano.setDescricaoPadrao(blankToNull(request.getDescricaoPadrao()));
        plano.setAtivo(Boolean.TRUE.equals(request.getAtivo()));
        plano.setOrdem(request.getOrdem() == null ? 0 : request.getOrdem());
        return plano;
    }

    private PlanoResponse toResponse(Plano plano) {
        return PlanoResponse.builder()
                .id(plano.getId())
                .codigo(plano.getCodigo())
                .nome(plano.getNome())
                .tipo(plano.getTipo())
                .vinculo(plano.getVinculo())
                .valor(plano.getValor())
                .valorLivre(plano.isValorLivre())
                .ciclo(plano.getCiclo())
                .descricaoPadrao(plano.getDescricaoPadrao())
                .ativo(plano.isAtivo())
                .ordem(plano.getOrdem())
                .createdAt(plano.getCreatedAt())
                .updatedAt(plano.getUpdatedAt())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
