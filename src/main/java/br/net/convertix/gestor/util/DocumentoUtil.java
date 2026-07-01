package br.net.convertix.gestor.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DocumentoUtil {

    public String normalizar(String documento) {
        if (documento == null) {
            return null;
        }
        return documento.replaceAll("\\D", "");
    }

    public boolean validar(String documento) {
        String digits = normalizar(documento);

        if (digits == null) {
            return false;
        }

        return switch (digits.length()) {
            case 11 -> validarCpf(digits);
            case 14 -> validarCnpj(digits);
            default -> false;
        };
    }

    private boolean validarCpf(String cpf) {
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        return calcularDigitoCpf(cpf, 9) == Character.getNumericValue(cpf.charAt(9))
                && calcularDigitoCpf(cpf, 10) == Character.getNumericValue(cpf.charAt(10));
    }

    private int calcularDigitoCpf(String cpf, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (posicao + 1 - i);
        }

        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private boolean validarCnpj(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }

        return calcularDigitoCnpj(cnpj, 12) == Character.getNumericValue(cnpj.charAt(12))
                && calcularDigitoCnpj(cnpj, 13) == Character.getNumericValue(cnpj.charAt(13));
    }

    private int calcularDigitoCnpj(String cnpj, int posicao) {
        int[] pesos = posicao == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += Character.getNumericValue(cnpj.charAt(i)) * pesos[i];
        }

        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
