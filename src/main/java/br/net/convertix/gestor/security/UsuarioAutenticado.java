package br.net.convertix.gestor.security;

import br.net.convertix.gestor.enums.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioAutenticado {

    private Long id;
    private String email;
    private TipoUsuario tipo;
    private Long clienteId;

    public boolean isAdmin() {
        return tipo == TipoUsuario.ADMIN;
    }

    public boolean isCliente() {
        return tipo == TipoUsuario.CLIENTE;
    }
}
