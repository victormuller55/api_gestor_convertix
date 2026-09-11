package br.net.convertix.gestor.service;

import br.net.convertix.gestor.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ArquivoService {

    private static final Set<String> TIPOS_IMAGEM = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> TIPOS_PDF = Set.of(
            "application/pdf"
    );

    private static final Map<String, String> EXTENSAO_POR_TIPO = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/pdf", ".pdf"
    );

    private static final String URL_PREFIX = "/uploads/";
    private static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private final Path diretorioBase;

    public ArquivoService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.diretorioBase = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(diretorioBase);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o diretório de uploads", e);
        }
    }

    public String salvar(MultipartFile arquivo, String subpasta) {
        return persistir(arquivo, subpasta, TIPOS_IMAGEM, "Formato de imagem não suportado. Use JPEG, PNG ou WebP");
    }

    public String salvarPdf(MultipartFile arquivo, String subpasta) {
        validarExtensaoPdf(arquivo);
        return persistir(arquivo, subpasta, TIPOS_PDF, "O documento de requisitos deve ser um arquivo PDF");
    }

    private String persistir(MultipartFile arquivo, String subpasta, Set<String> tiposPermitidos, String mensagemTipo) {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new BusinessException("Arquivo excede o tamanho máximo permitido de 5 MB");
        }

        String contentType = normalizarContentType(arquivo.getContentType());
        if (contentType == null || !tiposPermitidos.contains(contentType)) {
            throw new BusinessException(mensagemTipo);
        }

        String tipoDetectado = detectarTipoPorAssinatura(arquivo);
        if (tipoDetectado == null || !tipoDetectado.equals(contentType)) {
            throw new BusinessException("Conteúdo do arquivo não corresponde ao tipo informado");
        }

        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal != null && contemPathTraversal(nomeOriginal)) {
            throw new BusinessException("Nome de arquivo inválido");
        }

        String extensao = EXTENSAO_POR_TIPO.get(tipoDetectado);
        String nomeArquivo = UUID.randomUUID() + extensao;

        try {
            Path destino = diretorioBase.resolve(subpasta).normalize();
            if (!destino.startsWith(diretorioBase)) {
                throw new BusinessException("Caminho de upload inválido");
            }
            Files.createDirectories(destino);
            Path caminhoArquivo = destino.resolve(nomeArquivo).normalize();
            if (!caminhoArquivo.startsWith(diretorioBase)) {
                throw new BusinessException("Caminho de upload inválido");
            }
            arquivo.transferTo(caminhoArquivo);
            return URL_PREFIX + subpasta + "/" + nomeArquivo;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("Erro ao salvar arquivo em pasta controlada");
            throw new BusinessException("Erro ao salvar o arquivo");
        }
    }

    public void excluir(String urlPath) {
        if (urlPath == null || urlPath.isBlank() || !urlPath.startsWith(URL_PREFIX)) {
            return;
        }

        String caminhoRelativo = urlPath.substring(URL_PREFIX.length());
        Path caminhoArquivo = diretorioBase.resolve(caminhoRelativo).normalize();

        if (!caminhoArquivo.startsWith(diretorioBase)) {
            return;
        }

        try {
            Files.deleteIfExists(caminhoArquivo);
        } catch (IOException e) {
            log.warn("Não foi possível excluir arquivo de upload");
        }
    }

    private String normalizarContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private void validarExtensaoPdf(MultipartFile arquivo) {
        String nomeOriginal = arquivo == null ? null : arquivo.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            throw new BusinessException("O documento de requisitos deve ser um arquivo PDF");
        }
        String normalizado = nomeOriginal.replace('\\', '/');
        String nome = normalizado.substring(normalizado.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        if (!nome.endsWith(".pdf") || nome.equals(".pdf")) {
            throw new BusinessException("O documento de requisitos deve ser um arquivo PDF");
        }
    }

    private boolean contemPathTraversal(String nome) {
        String normalizado = nome.replace('\\', '/');
        return normalizado.contains("..") || normalizado.contains("/") || normalizado.startsWith(".");
    }

    private String detectarTipoPorAssinatura(MultipartFile arquivo) {
        try (InputStream in = arquivo.getInputStream()) {
            byte[] header = in.readNBytes(12);
            if (header.length < 3) {
                return null;
            }
            // JPEG: FF D8 FF
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if (header.length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A) {
                return "image/png";
            }
            // WEBP: RIFF....WEBP
            if (header.length >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P') {
                return "image/webp";
            }
            // PDF: %PDF
            if (header.length >= 4
                    && header[0] == '%'
                    && header[1] == 'P'
                    && header[2] == 'D'
                    && header[3] == 'F') {
                return "application/pdf";
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
