package com.froes.assembly.resource;

import com.froes.assembly.dto.MobileScreenDTO;
import com.froes.assembly.service.mobile.MobileScreenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
@Tag(name = "Aplicação móvel", description = "Definições JSON das telas usadas pelo cliente móvel.")
public class MobileScreenResource {

    private final MobileScreenService mobileScreenService;

    @GetMapping("/screens")
    @Operation(summary = "Obter telas da aplicação móvel",
            description = "Retorna as definições das telas para criar uma pauta, abrir uma sessão, registrar um voto e selecionar uma pauta. A tela de seleção inclui até 100 pautas mais recentes.",
            responses = @ApiResponse(responseCode = "200", description = "Definições das telas"))
    public List<MobileScreenDTO> screens() {
        return mobileScreenService.getScreens();
    }
}
