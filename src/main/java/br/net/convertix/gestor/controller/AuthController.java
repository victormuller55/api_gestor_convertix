package br.net.convertix.gestor.controller;

import br.net.convertix.gestor.dto.request.LoginRequest;
import br.net.convertix.gestor.dto.request.RecuperarSenhaRequest;
import br.net.convertix.gestor.dto.request.RedefinirSenhaRequest;
import br.net.convertix.gestor.dto.request.VerificarCodigoRecuperacaoRequest;
import br.net.convertix.gestor.dto.response.LoginResponse;
import br.net.convertix.gestor.dto.response.RecuperarSenhaResponse;
import br.net.convertix.gestor.dto.response.RedefinirSenhaResponse;
import br.net.convertix.gestor.dto.response.VerificarCodigoRecuperacaoResponse;
import br.net.convertix.gestor.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Solicitar código de recuperação de senha")
    @PostMapping("/recuperar-senha")
    public ResponseEntity<RecuperarSenhaResponse> recuperarSenha(
            @Valid @RequestBody RecuperarSenhaRequest request) {
        return ResponseEntity.ok(authService.recuperarSenha(request));
    }

    @Operation(summary = "Verificar código de recuperação de senha")
    @PostMapping("/verificar-codigo")
    public ResponseEntity<VerificarCodigoRecuperacaoResponse> verificarCodigo(
            @Valid @RequestBody VerificarCodigoRecuperacaoRequest request) {
        return ResponseEntity.ok(authService.verificarCodigo(request));
    }

    @Operation(summary = "Redefinir senha com código de recuperação")
    @PostMapping("/redefinir-senha")
    public ResponseEntity<RedefinirSenhaResponse> redefinirSenha(
            @Valid @RequestBody RedefinirSenhaRequest request) {
        return ResponseEntity.ok(authService.redefinirSenha(request));
    }
}
