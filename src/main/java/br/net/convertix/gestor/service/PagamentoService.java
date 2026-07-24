package br.net.convertix.gestor.service;

import br.net.convertix.gestor.dto.request.CartaoCreditoRequest;
import br.net.convertix.gestor.dto.request.CartaoTitularRequest;
import br.net.convertix.gestor.dto.request.EstornoPagamentoRequest;
import br.net.convertix.gestor.dto.request.PagamentoCartaoRequest;
import br.net.convertix.gestor.dto.request.PagamentoPixRequest;
import br.net.convertix.gestor.dto.request.PagamentoRequest;
import br.net.convertix.gestor.dto.response.PageResponse;
import br.net.convertix.gestor.dto.response.PagamentoResponse;
import br.net.convertix.gestor.dto.response.PagamentoResumoResponse;
import br.net.convertix.gestor.entity.Assinatura;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.HistoricoStatusPagamento;
import br.net.convertix.gestor.entity.Pagamento;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.enums.FormaPagamento;
import br.net.convertix.gestor.enums.OrigemAlteracaoStatus;
import br.net.convertix.gestor.enums.StatusPagamento;
import br.net.convertix.gestor.exception.BusinessException;
import br.net.convertix.gestor.exception.ResourceNotFoundException;
import br.net.convertix.gestor.integration.payment.PaymentGateway;
import br.net.convertix.gestor.repository.ClienteRepository;
import br.net.convertix.gestor.repository.PagamentoRepository;
import br.net.convertix.gestor.repository.SiteRepository;
import br.net.convertix.gestor.repository.spec.PagamentoSpecification;
import br.net.convertix.gestor.util.FinanceiroMapperUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final SiteRepository siteRepository;
    private final AutorizacaoService autorizacaoService;
    private final PaymentGateway paymentGateway;
    private final LiberacaoService liberacaoService;
    private final CobrancaAssinaturaService cobrancaAssinaturaService;

    /**
     * Cobrança sem forma fixa: Asaas UNDEFINED — o cliente escolhe ao pagar (invoice_url).
     */
    @Transactional
    public PagamentoResponse criar(PagamentoRequest request) {
        Cliente cliente = carregarClienteAutorizado(request.getClienteId());
        Site site = carregarSiteOpcional(request.getSiteId());

        String customerId = garantirCustomerAsaas(cliente);

        PaymentGateway.GatewayPayment gatewayPayment = paymentGateway.criarPagamento(
                new PaymentGateway.GatewayPaymentRequest(
                        customerId,
                        request.getValor(),
                        request.getDescricao(),
                        null,
                        request.getDataVencimento() != null ? request.getDataVencimento() : LocalDate.now(),
                        null,
                        request.getExternalReference(),
                        null,
                        null,
                        null,
                        null
                ));

        Pagamento pagamento = Pagamento.builder()
                .cliente(cliente)
                .site(site)
                .asaasPaymentId(gatewayPayment.id())
                .valor(request.getValor())
                .descricao(request.getDescricao())
                .status(gatewayPayment.status())
                .formaPagamento(null)
                .parcelas(1)
                .invoiceUrl(gatewayPayment.invoiceUrl())
                .comprovanteUrl(gatewayPayment.transactionReceiptUrl())
                .dataVencimento(gatewayPayment.dueDate())
                .dataConfirmacao(gatewayPayment.confirmedDate())
                .mensagemAsaas(gatewayPayment.message())
                .externalReference(request.getExternalReference())
                .build();

        registrarHistorico(
                pagamento,
                null,
                pagamento.getStatus(),
                OrigemAlteracaoStatus.CRIACAO,
                "Cobrança criada — forma de pagamento a escolher pelo cliente");
        pagamento = pagamentoRepository.save(pagamento);

        liberacaoService.processarStatusPagamento(pagamento.getStatus(), site, null);
        log.info("Pagamento {} criado (UNDEFINED) para cliente {}", pagamento.getId(), cliente.getId());
        return FinanceiroMapperUtil.toResponse(pagamento, true);
    }

    @Transactional
    public PagamentoResponse criarPix(PagamentoPixRequest request) {
        Cliente cliente = carregarClienteAutorizado(request.getClienteId());
        Site site = carregarSiteOpcional(request.getSiteId());

        String customerId = garantirCustomerAsaas(cliente);

        PaymentGateway.GatewayPayment gatewayPayment = paymentGateway.criarPagamento(
                new PaymentGateway.GatewayPaymentRequest(
                        customerId,
                        request.getValor(),
                        request.getDescricao(),
                        FormaPagamento.PIX,
                        request.getDataVencimento() != null ? request.getDataVencimento() : LocalDate.now(),
                        null,
                        request.getExternalReference(),
                        null,
                        null,
                        null,
                        null
                ));

        PaymentGateway.GatewayPixQrCode pix = paymentGateway.consultarPixQrCode(gatewayPayment.id());

        Pagamento pagamento = Pagamento.builder()
                .cliente(cliente)
                .site(site)
                .asaasPaymentId(gatewayPayment.id())
                .valor(request.getValor())
                .descricao(request.getDescricao())
                .status(gatewayPayment.status())
                .formaPagamento(FormaPagamento.PIX)
                .parcelas(1)
                .qrCode(pix.encodedImage())
                .codigoPix(pix.payload())
                .invoiceUrl(gatewayPayment.invoiceUrl())
                .comprovanteUrl(gatewayPayment.transactionReceiptUrl())
                .dataVencimento(gatewayPayment.dueDate())
                .dataConfirmacao(gatewayPayment.confirmedDate())
                .mensagemAsaas(gatewayPayment.message())
                .externalReference(request.getExternalReference())
                .build();

        registrarHistorico(pagamento, null, pagamento.getStatus(), OrigemAlteracaoStatus.CRIACAO, "Pagamento PIX criado");
        pagamento = pagamentoRepository.save(pagamento);

        liberacaoService.processarStatusPagamento(pagamento.getStatus(), site, null);
        log.info("Pagamento PIX {} criado para cliente {}", pagamento.getId(), cliente.getId());
        return FinanceiroMapperUtil.toResponse(pagamento, true);
    }

    @Transactional
    public PagamentoResponse criarCartao(PagamentoCartaoRequest request, HttpServletRequest httpRequest) {
        validarCartao(request);

        Cliente cliente = carregarClienteAutorizado(request.getClienteId());
        Site site = carregarSiteOpcional(request.getSiteId());
        String customerId = garantirCustomerAsaas(cliente);

        Integer parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        String remoteIp = resolverIp(httpRequest);

        PaymentGateway.GatewayPayment gatewayPayment = paymentGateway.criarPagamento(
                new PaymentGateway.GatewayPaymentRequest(
                        customerId,
                        request.getValor(),
                        request.getDescricao(),
                        FormaPagamento.CREDIT_CARD,
                        request.getDataVencimento() != null ? request.getDataVencimento() : LocalDate.now(),
                        parcelas,
                        request.getExternalReference(),
                        request.getCreditCardToken(),
                        toGatewayCard(request.getCreditCard()),
                        toGatewayHolder(request.getCreditCardHolderInfo()),
                        remoteIp
                ));

        Pagamento pagamento = Pagamento.builder()
                .cliente(cliente)
                .site(site)
                .asaasPaymentId(gatewayPayment.id())
                .valor(request.getValor())
                .descricao(request.getDescricao())
                .status(gatewayPayment.status())
                .formaPagamento(FormaPagamento.CREDIT_CARD)
                .parcelas(parcelas)
                .invoiceUrl(gatewayPayment.invoiceUrl())
                .comprovanteUrl(gatewayPayment.transactionReceiptUrl())
                .dataVencimento(gatewayPayment.dueDate())
                .dataConfirmacao(gatewayPayment.confirmedDate())
                .mensagemAsaas(gatewayPayment.message())
                .externalReference(request.getExternalReference())
                .build();

        registrarHistorico(pagamento, null, pagamento.getStatus(), OrigemAlteracaoStatus.CRIACAO, "Pagamento cartão criado");
        pagamento = pagamentoRepository.save(pagamento);

        liberacaoService.processarStatusPagamento(pagamento.getStatus(), site, null);
        log.info("Pagamento cartão {} criado para cliente {}", pagamento.getId(), cliente.getId());
        return FinanceiroMapperUtil.toResponse(pagamento, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<PagamentoResponse> listar(
            StatusPagamento status,
            FormaPagamento formaPagamento,
            LocalDate dataInicio,
            LocalDate dataFim,
            int page,
            int size) {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;

        Page<Pagamento> resultado = pagamentoRepository.findAll(
                PagamentoSpecification.comFiltros(clienteIdFiltro, status, formaPagamento, inicio, fim),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.<PagamentoResponse>builder()
                .content(resultado.getContent().stream().map(FinanceiroMapperUtil::toResponse).collect(Collectors.toList()))
                .page(resultado.getNumber())
                .size(resultado.getSize())
                .totalElements(resultado.getTotalElements())
                .totalPages(resultado.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public PagamentoResponse buscarPorId(Long id) {
        Pagamento pagamento = carregarPagamentoAutorizado(id);
        return FinanceiroMapperUtil.toResponse(pagamento, true);
    }

    @Transactional(readOnly = true)
    public List<PagamentoResumoResponse> listarUltimos() {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        List<Pagamento> pagamentos = clienteIdFiltro == null
                ? pagamentoRepository.findTop10ByOrderByCreatedAtDesc()
                : pagamentoRepository.findTop10ByClienteIdOrderByCreatedAtDesc(clienteIdFiltro);
        return pagamentos.stream().map(FinanceiroMapperUtil::toResumo).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> historicoCompleto() {
        Long clienteIdFiltro = autorizacaoService.getClienteIdFiltro();
        List<Pagamento> pagamentos = pagamentoRepository.findAll(
                PagamentoSpecification.comFiltros(clienteIdFiltro, null, null, null, null),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return pagamentos.stream()
                .map(p -> FinanceiroMapperUtil.toResponse(p, true))
                .collect(Collectors.toList());
    }

    @Transactional
    public PagamentoResponse sincronizarStatus(Long id) {
        Pagamento pagamento = carregarPagamentoAutorizado(id);
        if (!StringUtils.hasText(pagamento.getAsaasPaymentId())) {
            throw new BusinessException("Pagamento sem identificador no Asaas");
        }

        PaymentGateway.GatewayPayment remoto = paymentGateway.consultarPagamento(pagamento.getAsaasPaymentId());
        aplicarStatusGateway(pagamento, remoto, OrigemAlteracaoStatus.SINCRONIZACAO, "Sincronização manual com Asaas");

        if (pagamento.getFormaPagamento() == FormaPagamento.PIX
                && (!StringUtils.hasText(pagamento.getCodigoPix()) || !StringUtils.hasText(pagamento.getQrCode()))) {
            PaymentGateway.GatewayPixQrCode pix = paymentGateway.consultarPixQrCode(pagamento.getAsaasPaymentId());
            pagamento.setQrCode(pix.encodedImage());
            pagamento.setCodigoPix(pix.payload());
        }

        pagamento = pagamentoRepository.save(pagamento);
        liberacaoService.processarStatusPagamento(pagamento.getStatus(), pagamento.getSite(), pagamento.getAssinatura());
        gerarProximoPagamentoAssinaturaSeAplicavel(pagamento);
        return FinanceiroMapperUtil.toResponse(pagamento, true);
    }

    @Transactional
    public PagamentoResponse cancelar(Long id) {
        Pagamento pagamento = carregarPagamentoAutorizado(id);
        validarCancelamento(pagamento);

        PaymentGateway.GatewayPayment remoto = paymentGateway.cancelarPagamento(pagamento.getAsaasPaymentId());
        aplicarStatusGateway(pagamento, remoto, OrigemAlteracaoStatus.CANCELAMENTO, "Pagamento cancelado");
        if (pagamento.getStatus() != StatusPagamento.DELETED && pagamento.getStatus() != StatusPagamento.CANCELLED) {
            StatusPagamento anterior = pagamento.getStatus();
            pagamento.setStatus(StatusPagamento.CANCELLED);
            registrarHistorico(pagamento, anterior, StatusPagamento.CANCELLED, OrigemAlteracaoStatus.CANCELAMENTO, "Cancelamento forçado localmente");
        }

        pagamento = pagamentoRepository.save(pagamento);
        liberacaoService.processarStatusPagamento(pagamento.getStatus(), pagamento.getSite(), pagamento.getAssinatura());
        return FinanceiroMapperUtil.toResponse(pagamento, true);
    }

    @Transactional
    public PagamentoResponse estornar(Long id, EstornoPagamentoRequest request) {
        Pagamento pagamento = carregarPagamentoAutorizado(id);
        validarEstorno(pagamento);

        PaymentGateway.GatewayPayment remoto = paymentGateway.estornarPagamento(
                pagamento.getAsaasPaymentId(),
                request != null ? request.getValor() : null,
                request != null ? request.getDescricao() : null);

        aplicarStatusGateway(pagamento, remoto, OrigemAlteracaoStatus.ESTORNO, "Estorno solicitado");
        if (pagamento.getStatus() != StatusPagamento.REFUNDED) {
            StatusPagamento anterior = pagamento.getStatus();
            pagamento.setStatus(StatusPagamento.REFUNDED);
            registrarHistorico(pagamento, anterior, StatusPagamento.REFUNDED, OrigemAlteracaoStatus.ESTORNO, "Estorno aplicado");
        }

        pagamento = pagamentoRepository.save(pagamento);
        liberacaoService.processarStatusPagamento(pagamento.getStatus(), pagamento.getSite(), pagamento.getAssinatura());
        return FinanceiroMapperUtil.toResponse(pagamento, true);
    }

    @Transactional
    public void aplicarPagamentoDoWebhook(PaymentGateway.GatewayPayment gatewayPayment, Assinatura assinatura) {
        if (gatewayPayment == null || !StringUtils.hasText(gatewayPayment.id())) {
            return;
        }

        Pagamento pagamento = pagamentoRepository.findByAsaasPaymentId(gatewayPayment.id()).orElse(null);
        if (pagamento == null) {
            Cliente cliente = resolverClientePorCustomer(gatewayPayment.customerId());
            if (cliente == null) {
                log.warn("Webhook de pagamento {} ignorado: cliente Asaas não encontrado", gatewayPayment.id());
                return;
            }
            pagamento = Pagamento.builder()
                    .cliente(cliente)
                    .assinatura(assinatura)
                    .site(assinatura != null ? assinatura.getSite() : null)
                    .asaasPaymentId(gatewayPayment.id())
                    .valor(gatewayPayment.value())
                    .descricao(gatewayPayment.description())
                    .status(gatewayPayment.status())
                    .formaPagamento(gatewayPayment.billingType())
                    .parcelas(gatewayPayment.installmentCount())
                    .invoiceUrl(gatewayPayment.invoiceUrl())
                    .comprovanteUrl(gatewayPayment.transactionReceiptUrl())
                    .dataVencimento(gatewayPayment.dueDate())
                    .dataConfirmacao(gatewayPayment.confirmedDate())
                    .externalReference(gatewayPayment.externalReference())
                    .build();
            registrarHistorico(pagamento, null, pagamento.getStatus(), OrigemAlteracaoStatus.WEBHOOK, "Pagamento criado via webhook");
        } else {
            aplicarStatusGateway(pagamento, gatewayPayment, OrigemAlteracaoStatus.WEBHOOK, "Atualização via webhook");
            if (assinatura != null && pagamento.getAssinatura() == null) {
                pagamento.setAssinatura(assinatura);
                if (pagamento.getSite() == null) {
                    pagamento.setSite(assinatura.getSite());
                }
            }
        }

        pagamentoRepository.save(pagamento);
        liberacaoService.processarStatusPagamento(pagamento.getStatus(), pagamento.getSite(), pagamento.getAssinatura());
        gerarProximoPagamentoAssinaturaSeAplicavel(pagamento);
    }

    String garantirCustomerAsaas(Cliente cliente) {
        PaymentGateway.GatewayCustomer customer = paymentGateway.criarOuAtualizarCliente(
                new PaymentGateway.GatewayCustomerRequest(
                        cliente.getNomeEmpresa(),
                        cliente.getEmail(),
                        cliente.getDocumento(),
                        cliente.getTelefone(),
                        cliente.getTelefone(),
                        cliente.getAsaasCustomerId()
                ));
        if (!customer.id().equals(cliente.getAsaasCustomerId())) {
            cliente.setAsaasCustomerId(customer.id());
            clienteRepository.save(cliente);
        }
        return customer.id();
    }

    private void gerarProximoPagamentoAssinaturaSeAplicavel(Pagamento pagamento) {
        Assinatura assinatura = pagamento.getAssinatura();
        if (assinatura == null) {
            return;
        }
        StatusPagamento status = pagamento.getStatus();
        if (status != StatusPagamento.RECEIVED
                && status != StatusPagamento.CONFIRMED
                && status != StatusPagamento.OVERDUE) {
            return;
        }
        cobrancaAssinaturaService.avancarProximaCobrancaSePago(assinatura, pagamento);
        cobrancaAssinaturaService.garantirProximoPagamento(assinatura);
    }

    private void aplicarStatusGateway(
            Pagamento pagamento,
            PaymentGateway.GatewayPayment remoto,
            OrigemAlteracaoStatus origem,
            String mensagem) {
        StatusPagamento anterior = pagamento.getStatus();
        StatusPagamento novo = remoto.status();

        pagamento.setInvoiceUrl(remoto.invoiceUrl());
        pagamento.setComprovanteUrl(remoto.transactionReceiptUrl());
        pagamento.setDataVencimento(remoto.dueDate());
        pagamento.setDataConfirmacao(remoto.confirmedDate());
        pagamento.setMensagemAsaas(remoto.message() != null ? remoto.message() : remoto.rawStatus());
        if (remoto.value() != null) {
            pagamento.setValor(remoto.value());
        }
        if (remoto.description() != null) {
            pagamento.setDescricao(remoto.description());
        }
        if (remoto.billingType() != null) {
            pagamento.setFormaPagamento(remoto.billingType());
        }

        if (anterior != novo) {
            pagamento.setStatus(novo);
            registrarHistorico(pagamento, anterior, novo, origem, mensagem);
        }
    }

    private void registrarHistorico(
            Pagamento pagamento,
            StatusPagamento anterior,
            StatusPagamento novo,
            OrigemAlteracaoStatus origem,
            String mensagem) {
        HistoricoStatusPagamento historico = HistoricoStatusPagamento.builder()
                .pagamento(pagamento)
                .statusAnterior(anterior)
                .statusNovo(novo)
                .origem(origem)
                .mensagem(mensagem)
                .build();
        pagamento.getHistoricoStatus().add(historico);
    }

    private Cliente carregarClienteAutorizado(Long clienteIdInformado) {
        Long clienteId = autorizacaoService.resolverClienteId(clienteIdInformado);
        if (clienteId == null) {
            throw new BusinessException("O cliente é obrigatório");
        }
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com id: " + clienteId));
    }

    private Site carregarSiteOpcional(Long siteId) {
        if (siteId == null) {
            return null;
        }
        autorizacaoService.validarAcessoSite(siteId);
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site não encontrado com id: " + siteId));
    }

    private Pagamento carregarPagamentoAutorizado(Long id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com id: " + id));
        autorizacaoService.validarAcessoCliente(pagamento.getCliente().getId());
        return pagamento;
    }

    private Cliente resolverClientePorCustomer(String asaasCustomerId) {
        if (!StringUtils.hasText(asaasCustomerId)) {
            return null;
        }
        return clienteRepository.findByAsaasCustomerId(asaasCustomerId).orElse(null);
    }

    private void validarCartao(PagamentoCartaoRequest request) {
        boolean temToken = StringUtils.hasText(request.getCreditCardToken());
        boolean temCartao = request.getCreditCard() != null && request.getCreditCardHolderInfo() != null;
        if (!temToken && !temCartao) {
            throw new BusinessException("Informe credit_card_token ou os dados do cartão e do titular");
        }
    }

    private void validarCancelamento(Pagamento pagamento) {
        if (pagamento.getStatus() == StatusPagamento.RECEIVED
                || pagamento.getStatus() == StatusPagamento.CONFIRMED
                || pagamento.getStatus() == StatusPagamento.REFUNDED
                || pagamento.getStatus() == StatusPagamento.CANCELLED
                || pagamento.getStatus() == StatusPagamento.DELETED) {
            throw new BusinessException("Pagamento não pode ser cancelado no status atual: " + pagamento.getStatus());
        }
        if (!StringUtils.hasText(pagamento.getAsaasPaymentId())) {
            throw new BusinessException("Pagamento sem identificador no Asaas");
        }
    }

    private void validarEstorno(Pagamento pagamento) {
        if (pagamento.getStatus() != StatusPagamento.RECEIVED && pagamento.getStatus() != StatusPagamento.CONFIRMED) {
            throw new BusinessException("Somente pagamentos recebidos/confirmados podem ser estornados");
        }
        if (!StringUtils.hasText(pagamento.getAsaasPaymentId())) {
            throw new BusinessException("Pagamento sem identificador no Asaas");
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
