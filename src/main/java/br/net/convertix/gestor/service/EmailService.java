package br.net.convertix.gestor.service;

import br.net.convertix.gestor.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String COR_CABECALHO = "#0b1f14";
    private static final String COR_DESTAQUE = "#00c44a";
    private static final String LOGO_CID = "logoConvertix";
    private static final String LOGO_CLASSPATH = "mail/logo-convertix-branco.png";

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@convertix.net.br}")
    private String from;

    public void enviarCodigoRecuperacaoSenha(String destinatario, String nomeUsuario, String codigo) {
        try {
            enviar(
                    destinatario,
                    "Código de recuperação de senha — Convertix",
                    montarHtmlRecuperacaoSenha(nomeUsuario, codigo));
            log.info("E-mail de recuperação de senha enviado");
        } catch (Exception ex) {
            log.error("Falha ao enviar e-mail de recuperação de senha: {}", ex.getClass().getSimpleName());
            throw new BusinessException("Não foi possível enviar o e-mail de recuperação de senha");
        }
    }

    public void enviarAvisoSenhaAlterada(String destinatario, String nomeUsuario) {
        try {
            enviar(
                    destinatario,
                    "Senha alterada — Convertix Gestor",
                    montarHtmlSenhaAlterada(nomeUsuario));
            log.info("E-mail de aviso de senha alterada enviado");
        } catch (Exception ex) {
            log.error("Falha ao enviar e-mail de aviso de senha alterada: {}", ex.getClass().getSimpleName());
            throw new BusinessException("Não foi possível enviar o e-mail de aviso de senha alterada");
        }
    }

    @Async
    public void enviarConfirmacaoLead(String destinatario, String nomeUsuario) {
        if (!StringUtils.hasText(destinatario)) {
            return;
        }
        try {
            enviar(
                    destinatario,
                    "Recebemos a sua mensagem — Convertix",
                    montarHtmlConfirmacaoLead(nomeUsuario));
            log.info("E-mail de confirmação de lead enviado");
        } catch (Exception ex) {
            log.error("Falha ao enviar e-mail de confirmação de lead: {}", ex.getClass().getSimpleName());
        }
    }

    private void enviar(String destinatario, String assunto, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(destinatario);
        helper.setSubject(assunto);
        helper.setText(html, true);
        helper.addInline(LOGO_CID, new ClassPathResource(LOGO_CLASSPATH), "image/png");
        mailSender.send(message);
    }

    private String montarHtmlRecuperacaoSenha(String nomeUsuario, String codigo) {
        String corpo = """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.5;">%s</p>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#334155;">
                  Use o código abaixo para continuar a recuperação da senha da sua conta
                  no sistema Gestor da Convertix. O código é válido por <strong>2 horas</strong>.
                </p>
                <div style="text-align:center;margin:28px 0;">
                  <div style="display:inline-block;background:#f1f5f9;border:1px solid #e2e8f0;border-radius:10px;padding:16px 28px;font-size:32px;font-weight:700;letter-spacing:8px;color:%s;">
                    %s
                  </div>
                </div>
                <p style="margin:0;font-size:13px;line-height:1.6;color:#64748b;">
                  Se você não solicitou esta recuperação, ignore este e-mail.
                </p>
                """.formatted(saudacao(nomeUsuario), COR_DESTAQUE, escaparHtml(codigo));
        return envelopar("Recuperação de senha", corpo);
    }

    private String montarHtmlSenhaAlterada(String nomeUsuario) {
        String corpo = """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.5;">%s</p>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#334155;">
                  A senha da sua conta no sistema <strong>Gestor Convertix</strong> foi alterada com sucesso.
                </p>
                <p style="margin:0;font-size:13px;line-height:1.6;color:#64748b;">
                  Se você não reconhece esta alteração, entre em contato imediatamente com o suporte da Convertix
                  e solicite a recuperação da sua conta.
                </p>
                """.formatted(saudacao(nomeUsuario));
        return envelopar("Aviso de segurança", corpo);
    }

    private String montarHtmlConfirmacaoLead(String nomeUsuario) {
        String corpo = """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.5;">%s</p>
                <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#334155;">
                  Recebemos a sua mensagem. Já vamos entrar em contato para tirar todas as dúvidas
                  e ver um orçamento para você.
                </p>
                <p style="margin:0;font-size:13px;line-height:1.6;color:#64748b;">
                  Fique de olho no seu e-mail e no WhatsApp — em breve falamos com você.
                </p>
                """.formatted(saudacao(nomeUsuario));
        return envelopar("Mensagem recebida", corpo);
    }

    private String envelopar(String tituloCabecalho, String corpo) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f8;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border-radius:12px;overflow:hidden;">
                          <tr>
                            <td style="background:%s;padding:28px 28px 24px;" align="center">
                              <img src="cid:%s" alt="Convertix" width="200" style="display:block;width:200px;max-width:70%%;height:auto;border:0;outline:none;text-decoration:none;">
                              <div style="margin-top:14px;font-size:13px;color:rgba(255,255,255,0.75);letter-spacing:0.3px;">
                                %s
                              </div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 28px 24px;border-top:1px solid #e2e8f0;font-size:12px;color:#94a3b8;">
                              Este é um e-mail automático. Não responda.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(tituloCabecalho, COR_CABECALHO, LOGO_CID, tituloCabecalho, corpo);
    }

    private String saudacao(String nomeUsuario) {
        if (StringUtils.hasText(nomeUsuario)) {
            return "Olá, <strong>" + escaparHtml(nomeUsuario.trim()) + "</strong>,";
        }
        return "Olá,";
    }

    private String escaparHtml(String valor) {
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
