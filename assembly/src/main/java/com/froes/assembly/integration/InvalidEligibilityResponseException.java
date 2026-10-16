package com.froes.assembly.integration;

public class InvalidEligibilityResponseException extends RuntimeException {
    public InvalidEligibilityResponseException() {
        super("O serviço externo de elegibilidade retornou uma resposta inválida");
    }
}
