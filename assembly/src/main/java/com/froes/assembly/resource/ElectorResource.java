package com.froes.assembly.resource;


import com.froes.assembly.dto.ElectorDTO;
import com.froes.assembly.dto.ElectorRequestDTO;
import com.froes.assembly.service.elector.ElectorService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/electors")
@RequiredArgsConstructor
@Tag(name = "Associados", description = "Cadastro e consulta de associados aptos a participar das votações.")
public class ElectorResource {

    private final ElectorService service;

    @PostMapping
    @ResponseStatus(CREATED)
    @Operation(summary = "Cadastrar associado",
            description = "Valida o CPF, consulta a elegibilidade quando configurada e cria o associado.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Associado cadastrado"),
                    @ApiResponse(responseCode = "400", description = "CPF ausente ou inválido"),
                    @ApiResponse(responseCode = "409", description = "CPF já cadastrado"),
                    @ApiResponse(responseCode = "503", description = "Serviço externo de elegibilidade indisponível")
            })
    public ElectorDTO create(@Valid @RequestBody ElectorRequestDTO requestDTO) {
        return service.create(requestDTO);
    }

    @GetMapping
    @Operation(summary = "Listar associados",
            description = "Retorna associados em páginas. Aceita filtro opcional por CPF e parâmetros de paginação `page`, `size` e `sort`.",
            responses = @ApiResponse(responseCode = "200", description = "Página de associados"))
    public Page<ElectorDTO> index(
            @Parameter(description = "Filtra pelo CPF do associado.")
            @RequestParam(required = false) String cpf,
            @ParameterObject Pageable pageable) {
        return service.findAll(Optional.ofNullable(cpf), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar associado",
            description = "Busca os dados de um associado pelo identificador.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Associado encontrado"),
                    @ApiResponse(responseCode = "404", description = "Associado não encontrado")
            })
    public ElectorDTO findById(@Parameter(description = "Identificador do associado.") @PathVariable Long id) {
        return service.findDTOById(id);
    }

}
