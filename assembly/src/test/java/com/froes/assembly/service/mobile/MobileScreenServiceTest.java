package com.froes.assembly.service.mobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.froes.assembly.dto.SurveyDTO;
import com.froes.assembly.service.survey.SurveyService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MobileScreenServiceTest {

    @Test
    void buildsGetResultUrlForEachSurveySelection() {
        SurveyService surveyService = mock(SurveyService.class);
        when(surveyService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(java.util.List.of(
                new SurveyDTO(17L, null, "Pauta de teste", null, null, 2L))));
        MobileScreenService service = new MobileScreenService(
                surveyService,
                new ObjectMapper(),
                new DefaultResourceLoader(),
                "classpath:sdui/mobile-screens.json",
                100,
                RestClient.builder().build(),
                "sdui/mobile-screens",
                "",
                false);

        JsonNode selectionItem = new ObjectMapper().valueToTree(service.getScreens().get(3).itens().get(0));

        assertEquals("Pauta de teste", selectionItem.get("texto").asText());
        assertEquals("/api/v1/results/17", selectionItem.get("url").asText());
        assertFalse(selectionItem.has("body"));
    }

    @Test
    void appliesValidConsulUpdatesWithoutRestartingAndKeepsLastGoodConfiguration() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer consul = MockRestServiceServer.bindTo(restClientBuilder).build();
        SurveyService surveyService = mock(SurveyService.class);
        when(surveyService.findAll(any(Pageable.class))).thenReturn(org.springframework.data.domain.Page.empty());

        MobileScreenService service = new MobileScreenService(
                surveyService,
                new ObjectMapper(),
                new DefaultResourceLoader(),
                "classpath:sdui/mobile-screens.json",
                100,
                restClientBuilder.baseUrl("http://consul:8500").build(),
                "sdui/mobile-screens",
                "",
                true);

        consul.expect(requestTo("http://consul:8500/v1/kv/sdui/mobile-screens?raw"))
                .andRespond(withSuccess(
                        "{\"tipo\":\"FORM1\",\"titulo\":\"Criar pauta 1\",\"itens\":["
                                + "{\"tipo\":\"TEXTO 1\",\"texto\":\"Informe a descrição da pauta. 1\"},"
                                + "{\"tipo\":\"INPUT_TEXTO 1\",\"id\":\"question 1\","
                                + "\"titulo\":\"Descrição da pauta 1\",\"valor\":\"\"}]}",
                        APPLICATION_JSON));
        service.refreshConfiguration();
        assertEquals("Criar pauta 1", service.getScreens().get(0).titulo());
        assertEquals("FORM1", service.getScreens().get(0).tipo());
        JsonNode dynamicTextItem = new ObjectMapper().valueToTree(service.getScreens().get(0).itens().get(0));
        assertEquals("TEXTO 1", dynamicTextItem.get("tipo").asText());
        assertEquals("Informe a descrição da pauta. 1", dynamicTextItem.get("texto").asText());
        JsonNode dynamicInputItem = new ObjectMapper().valueToTree(service.getScreens().get(0).itens().get(1));
        assertEquals("INPUT_TEXTO 1", dynamicInputItem.get("tipo").asText());
        assertEquals("question 1", dynamicInputItem.get("id").asText());
        assertEquals("Descrição da pauta 1", dynamicInputItem.get("titulo").asText());
        consul.verify();
        consul.reset();

        consul.expect(requestTo("http://consul:8500/v1/kv/sdui/mobile-screens?raw"))
                .andRespond(withSuccess(
                        "[{\"tipo\":\"FORMULARIO\",\"titulo\":\"Tela atualizada\",\"itens\":[]}]",
                        APPLICATION_JSON));
        service.refreshConfiguration();
        assertEquals("Tela atualizada", service.getScreens().get(0).titulo());
        consul.verify();
        consul.reset();

        consul.expect(requestTo("http://consul:8500/v1/kv/sdui/mobile-screens?raw"))
                .andRespond(withSuccess(
                        "{\"tipo\":\"FORMULARIO\",\"titulo\":\"Configuração inválida\","
                                + "\"itens\":[{\"tipo\":\"TEXTO\"}]}",
                        APPLICATION_JSON));
        service.refreshConfiguration();
        assertEquals("Tela atualizada", service.getScreens().get(0).titulo());
        consul.verify();
    }
}
