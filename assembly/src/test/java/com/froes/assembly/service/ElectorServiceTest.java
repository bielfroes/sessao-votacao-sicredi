package com.froes.assembly.service;


import com.froes.assembly.dto.ElectorRequestDTO;
import com.froes.assembly.enums.ElectorStatusEnum;
import com.froes.assembly.helper.MessageHelper;
import com.froes.assembly.integration.VoterEligibilityClient;
import com.froes.assembly.repository.ElectorRepository;
import com.froes.assembly.service.elector.ElectorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static com.froes.assembly.util.creator.ElectorCreator.*;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(SpringExtension.class)
public class ElectorServiceTest {
    @InjectMocks
    private ElectorService service;
    @Mock
    private ElectorRepository repository;
    @Mock
    private MessageHelper messageHelper;
    @Mock
    private VoterEligibilityClient eligibilityClient;

    @Test
    void create_returnsElectorDTO_WhenSuccessful() {
        when(repository.saveAndFlush(any())).thenReturn(elector);
        assertEquals(electorDTO, service.create(createElectorRequestDTO()));
    }

    @Test
    void create_returnsEligibilityFromServerWithoutAcceptingClientStatus() {
        ReflectionTestUtils.setField(service, "externalEligibilityEnabled", true);
        when(eligibilityClient.checkEligibility("67953008033"))
                .thenReturn(ElectorStatusEnum.UNABLE_TO_VOTE);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(createElectorRequestDTO());

        assertEquals(ElectorStatusEnum.UNABLE_TO_VOTE, created.status());
        assertEquals("67953008033", created.cpf());
        verify(eligibilityClient).checkEligibility("67953008033");
    }

    @Test
    void create_returns_BAD_REQUEST_When_INVALID_CPF() {
        when(repository.saveAndFlush(any())).thenReturn(elector);
        assertEquals(BAD_REQUEST, assertThrows(ResponseStatusException.class, () ->
                service.create(new ElectorRequestDTO(INVALID_CPF))).getStatusCode());
    }
    @Test
    void create_returns_CONFLICT_When_CPF_ALREADY_USED() {
        when(repository.existsByCpf(any())).thenReturn(true);
        assertEquals(CONFLICT, assertThrows(ResponseStatusException.class, () ->
                service.create(createElectorRequestDTO())).getStatusCode());
    }

    @Test
    void statusForVotingRechecksExternalEligibilityWhenEnabled() {
        ReflectionTestUtils.setField(service, "externalEligibilityEnabled", true);
        when(eligibilityClient.checkEligibility("67953008033")).thenReturn(ElectorStatusEnum.UNABLE_TO_VOTE);

        assertEquals(ElectorStatusEnum.UNABLE_TO_VOTE, service.statusForVoting(elector));
        verify(eligibilityClient).checkEligibility("67953008033");
    }

    @Test
    void update_returnsElectorDTO_WhenSuccessful() {
        when(repository.findById(any())).thenReturn(Optional.of(elector));
        when(repository.saveAndFlush(any())).thenReturn(elector);
        assertEquals(electorDTO, service.update(electorUpdateDTO()));
    }

    @Test
    void update_returns_BAD_REQUEST_When_INVALID_CPF() {
        when(repository.findById(any())).thenReturn(Optional.of(elector));
        when(repository.saveAndFlush(any())).thenReturn(elector);
        assertEquals(BAD_REQUEST, assertThrows(ResponseStatusException.class, () ->
                service.update(electorUpdateDTO().withCpf(INVALID_CPF))).getStatusCode());
    }

    @Test
    void update_returns_CONFLICT_When_CPF_ALREADY_USED() {
        when(repository.findById(any())).thenReturn(Optional.of(elector));
        when(repository.existsByCpfAndIdNot(any(), any())).thenReturn(true);
        assertEquals(CONFLICT, assertThrows(ResponseStatusException.class, () ->
                service.update(electorUpdateDTO().withCpf("950.575.060-99"))).getStatusCode());
    }

    @Test
    void findById_throws404_whenElectorNotFound() {
        when(repository.findById(elector.getId())).thenReturn(Optional.empty());
        final var exception = assertThrows(ResponseStatusException.class, () ->
                service.findById(elector.getId())).getStatusCode();
        assertEquals(NOT_FOUND, exception);
    }

    @Test
    void findDtoById_whenSuccessful() {
        when(repository.findById(elector.getId())).thenReturn(Optional.of(elector));
        assertEquals(electorDTO, service.findDTOById(elector.getId()));
    }

    @Test
    void delete_removesEntity_whenSuccessful() {
        when(repository.findById(elector.getId())).thenReturn(Optional.of(elector));
        service.delete(elector.getId());
        verify(repository, times(1)).delete(elector);
    }

    @Test
    void findAll_returnsPageOfDTOs_whenSuccessful() {
        final var pageable = PageRequest.of(0, 10, Sort.by(ASC, "id"));
        final var assertion = new PageImpl<>(List.of(electorDTO));
        final var electors = new PageImpl<>(List.of(elector));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(electors);
        assertEquals(assertion, service.findAll(empty(),pageable));
    }

}
