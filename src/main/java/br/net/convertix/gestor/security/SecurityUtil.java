package br.net.convertix.gestor.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class SecurityUtil {

    public UsuarioAutenticado getUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario;
        }

        throw new AccessDeniedException("Usuário não autenticado");
    }

    public void exigirAdmin() {
        if (!getUsuarioLogado().isAdmin()) {
            throw new AccessDeniedException("Acesso permitido apenas para administradores");
        }
    }

    public void validarAcessoCliente(Long clienteId) {
        UsuarioAutenticado usuario = getUsuarioLogado();

        if (usuario.isAdmin()) {
            return;
        }

        if (usuario.getClienteId() == null || !usuario.getClienteId().equals(clienteId)) {
            throw new AccessDeniedException("Acesso negado a este cliente");
        }
    }
}
