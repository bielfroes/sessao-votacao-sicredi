package com.froes.assembly.integration;

import com.froes.assembly.enums.ElectorStatusEnum;

public interface VoterEligibilityClient {
    ElectorStatusEnum checkEligibility(String cpf);
}
