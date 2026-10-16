package com.froes.assembly.util.validator;

public class CPFValidator {
    private CPFValidator() {
    }

    public static boolean isValidCPF(String cpf) {
        String normalizedCpf = normalize(cpf);
        if (normalizedCpf == null) {
            return false;
        }

        if (normalizedCpf.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = calculateCheckDigit(normalizedCpf, 9, 10);
        int secondDigit = calculateCheckDigit(normalizedCpf, 10, 11);

        return normalizedCpf.charAt(9) - '0' == firstDigit
                && normalizedCpf.charAt(10) - '0' == secondDigit;
    }

    public static String normalize(String cpf) {
        if (cpf == null) {
            return null;
        }

        String digits = cpf.replaceAll("[.\\-\\s]", "");
        if (!digits.matches("\\d+")
                || (digits.length() != 10 && digits.length() != 11)) {
            return null;
        }

        return String.format("%011d", Long.parseLong(digits));
    }

    private static int calculateCheckDigit(String cpf, int length, int initialWeight) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (cpf.charAt(index) - '0') * (initialWeight - index);
        }

        int digit = 11 - (sum % 11);
        if (digit >= 10) {
            return 0;
        }
        return digit;
    }
}
