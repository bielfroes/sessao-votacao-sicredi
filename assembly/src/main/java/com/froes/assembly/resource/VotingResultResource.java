package com.froes.assembly.resource;

import com.froes.assembly.dto.ResultDTO;
import com.froes.assembly.service.result.VotingResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
@Tag(name = "Resultados", description = "Apuração consolidada das votações.")
public class VotingResultResource {

    private final VotingResultService votingResultService;

    @GetMapping
    @Operation(summary = "Listar resultados",
            description = "Retorna a apuração de todas as pautas, incluindo votos favoráveis, contrários, total, situação e resultado.",
            responses = @ApiResponse(responseCode = "200", description = "Lista de resultados"))
    public List<ResultDTO> index() {
        return votingResultService.findAll();
    }

    @GetMapping("/{surveyId}")
    @Operation(summary = "Consultar resultado por pauta",
            description = "Apura os votos de uma pauta específica e informa totais, situação e resultado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resultado da pauta"),
                    @ApiResponse(responseCode = "404", description = "Pauta não encontrada")
            })
    public ResultDTO findBySurveyId(
            @Parameter(description = "Identificador da pauta cuja votação será apurada.")
            @PathVariable Long surveyId) {
        return votingResultService.findBySurveyId(surveyId);
    }
}
