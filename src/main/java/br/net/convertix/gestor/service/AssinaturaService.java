package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.AssinaturaRequest;
import br.net.convertix.gestor.dto.request.AssinaturaUpdateRequest;
import br.net.convertix.gestor.dto.request.CartaoCreditoRequest;
import br.net.convertix.gestor.dto.request.CartaoTitularRequest;
import br.net.convertix.gestor.dto.response.AssinaturaResponse;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.entity.AplicativoMobile;
import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.entity.Plano;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.enums.CicloAssinatura;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.enums.VinculoPlano;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.integration.payment.PaymentGateway;
import br.net.convertix.gestor.repository.AplicativoMobileRepository;
import br.net.convertix.gestor.repository.AssinaturaRepository;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.PagamentoRepository;
import br.net.convertix.gestor.repository.PlanoRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.spec.AssinaturaSpecification;
import br.net.convertix.gestor.util.FinanceiroMapperUtil;
import br.net.convertix.gestor.util.PaginationUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final SiteRepository siteRepository;
    private final AplicativoMobileRepository aplicativoMobileRepository;
    private final PlanoRepository planoRepository;
    private final AutorizacaoService autorizacaoService;
    private final PagamentoService pagamentoService;
    private final PaymentGateway paymentGateway;
    private final LiberacaoService liberacaoService;
    private final CobrancaAssinaturaService cobrancaAssinaturaService;

    @Transactional
    public AssinaturaResponse criar(AssinaturaRequest request, HttpServletRequest httpRequest) {
        validarCartaoSeNecessario(request);

        Long clienteId = autorizacaoService.resolverClienteId(request.getClienteId());
        if (clienteId == null) {
            throw new BusinessException("O cliente é obrigatório");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));

        Site site = carregarSite(request.getSiteId());
        AplicativoMobile aplicativo = carregarAplicativo(request.getAplicativoMobileId());
        Plano plano = carregarPlano(request.getPlanoId());
        validarVinculoAssinatura(cliente.getId(), site, aplicativo);
        validarVinculoPlano(plano, site, aplicativo);
        validarAplicativoSemAssinaturaAtiva(aplicativo, null);

        BigDecimal valor = request.getValor();
        CicloAssinatura ciclo = request.getCiclo();
        String descricao = request.getDescricao();
        if (plano != null) {
            if (!plano.isValorLivre()) {
                if (plano.getValor() == null) {
                    throw new BusinessException("O plano selecionado não tem valor cadastrado");
                }
                valor = plano.getValor();
                ciclo = plano.getCiclo();
            }
            if (!StringUtils.hasText(descricao)) {
                descricao = plano.getDescricaoPadrao();
            }
        }
        if (!StringUtils.hasText(descricao)) {
            throw new BusinessException("A descrição é obrigatória");
        }

        String customerId = pagamentoService.garantirCustomerAsaas(cliente);
        String remoteIp = resolverIp(httpRequest);

        PaymentGateway.GatewaySubscription gateway = paymentGateway.criarAssinatura(
                new PaymentGateway.GatewaySubscriptionRequest(
                        customerId,
                        valor,
                        descricao,
                        ciclo,
                        request.getFormaPagamento(),
                        request.getProximaCobranca(),
                        request.getExternalReference(),
                        request.getCreditCardToken(),
                        toGatewayCard(request.getCreditCard()),
                        toGatewayHolder(request.getCreditCardHolderInfo()),
                        remoteIp
                ));

        Assinatura assinatura = Assinatura.builder()
                .cliente(cliente)
                .site(site)
                .aplicativoMobile(aplicativo)
                .plano(plano)
                .asaasSubscriptionId(gateway.id())
                .valor(valor)
                .descricao(descricao)
                .ciclo(ciclo)
                .formaPagamento(request.getFormaPagamento())
                .status(gateway.status())
                .proximaCobranca(gateway.nextDueDate() != null ? gateway.nextDueDate() : request.getProximaCobranca())
                .creditCardToken(gateway.creditCardToken() != null ? gateway.creditCardToken() : request.getCreditCardToken())
                .externalReference(request.getExternalReference())
                .mensagemAsaas(gateway.message())
                .build();

        assinatura = assinaturaRepository.save(assinatura);
        liberacaoService.processarStatusAssinatura(assinatura);
        cobrancaAssinaturaService.garantirProximoPagamento(assinatura);
        log.info("Assinatura {} criada para cliente {}", assinatura.getId(), cliente.getId());
        List<Pagamento> cobrancas = pagamentoRepository.findByAssinaturaIdOrderByCreatedAtDesc(assinatura.getId());
        return FinanceiroMapperUtil.toResponse(assinatura, cobrancas);
    }

    @Transactional(readOnly = true)
    public PageResponse<AssinaturaResponse> listar(StatusAssinatura status, int page, int size) {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        Page<Assinatura> resultado = assinaturaRepository.findAll(
                AssinaturaSpecification.comFiltros(clienteIdFiltro, status),
                PaginationUtil.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PaginationUtil.toResponse(resultado, FinanceiroMapperUtil::toResponse);
    }

    @Transactional(readOnly = true)
    public AssinaturaResponse buscarPorId(Long id) {
        Assinatura assinatura = carregarAutorizada(id);
        List<Pagamento> cobrancas = pagamentoRepository.findByAssinaturaIdOrderByCreatedAtDesc(assinatura.getId());
        return FinanceiroMapperUtil.toResponse(assinatura, cobrancas);
    }

    @Transactional
    public AssinaturaResponse atualizar(Long id, AssinaturaUpdateRequest request) {
        Assinatura assinatura = carregarAutorizada(id);
        if (!StringUtils.hasText(assinatura.getAsaasSubscriptionId())) {
            throw new BusinessException("Assinatura sem identificador no Asaas");
        }

        PaymentGateway.GatewaySubscription remoto = paymentGateway.atualizarAssinatura(
                assinatura.getAsaasSubscriptionId(),
                new PaymentGateway.GatewaySubscriptionUpdateRequest(
                        request.getValor(),
                        request.getDescricao(),
                        request.getCiclo(),
                        request.getFormaPagamento(),
                        request.getProximaCobranca(),
                        request.getUpdatePendingPayments()
                ));

        if (request.getValor() != null) {
            assinatura.setValor(request.getValor());
        }
        if (request.getDescricao() != null) {
            assinatura.setDescricao(request.getDescricao());
        }
        if (request.getCiclo() != null) {
            assinatura.setCiclo(request.getCiclo());
        }
        if (request.getFormaPagamento() != null) {
            assinatura.setFormaPagamento(request.getFormaPagamento());
        }
        if (request.getProximaCobranca() != null) {
            assinatura.setProximaCobranca(request.getProximaCobranca());
        }
        if (request.getSiteId() != null) {
            Site site = carregarSite(request.getSiteId());
            AplicativoMobile aplicativo = request.getAplicativoMobileId() != null
                    ? carregarAplicativo(request.getAplicativoMobileId())
                    : assinatura.getAplicativoMobile();
            validarVinculoAssinatura(assinatura.getCliente().getId(), site, aplicativo);
            validarAplicativoSemAssinaturaAtiva(aplicativo, assinatura.getId());
            assinatura.setSite(site);
            if (request.getAplicativoMobileId() != null) {
                assinatura.setAplicativoMobile(aplicativo);
            } else if (site != null) {
                assinatura.setAplicativoMobile(null);
            }
        } else if (request.getAplicativoMobileId() != null) {
            AplicativoMobile aplicativo = carregarAplicativo(request.getAplicativoMobileId());
            validarVinculoAssinatura(assinatura.getCliente().getId(), null, aplicativo);
            validarAplicativoSemAssinaturaAtiva(aplicativo, assinatura.getId());
            assinatura.setAplicativoMobile(aplicativo);
            assinatura.setSite(null);
        }

        assinatura.setStatus(remoto.status());
        if (remoto.nextDueDate() != null) {
            assinatura.setProximaCobranca(remoto.nextDueDate());
        }
        assinatura.setMensagemAsaas(remoto.message());

        assinatura = assinaturaRepository.save(assinatura);
        liberacaoService.processarStatusAssinatura(assinatura);
        return FinanceiroMapperUtil.toResponse(assinatura);
    }

    @Transactional
    public AssinaturaResponse cancelar(Long id) {
        Assinatura assinatura = carregarAutorizada(id);
        if (!StringUtils.hasText(assinatura.getAsaasSubscriptionId())) {
            throw new BusinessException("Assinatura sem identificador no Asaas");
        }

        PaymentGateway.GatewaySubscription remoto = paymentGateway.cancelarAssinatura(assinatura.getAsaasSubscriptionId());
        assinatura.setStatus(remoto.status() != null ? remoto.status() : StatusAssinatura.INACTIVE);
        assinatura.setMensagemAsaas(remoto.message());
        assinatura = assinaturaRepository.save(assinatura);
        liberacaoService.processarStatusAssinatura(assinatura);
        return FinanceiroMapperUtil.toResponse(assinatura);
    }

    @Transactional
    public void aplicarAssinaturaDoWebhook(PaymentGateway.GatewaySubscription gatewaySubscription) {
        if (gatewaySubscription == null || !StringUtils.hasText(gatewaySubscription.id())) {
            return;
        }

        Assinatura assinatura = assinaturaRepository.findByAsaasSubscriptionId(gatewaySubscription.id()).orElse(null);
        if (assinatura == null) {
            log.warn("Webhook de assinatura {} ignorado: assinatura local não encontrada", gatewaySubscription.id());
            return;
        }

        assinatura.setStatus(gatewaySubscription.status());
        assinatura.setProximaCobranca(gatewaySubscription.nextDueDate());
        if (gatewaySubscription.value() != null) {
            assinatura.setValor(gatewaySubscription.value());
        }
        assinatura.setMensagemAsaas(gatewaySubscription.message());
        assinaturaRepository.save(assinatura);
        liberacaoService.processarStatusAssinatura(assinatura);
        if (assinatura.getStatus() == StatusAssinatura.ACTIVE) {
            cobrancaAssinaturaService.garantirProximoPagamento(assinatura);
        }
    }

    Assinatura buscarPorAsaasId(String asaasSubscriptionId) {
        if (!StringUtils.hasText(asaasSubscriptionId)) {
            return null;
        }
        return assinaturaRepository.findByAsaasSubscriptionId(asaasSubscriptionId).orElse(null);
    }

    private Assinatura carregarAutorizada(Long id) {
        Assinatura assinatura = assinaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada com id: " + id));
        autorizacaoService.validarAcessoCliente(assinatura.getCliente().getId());
        return assinatura;
    }

    private Site carregarSite(Long siteId) {
        if (siteId == null) {
            return null;
        }
        autorizacaoService.validarAcessoSite(siteId);
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + siteId));
    }

    private AplicativoMobile carregarAplicativo(Long aplicativoMobileId) {
        if (aplicativoMobileId == null) {
            return null;
        }
        autorizacaoService.validarAcessoAplicativoMobile(aplicativoMobileId);
        return aplicativoMobileRepository.findById(aplicativoMobileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aplicativo mobile não encontrado com id: " + aplicativoMobileId));
    }

    private Plano carregarPlano(Long planoId) {
        if (planoId == null) {
            return null;
        }
        Plano plano = planoRepository.findById(planoId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado com id: " + planoId));
        if (!plano.isAtivo()) {
            throw new BusinessException("Este plano está inativo");
        }
        return plano;
    }

    private void validarVinculoPlano(Plano plano, Site site, AplicativoMobile aplicativo) {
        if (plano == null) {
            return;
        }
        if (plano.getVinculo() == VinculoPlano.SITE && site == null) {
            throw new BusinessException("Este plano exige um site");
        }
        if (plano.getVinculo() == VinculoPlano.APLICATIVO && aplicativo == null) {
            throw new BusinessException("Este plano exige um aplicativo mobile");
        }
        if (plano.getVinculo() == VinculoPlano.SITE && aplicativo != null) {
            throw new BusinessException("Este plano não aceita aplicativo mobile");
        }
        if (plano.getVinculo() == VinculoPlano.APLICATIVO && site != null) {
            throw new BusinessException("Este plano não aceita site");
        }
    }

    private void validarVinculoAssinatura(Long clienteId, Site site, AplicativoMobile aplicativo) {
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

    private void validarAplicativoSemAssinaturaAtiva(AplicativoMobile aplicativo, Long assinaturaIdExcluir) {
        if (aplicativo == null || aplicativo.getId() == null) {
            return;
        }
        boolean existe = assinaturaIdExcluir == null
                ? assinaturaRepository.existsByAplicativoMobileIdAndStatus(
                        aplicativo.getId(), StatusAssinatura.ACTIVE)
                : assinaturaRepository.existsByAplicativoMobileIdAndStatusAndIdNot(
                        aplicativo.getId(), StatusAssinatura.ACTIVE, assinaturaIdExcluir);
        if (existe) {
            throw new BusinessException("Já existe uma assinatura ativa para este aplicativo");
        }
    }

    private void validarCartaoSeNecessario(AssinaturaRequest request) {
        if (request.getFormaPagamento() != FormaPagamento.CREDIT_CARD) {
            return;
        }
        boolean temToken = StringUtils.hasText(request.getCreditCardToken());
        boolean temCartao = request.getCreditCard() != null && request.getCreditCardHolderInfo() != null;
        if (!temToken && !temCartao) {
            throw new BusinessException("Para assinatura com cartão informe credit_card_token ou dados do cartão/titular");
        }
    }

    private PaymentGateway.GatewayCreditCard toGatewayCard(CartaoCreditoRequest card) {
        if (card == null) {
            return null;
        }
        return new PaymentGateway.GatewayCreditCard(
                card.getHolderName(),
                card.getNumber(),
                card.getExpiryMonth(),
                card.getExpiryYear(),
                card.getCcv());
    }

    private PaymentGateway.GatewayCreditCardHolder toGatewayHolder(CartaoTitularRequest holder) {
        if (holder == null) {
            return null;
        }
        return new PaymentGateway.GatewayCreditCardHolder(
                holder.getName(),
                holder.getEmail(),
                holder.getCpfCnpj(),
                holder.getPostalCode(),
                holder.getAddressNumber(),
                holder.getAddressComplement(),
                holder.getPhone(),
                holder.getMobilePhone());
    }

    private String resolverIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
