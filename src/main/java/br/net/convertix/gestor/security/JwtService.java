package br.net.convertix.gestor.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final ZoneId zoneId;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.timezone}") String timezone) {
        this.secretKey = Keys.hmacShaKeyFor(gerarChave(secret));
        this.zoneId = ZoneId.of(timezone);
    }

    public String gerarToken(UsuarioAutenticado usuario) {
        Date expiracao = Date.from(
                LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant());

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("tipo", usuario.getTipo().name())
                .claim("cliente_id", usuario.getClienteId())
                .issuedAt(new Date())
                .expiration(expiracao)
                .signWith(secretKey)
                .compact();
    }

    public UsuarioAutenticado extrairUsuario(String token) {
        var claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long clienteId = claims.get("cliente_id", Number.class) != null
                ? claims.get("cliente_id", Number.class).longValue()
                : null;

        return UsuarioAutenticado.builder()
                .id(Long.parseLong(claims.getSubject()))
                .email(claims.get("email", String.class))
                .tipo(br.net.convertix.gestor.enums.TipoUsuario.valueOf(claims.get("tipo", String.class)))
                .clienteId(clienteId)
                .build();
    }

    private byte[] gerarChave(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= 32) {
            return keyBytes;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Erro ao gerar chave JWT", e);
        }
    }
}
