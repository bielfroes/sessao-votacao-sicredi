package com.froes.assembly.resource;

import com.froes.assembly.dto.VoteDTO;
import com.froes.assembly.dto.VoteRequestDTO;
import com.froes.assembly.service.vote.RegisterVoteService;
import com.froes.assembly.service.vote.VoteQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
@Tag(name = "Votos", description = "Registro, consulta e apuração de votos.")
public class VoteResource {

    private final RegisterVoteService registerVoteService;
    private final VoteQueryService voteQueryService;

    @PostMapping
    @ResponseStatus(CREATED)
    @Operation(summary = "Registrar voto",
            description = "Registra um voto `SIM` ou `NAO` para um associado em uma pauta. Cada associado pode votar uma única vez por pauta e somente durante uma sessão aberta.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Voto registrado", content = @Content(schema = @Schema(implementation = VoteDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
                    @ApiResponse(responseCode = "404", description = "Pauta ou associado não encontrado"),
                    @ApiResponse(responseCode = "409", description = "Associado já votou ou sessão não está aberta")
            })
    public VoteDTO create(@Valid @RequestBody VoteRequestDTO requestDTO) {
        return registerVoteService.execute(requestDTO);
    }

    @GetMapping
    @Operation(summary = "Listar votos",
            description = "Retorna os votos em páginas. Aceita `page`, `size` e `sort` para controlar paginação e ordenação.",
            responses = @ApiResponse(responseCode = "200", description = "Página de votos"))
    public Page<VoteDTO> index(@ParameterObject Pageable pageable) {
        return voteQueryService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar voto",
            description = "Busca um voto pelo identificador.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Voto encontrado"),
                    @ApiResponse(responseCode = "404", description = "Voto não encontrado")
            })
    public VoteDTO findById(@Parameter(description = "Identificador do voto.") @PathVariable Long id) {
        return voteQueryService.findDTOById(id);
    }
}
