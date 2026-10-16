package com.froes.assembly.integration;

public class ExternalVoterNotFoundException extends RuntimeException {
    public ExternalVoterNotFoundException() {
        super("O serviço externo de elegibilidade não encontrou este CPF");
    }
}
