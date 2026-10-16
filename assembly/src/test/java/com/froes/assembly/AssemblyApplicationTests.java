package com.froes.assembly;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AssemblyApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void versionedMobileAndAggregatedResultRoutesRespond() throws Exception {
		mockMvc.perform(post("/api/v1/surveys")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"question\":\"Pauta de teste\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/surveys")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"question\":\"Pauta de teste\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Já existe uma pauta cadastrada com essa descrição."));
		mockMvc.perform(get("/api/v1/mobile/screens"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$[0].tipo").value("FORMULARIO"))
				.andExpect(jsonPath("$[0].titulo").value("Criar pauta"))
				.andExpect(jsonPath("$[0].itens[0].tipo").value("TEXTO"))
				.andExpect(jsonPath("$[0].itens[0].texto").value("Informe a descrição da pauta."))
				.andExpect(jsonPath("$[0].itens[1].tipo").value("INPUT_TEXTO"))
				.andExpect(jsonPath("$[0].botaoOk.texto").value("Criar pauta"))
				.andExpect(jsonPath("$[0].botaoOk.url").value("/api/v1/surveys"))
				.andExpect(jsonPath("$[1].botaoOk.url").value("/api/v1/surveys/{surveyId}/sessions"))
				.andExpect(jsonPath("$[3].tipo").value("SELECAO"))
				.andExpect(jsonPath("$[3].itens").isArray())
				.andExpect(jsonPath("$[3].itens[0].url").value("/api/v1/results/1"))
				.andExpect(jsonPath("$[3].itens[0].body").doesNotExist());
		mockMvc.perform(post("/api/v1/surveys/1/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"minutes\":10}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.surveyId").value(1));
		mockMvc.perform(post("/api/v1/surveys/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"surveyId\":1,\"minutes\":10}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/results/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.survey.id").value(1));
		mockMvc.perform(get("/api/v1/results/999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void onlyVersionedPaginatedRoutesAreExposed() throws Exception {
		mockMvc.perform(get("/api/v1/electors").param("cpf", "67953008033"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
		mockMvc.perform(get("/api/v1/surveys").param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
		mockMvc.perform(get("/api/v1/votes").param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
		mockMvc.perform(get("/api/v1/results"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
		mockMvc.perform(get("/elector"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/survey"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/vote"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/result"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/surveys/index"))
				.andExpect(status().isNotFound());
		mockMvc.perform(put("/api/v1/surveys")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"question\":\"Updated survey\",\"minutes\":5}"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(put("/api/v1/surveys/999999")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"question\":\"Updated survey\",\"minutes\":5}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/votes/result")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"surveyId\":1}"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(put("/api/v1/electors")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(delete("/api/v1/electors/1"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(put("/api/v1/votes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(delete("/api/v1/votes/1"))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(delete("/api/v1/surveys/1"))
				.andExpect(status().isMethodNotAllowed());
	}

}
