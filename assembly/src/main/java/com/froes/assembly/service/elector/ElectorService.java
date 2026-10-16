package com.froes.assembly.service.elector;

import com.froes.assembly.dto.ElectorDTO;
import com.froes.assembly.dto.ElectorRequestDTO;
import com.froes.assembly.dto.ElectorUpdateDTO;
import com.froes.assembly.entity.Elector;
import com.froes.assembly.enums.ElectorStatusEnum;
import com.froes.assembly.helper.MessageHelper;
import com.froes.assembly.integration.VoterEligibilityClient;
import com.froes.assembly.repository.ElectorRepository;
import com.froes.assembly.repository.spec.ElectorSpecification;
import com.froes.assembly.util.validator.CPFValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;


import static com.froes.assembly.exception.ErrorCodeEnum.*;
import static com.froes.assembly.util.database.DatabaseConstraintUtils.isUniqueViolation;
import static com.froes.assembly.util.mapper.MapperConstants.electorMapper;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElectorService {

    private final ElectorRepository repository;
    private final MessageHelper messageHelper;
    private final VoterEligibilityClient eligibilityClient;

    @Value("${app.eligibility.enabled:false}")
    private boolean externalEligibilityEnabled;

    public ElectorDTO create(final ElectorRequestDTO requestDTO) {

        if (!CPFValidator.isValidCPF(requestDTO.cpf())) {
            log.warn("Cadastro de eleitor rejeitado porque a validação do CPF falhou");
            throw new ResponseStatusException(BAD_REQUEST, messageHelper.get(ERROR_INVALID_CPF, requestDTO.cpf()));
        }

        String cpf = CPFValidator.normalize(requestDTO.cpf());

        if (repository.existsByCpf(cpf)) {
            throw duplicateCpf();
        }

        ElectorStatusEnum status = resolveStatus(cpf);

        try {
            Elector elector = electorMapper.buildElector(requestDTO);
            return electorMapper.buildElectorDTO(repository.saveAndFlush(
                    elector.withCpf(cpf).withStatus(status)));
        } catch (DataIntegrityViolationException exception) {
            if (!isUniqueViolation(exception)) {
                throw exception;
            }
            throw duplicateCpf();
        }
    }

    public ElectorDTO update(final ElectorUpdateDTO updateDTO) {
        if (!CPFValidator.isValidCPF(updateDTO.cpf())) {
            log.warn("Atualização de eleitor rejeitada porque a validação do CPF falhou");
            throw new ResponseStatusException(BAD_REQUEST, messageHelper.get(ERROR_INVALID_CPF, updateDTO.cpf()));
        }

        String cpf = CPFValidator.normalize(updateDTO.cpf());

        Elector elector = findById(updateDTO.id());

        if (!elector.getCpf().equals(cpf) && repository.existsByCpfAndIdNot(cpf, elector.getId())) {
            throw duplicateCpf();
        }

        ElectorStatusEnum electorStatus = resolveStatus(cpf, updateDTO.status());
        try {
            return electorMapper.buildElectorDTO(repository.saveAndFlush(elector.withCpf(cpf)
                    .withStatus(electorStatus)));
        } catch (DataIntegrityViolationException exception) {
            if (!isUniqueViolation(exception)) {
                throw exception;
            }
            throw duplicateCpf();
        }
    }

    public Elector findById(final Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error(messageHelper.get(ERROR_ELECTOR_NOT_FOUND, id.toString()));
            return new ResponseStatusException(NOT_FOUND, messageHelper.get(ERROR_ELECTOR_NOT_FOUND, id.toString()));
        });
    }

    public ElectorDTO findDTOById(final Long id) {
        Elector elector = findById(id);
        return electorMapper.buildElectorDTO(elector);
    }

    public void delete(final Long id) {
        Elector elector = findById(id);
        repository.delete(elector);
    }

    public Page<ElectorDTO> findAll(final Optional<String> cpf, final Pageable pageable) {
        return repository.findAll(ElectorSpecification.withCpf(cpf.orElse(null)), pageable)
                .map(electorMapper::buildElectorDTO);
    }

    public ElectorStatusEnum statusForVoting(Elector elector) {
        return externalEligibilityEnabled
                ? eligibilityClient.checkEligibility(CPFValidator.normalize(elector.getCpf()))
                : elector.getStatus();
    }

    private ElectorStatusEnum resolveStatus(String cpf) {
        if (externalEligibilityEnabled) {
            return eligibilityClient.checkEligibility(cpf);
        }
        return ElectorStatusEnum.ABLE_TO_VOTE;
    }

    private ElectorStatusEnum resolveStatus(String cpf, ElectorStatusEnum requestedStatus) {
        if (externalEligibilityEnabled) {
            return eligibilityClient.checkEligibility(cpf);
        }
        return isNull(requestedStatus) ? ElectorStatusEnum.ABLE_TO_VOTE : requestedStatus;
    }

    private ResponseStatusException duplicateCpf() {
        return new ResponseStatusException(CONFLICT, messageHelper.get(ERROR_CPF_ALREADY_USED_GENERIC));
    }

}
