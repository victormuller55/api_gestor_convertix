package br.net.convertix.gestor.integration.asaas;

import br.net.convertix.gestor.exception.BusinessException;

public class AsaasException extends BusinessException {

    public AsaasException(String message) {
        super(message);
    }

    public AsaasException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
