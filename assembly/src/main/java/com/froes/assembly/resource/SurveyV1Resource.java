package com.froes.assembly.resource;

import com.froes.assembly.dto.AgendaRequestDTO;
import com.froes.assembly.dto.SurveyDTO;
import com.froes.assembly.dto.SurveyUpdateDTO;
import com.froes.assembly.dto.VotingSessionDTO;
import com.froes.assembly.dto.VotingSessionRequestDTO;
import com.froes.assembly.service.session.VotingSessionService;
import com.froes.assembly.service.survey.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
@Tag(name = "Pautas", description = "Cadastro e gerenciamento de pautas e sessões de votação.")
public class SurveyV1Resource {

    private final SurveyService surveyService;
    private final VotingSessionService votingSessionService;

    @PostMapping
    @ResponseStatus(CREATED)
    @Operation(summary = "Cadastrar pauta",
            description = "Cria uma pauta com status `NOT_STARTED`. Para habilitar a votação, abra uma sessão pelo endpoint de sessões.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Pauta cadastrada"),
                    @ApiResponse(responseCode = "400", description = "Descrição inválida"),
                    @ApiResponse(responseCode = "409", description = "Já existe uma pauta cadastrada com essa descrição")
            })
    public SurveyDTO create(@Valid @RequestBody AgendaRequestDTO request) {
        return surveyService.createAgenda(request);
    }

    @GetMapping
    @Operation(summary = "Listar pautas",
            description = "Retorna pautas em páginas. Aceita os parâmetros `page`, `size` e `sort` para controlar paginação e ordenação.",
            responses = @ApiResponse(responseCode = "200", description = "Página de pautas"))
    public Page<SurveyDTO> index(@ParameterObject Pageable pageable) {
        return surveyService.findAll(pageable);
    }

    @GetMapping("/{surveyId:\\d+}")
    @Operation(summary = "Consultar pauta",
            description = "Busca os dados de uma pauta pelo identificador.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pauta encontrada"),
                    @ApiResponse(responseCode = "404", description = "Pauta não encontrada")
            })
    public SurveyDTO findById(@Parameter(description = "Identificador da pauta.") @PathVariable Long surveyId) {
        return surveyService.findDTOById(surveyId);
    }

    @PutMapping("/{surveyId:\\d+}")
    @Operation(summary = "Atualizar pauta",
            description = "Atualiza somente a descrição (`question`) e a duração (`minutes`) de uma pauta com status `NOT_STARTED`. O identificador é informado na URL. Pautas abertas ou encerradas não podem ser alteradas.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pauta atualizada"),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
                    @ApiResponse(responseCode = "409", description = "Pauta não está em status NOT_STARTED ou já existe outra pauta com essa descrição"),
                    @ApiResponse(responseCode = "404", description = "Pauta não encontrada")
            })
    public SurveyDTO update(
            @Parameter(description = "Identificador da pauta a atualizar.")
            @PathVariable Long surveyId,
            @Valid @RequestBody SurveyUpdateDTO updateDTO) {
        return surveyService.update(surveyId, updateDTO);
    }

    @PostMapping("/{surveyId:\\d+}/sessions")
    @ResponseStatus(CREATED)
    @Operation(summary = "Abrir sessão de votação",
            description = "Abre a primeira sessão da pauta. A duração pode ser informada em minutos; se omitida, é usado o valor padrão configurado. Uma pauta com sessão aberta ou já encerrada não pode ser reaberta.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Sessão aberta"),
                    @ApiResponse(responseCode = "400", description = "Duração inválida"),
                    @ApiResponse(responseCode = "404", description = "Pauta não encontrada"),
                    @ApiResponse(responseCode = "409", description = "Pauta já possui sessão aberta ou encerrada")
            })
    public VotingSessionDTO openSession(
                                        @Parameter(description = "Identificador da pauta para a qual a sessão será aberta.")
                                        @PathVariable Long surveyId,
                                        @Valid @RequestBody(required = false)
                                        VotingSessionRequestDTO request) {
        return votingSessionService.open(surveyId, request);
    }

}
