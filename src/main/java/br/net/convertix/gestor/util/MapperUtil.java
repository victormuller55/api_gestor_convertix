package br.net.convertix.gestor.util;

import br.net.convertix.gestor.dto.request.BioLinkItemRequest;
import br.net.convertix.gestor.dto.request.BioLinkRequest;
import br.net.convertix.gestor.dto.request.ClienteRequest;
import br.net.convertix.gestor.dto.request.LandingPageCampoRequest;
import br.net.convertix.gestor.dto.request.LandingPageFormularioRequest;
import br.net.convertix.gestor.dto.request.LandingPageRequest;
import br.net.convertix.gestor.dto.request.SiteDominioRequest;
import br.net.convertix.gestor.dto.request.SiteRequest;
import br.net.convertix.gestor.dto.request.UsuarioRequest;
import br.net.convertix.gestor.dto.response.BioLinkItemResponse;
import br.net.convertix.gestor.dto.response.BioLinkResponse;
import br.net.convertix.gestor.dto.response.ClienteResponse;
import br.net.convertix.gestor.dto.response.LandingPageCampoResponse;
import br.net.convertix.gestor.dto.response.LandingPageFormularioResponse;
import br.net.convertix.gestor.dto.response.LandingPageResponse;
import br.net.convertix.gestor.dto.response.LandingPageLeadResponse;
import br.net.convertix.gestor.dto.response.LoginResponse;
import br.net.convertix.gestor.dto.response.SiteDominioResponse;
import br.net.convertix.gestor.dto.response.SiteResponse;
import br.net.convertix.gestor.dto.response.UsuarioResponse;
import br.net.convertix.gestor.entity.BioLink;
import br.net.convertix.gestor.entity.BioLinkItem;
import br.net.convertix.gestor.entity.Cliente;
import br.net.convertix.gestor.entity.LandingPage;
import br.net.convertix.gestor.entity.LandingPageCampo;
import br.net.convertix.gestor.entity.LandingPageFormulario;
import br.net.convertix.gestor.entity.LandingPageLead;
import br.net.convertix.gestor.entity.LandingPageLeadValor;
import br.net.convertix.gestor.entity.Site;
import br.net.convertix.gestor.entity.SiteDominio;
import br.net.convertix.gestor.entity.Usuario;
import br.net.convertix.gestor.enums.TipoUsuario;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class MapperUtil {

    public UsuarioResponse toResponse(Usuario entity) {
        return UsuarioResponse.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .email(entity.getEmail())
                .ativo(entity.getAtivo())
                .tipo(entity.getTipo())
                .foto(entity.getFoto())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(Usuario entity, UsuarioRequest request) {
        entity.setNome(request.getNome());
        entity.setEmail(request.getEmail());
        entity.setAtivo(request.getAtivo());
    }

    public Usuario toAdminEntity(UsuarioRequest request) {
        return Usuario.builder()
                .nome(request.getNome())
                .email(request.getEmail())
                .senha(request.getSenha())
                .ativo(request.getAtivo())
                .tipo(TipoUsuario.ADMIN)
                .build();
    }

    public LoginResponse toLoginResponse(Usuario usuario, Cliente cliente, String token) {
        LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .tipo(usuario.getTipo())
                .ativo(usuario.getAtivo())
                .foto(usuario.getFoto())
                .token(token)
                .createdAt(usuario.getCreatedAt())
                .updatedAt(usuario.getUpdatedAt());

        if (cliente != null) {
            builder.clienteId(cliente.getId())
                    .nomeEmpresa(cliente.getNomeEmpresa())
                    .documento(cliente.getDocumento())
                    .telefone(cliente.getTelefone());
        }

        return builder.build();
    }

    public ClienteResponse toResponse(Cliente entity) {
        String foto = entity.getUsuario() != null ? entity.getUsuario().getFoto() : null;
        return ClienteResponse.builder()
                .id(entity.getId())
                .nomeEmpresa(entity.getNomeEmpresa())
                .documento(entity.getDocumento())
                .email(entity.getEmail())
                .telefone(entity.getTelefone())
                .foto(foto)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(Cliente entity, ClienteRequest request) {
        entity.setNomeEmpresa(request.getNomeEmpresa());
        entity.setDocumento(DocumentoUtil.normalizar(request.getDocumento()));
        entity.setEmail(request.getEmail());
        entity.setTelefone(request.getTelefone());
    }

    public Usuario toUsuarioFromCliente(ClienteRequest request) {
        return Usuario.builder()
                .nome(request.getNomeEmpresa())
                .email(request.getEmail())
                .senha(request.getSenha())
                .ativo(true)
                .tipo(TipoUsuario.CLIENTE)
                .build();
    }

    public void updateUsuarioFromCliente(Usuario usuario, ClienteRequest request) {
        usuario.setNome(request.getNomeEmpresa());
        usuario.setEmail(request.getEmail());
    }

    public Cliente toEntity(ClienteRequest request, Usuario usuario) {
        return Cliente.builder()
                .nomeEmpresa(request.getNomeEmpresa())
                .documento(DocumentoUtil.normalizar(request.getDocumento()))
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .usuario(usuario)
                .build();
    }

    public SiteResponse toResponse(Site entity) {
        return SiteResponse.builder()
                .id(entity.getId())
                .clienteId(entity.getCliente().getId())
                .clienteNomeEmpresa(entity.getCliente().getNomeEmpresa())
                .nome(entity.getNome())
                .tipo(entity.getTipo())
                .dominio(entity.getDominio())
                .subdominio(entity.getSubdominio())
                .status(entity.getStatus())
                .dominioInfo(toDominioResponse(entity.getSiteDominio()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SiteDominioResponse toDominioResponse(SiteDominio entity) {
        if (entity == null) {
            return null;
        }

        Integer duracaoDominio = null;
        if (entity.getDataFimDominio() != null) {
            duracaoDominio = (int) ChronoUnit.DAYS.between(LocalDate.now(), entity.getDataFimDominio());
        }

        return SiteDominioResponse.builder()
                .valorDominio(entity.getValorDominio())
                .duracaoDominio(duracaoDominio)
                .dataCompraDominio(entity.getDataCompraDominio())
                .dataFimDominio(entity.getDataFimDominio())
                .dataRenovacao(entity.getDataRenovacao())
                .build();
    }

    public SiteDominio toDominioEntity(SiteDominioRequest request, Site site) {
        LocalDate dataRenovacao = request.getDataRenovacao() != null
                ? request.getDataRenovacao()
                : request.getDataCompraDominio();

        return SiteDominio.builder()
                .site(site)
                .valorDominio(request.getValorDominio())
                .dataCompraDominio(request.getDataCompraDominio())
                .dataFimDominio(request.getDataFimDominio())
                .dataRenovacao(dataRenovacao)
                .build();
    }

    public void updateDominioEntity(SiteDominio entity, SiteDominioRequest request) {
        LocalDate dataRenovacao = request.getDataRenovacao() != null
                ? request.getDataRenovacao()
                : request.getDataCompraDominio();

        entity.setValorDominio(request.getValorDominio());
        entity.setDataCompraDominio(request.getDataCompraDominio());
        entity.setDataFimDominio(request.getDataFimDominio());
        entity.setDataRenovacao(dataRenovacao);
    }

    public void updateEntity(Site entity, SiteRequest request) {
        entity.setNome(request.getNome());
        entity.setTipo(request.getTipo());
        entity.setDominio(request.getDominio());
        entity.setSubdominio(request.getSubdominio());
        entity.setStatus(request.getStatus());
    }

    public BioLinkResponse toResponse(BioLink entity) {
        return BioLinkResponse.builder()
                .id(entity.getId())
                .siteId(entity.getSite().getId())
                .siteNome(entity.getSite().getNome())
                .nomeUsuario(entity.getNomeUsuario())
                .descricao(entity.getDescricao())
                .fotoPerfil(entity.getFotoPerfil())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(BioLink entity, BioLinkRequest request) {
        entity.setNomeUsuario(request.getNomeUsuario());
        entity.setDescricao(request.getDescricao());
    }

    public BioLinkItemResponse toResponse(BioLinkItem entity) {
        return BioLinkItemResponse.builder()
                .id(entity.getId())
                .biolinkId(entity.getBioLink().getId())
                .titulo(entity.getTitulo())
                .url(entity.getUrl())
                .icone(entity.getIcone())
                .ordem(entity.getOrdem())
                .ativo(entity.getAtivo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(BioLinkItem entity, BioLinkItemRequest request) {
        entity.setTitulo(request.getTitulo());
        entity.setUrl(request.getUrl());
        entity.setIcone(request.getIcone());
        entity.setOrdem(request.getOrdem());
        entity.setAtivo(request.getAtivo());
    }

    public BioLinkItem toEntity(BioLinkItemRequest request, BioLink bioLink) {
        return BioLinkItem.builder()
                .bioLink(bioLink)
                .titulo(request.getTitulo())
                .url(request.getUrl())
                .icone(request.getIcone())
                .ordem(request.getOrdem())
                .ativo(request.getAtivo())
                .build();
    }

    public LandingPageResponse toResponse(LandingPage entity) {
        return LandingPageResponse.builder()
                .id(entity.getId())
                .siteId(entity.getSite().getId())
                .siteNome(entity.getSite().getNome())
                .slug(entity.getSlug())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public LandingPageFormularioResponse toResponse(LandingPageFormulario entity) {
        return LandingPageFormularioResponse.builder()
                .id(entity.getId())
                .landingPageId(entity.getLandingPage().getId())
                .nome(entity.getNome())
                .titulo(entity.getTitulo())
                .descricao(entity.getDescricao())
                .textoBotao(entity.getTextoBotao())
                .ativo(entity.getAtivo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(LandingPageFormulario entity, LandingPageFormularioRequest request) {
        entity.setNome(request.getNome());
        entity.setTitulo(request.getTitulo());
        entity.setDescricao(request.getDescricao());
        entity.setTextoBotao(request.getTextoBotao());
        entity.setAtivo(request.getAtivo());
    }

    public LandingPageFormulario toEntity(LandingPageFormularioRequest request, LandingPage landingPage) {
        return LandingPageFormulario.builder()
                .landingPage(landingPage)
                .nome(request.getNome())
                .titulo(request.getTitulo())
                .descricao(request.getDescricao())
                .textoBotao(request.getTextoBotao())
                .ativo(request.getAtivo())
                .build();
    }

    public LandingPageCampoResponse toResponse(LandingPageCampo entity) {
        return LandingPageCampoResponse.builder()
                .id(entity.getId())
                .formularioId(entity.getFormulario().getId())
                .nomeInterno(entity.getNomeInterno())
                .label(entity.getLabel())
                .tipo(entity.getTipo())
                .placeholder(entity.getPlaceholder())
                .obrigatorio(entity.getObrigatorio())
                .ordem(entity.getOrdem())
                .ativo(entity.getAtivo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(LandingPageCampo entity, LandingPageCampoRequest request) {
        entity.setNomeInterno(request.getNomeInterno());
        entity.setLabel(request.getLabel());
        entity.setTipo(request.getTipo());
        entity.setPlaceholder(request.getPlaceholder());
        entity.setObrigatorio(request.getObrigatorio());
        entity.setOrdem(request.getOrdem());
        entity.setAtivo(request.getAtivo());
    }

    public LandingPageCampo toEntity(LandingPageCampoRequest request, LandingPageFormulario formulario) {
        return LandingPageCampo.builder()
                .formulario(formulario)
                .nomeInterno(request.getNomeInterno())
                .label(request.getLabel())
                .tipo(request.getTipo())
                .placeholder(request.getPlaceholder())
                .obrigatorio(request.getObrigatorio())
                .ordem(request.getOrdem())
                .ativo(request.getAtivo())
                .build();
    }

    public LandingPageLeadResponse toResponse(LandingPageLead entity, List<LandingPageLeadValor> valores) {
        Map<String, String> respostas = new LinkedHashMap<>();
        for (LandingPageLeadValor valor : valores) {
            respostas.put(valor.getCampo().getNomeInterno(), valor.getValor());
        }

        return LandingPageLeadResponse.builder()
                .id(entity.getId())
                .landingPageId(entity.getLandingPage().getId())
                .formularioId(entity.getFormulario().getId())
                .nome(entity.getNome())
                .email(entity.getEmail())
                .telefone(entity.getTelefone())
                .ip(entity.getIp())
                .origem(entity.getOrigem())
                .userAgent(entity.getUserAgent())
                .status(entity.getStatus())
                .observacao(entity.getObservacao())
                .respostas(respostas)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
