package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.AssinaturaRequest;
import br.net.convertix.gestor.dto.request.AssinaturaUpdateRequest;
import br.net.convertix.gestor.dto.request.CartaoCreditoRequest;
import br.net.convertix.gestor.dto.request.CartaoTitularRequest;
import br.net.convertix.gestor.dto.response.AssinaturaResponse;
import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.StatusAssinatura;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.integration.payment.PaymentGateway;
import br.net.convertix.gestor.repository.AssinaturaRepository;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.PagamentoRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.spec.AssinaturaSpecification;
import br.net.convertix.gestor.util.FinanceiroMapperUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final SiteRepository siteRepository;
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

        Site site = null;
        if (request.getSiteId() != null) {
            autorizacaoService.validarAcessoSite(request.getSiteId());
            site = siteRepository.findById(request.getSiteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + request.getSiteId()));
        }

        String customerId = pagamentoService.garantirCustomerAsaas(cliente);
        String remoteIp = resolverIp(httpRequest);

        PaymentGateway.GatewaySubscription gateway = paymentGateway.criarAssinatura(
                new PaymentGateway.GatewaySubscriptionRequest(
                        customerId,
                        request.getValor(),
                        request.getDescricao(),
                        request.getCiclo(),
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
                .asaasSubscriptionId(gateway.id())
                .valor(request.getValor())
                .descricao(request.getDescricao())
                .ciclo(request.getCiclo())
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
    public List<AssinaturaResponse> listar(StatusAssinatura status) {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        return assinaturaRepository.findAll(
                        AssinaturaSpecification.comFiltros(clienteIdFiltro, status),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(FinanceiroMapperUtil::toResponse)
                .collect(Collectors.toList());
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
            autorizacaoService.validarAcessoSite(request.getSiteId());
            Site site = siteRepository.findById(request.getSiteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + request.getSiteId()));
            assinatura.setSite(site);
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
