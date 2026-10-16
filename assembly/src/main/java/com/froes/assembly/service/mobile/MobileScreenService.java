package com.froes.assembly.service.mobile;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.froes.assembly.dto.MobileActionDTO;
import com.froes.assembly.dto.MobileFieldDTO;
import com.froes.assembly.dto.MobileScreenDTO;
import com.froes.assembly.dto.MobileScreenItemDTO;
import com.froes.assembly.dto.MobileSelectionItemDTO;
import com.froes.assembly.service.survey.SurveyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.data.domain.Sort.Direction.DESC;

@Slf4j
@Service
public class MobileScreenService {

    private final SurveyService surveyService;
    private final ObjectMapper objectMapper;
    private final ObjectMapper configurationMapper;
    private final RestClient consulRestClient;
    private final String consulKey;
    private final String consulToken;
    private final boolean consulEnabled;
    private final AtomicReference<List<MobileScreenDefinition>> screenDefinitions;
    private final AtomicBoolean consulFailureLogged = new AtomicBoolean();
    private volatile String lastConsulConfig;
    private final int surveyLimit;

    public MobileScreenService(
            SurveyService surveyService,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${app.mobile.screens-location:classpath:sdui/mobile-screens.json}") String screensLocation,
            @Value("${app.mobile.survey-limit:100}") int surveyLimit,
            @Qualifier("consulRestClient") RestClient consulRestClient,
            @Value("${app.mobile.consul.key:sdui/mobile-screens}") String consulKey,
            @Value("${app.mobile.consul.token:}") String consulToken,
            @Value("${app.mobile.consul.enabled:false}") boolean consulEnabled) {
        this.surveyService = surveyService;
        this.objectMapper = objectMapper;
        this.configurationMapper = objectMapper.copy()
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature());
        this.consulRestClient = consulRestClient;
        this.consulKey = consulKey;
        this.consulToken = consulToken;
        this.consulEnabled = consulEnabled;
        this.surveyLimit = surveyLimit;

        Resource screensResource = resourceLoader.getResource(screensLocation);
        try (InputStream inputStream = screensResource.getInputStream()) {
            List<MobileScreenDefinition> initialDefinitions = parseDefinitions(configurationMapper.readTree(inputStream));
            validateDefinitions(initialDefinitions);
            this.screenDefinitions = new AtomicReference<>(List.copyOf(initialDefinitions));
        } catch (IOException exception) {
            log.error("Não foi possível carregar a configuração de telas mobile em {}", screensLocation, exception);
            throw new IllegalStateException("Falha ao carregar a configuração de telas mobile: " + screensLocation,
                    exception);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadConsulConfigurationOnStartup() {
        if (consulEnabled) {
            refreshConfiguration();
        }
    }

    @Scheduled(fixedDelayString = "${app.mobile.consul.poll-interval-ms:5000}")
    public void refreshConfiguration() {
        if (!consulEnabled) {
            return;
        }

        try {
            String remoteConfig = consulRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v1/kv/")
                            .path(consulKey)
                            .queryParam("raw")
                            .build())
                    .headers(headers -> addConsulToken(headers))
                    .retrieve()
                    .body(String.class);

            if (remoteConfig == null || remoteConfig.isBlank() || remoteConfig.equals(lastConsulConfig)) {
                return;
            }

            List<MobileScreenDefinition> updatedDefinitions =
                    parseDefinitions(configurationMapper.readTree(remoteConfig));
            validateDefinitions(updatedDefinitions);
            screenDefinitions.set(List.copyOf(updatedDefinitions));
            lastConsulConfig = remoteConfig;
            if (consulFailureLogged.getAndSet(false)) {
                log.info("Conexão com Consul restabelecida; configuração SDUI atualizada");
            }
            log.info("Configuração SDUI atualizada pelo Consul KV, chave {}", consulKey);
        } catch (RestClientException | IOException | IllegalArgumentException exception) {
            if (consulFailureLogged.compareAndSet(false, true)) {
                log.warn("Não foi possível atualizar a configuração SDUI pela chave {} do Consul; "
                        + "a última configuração válida será mantida: {}", consulKey, exception.getMessage());
            }
        }
    }

    private List<MobileScreenDefinition> parseDefinitions(JsonNode root) throws IOException {
        if (root.isArray()) {
            List<MobileScreenDefinition> definitions = new ArrayList<>();
            for (JsonNode screen : root) {
                definitions.add(objectMapper.treeToValue(screen, MobileScreenDefinition.class));
            }
            return definitions;
        }
        if (root.isObject()) {
            return List.of(objectMapper.treeToValue(root, MobileScreenDefinition.class));
        }
        throw new IllegalArgumentException("A configuração SDUI deve ser um objeto de tela ou uma lista de telas");
    }

    public List<MobileScreenDTO> getScreens() {
        return screenDefinitions.get().stream()
                .map(this::buildScreen)
                .toList();
    }

    private void addConsulToken(HttpHeaders headers) {
        if (consulToken != null && !consulToken.isBlank()) {
            headers.set("X-Consul-Token", consulToken);
        }
    }

    private MobileScreenDTO buildScreen(MobileScreenDefinition definition) {
        List<MobileScreenItemDTO> items = "SELECAO".equals(definition.tipo())
                ? buildSelectionItems(definition)
                : buildFormItems(definition);
        return new MobileScreenDTO(
                definition.tipo(),
                definition.titulo(),
                items,
                definition.botaoOk(),
                definition.botaoCancelar());
    }

    private List<MobileScreenItemDTO> buildFormItems(MobileScreenDefinition definition) {
        if (definition.itens() == null) {
            return List.of();
        }
        return definition.itens().stream()
                .map(item -> new MobileFieldDTO(
                        item.tipo(),
                        item.texto(),
                        item.id(),
                        item.titulo(),
                        item.valor(),
                        item.additionalProperties()))
                .map(item -> (MobileScreenItemDTO) item)
                .toList();
    }

    private List<MobileScreenItemDTO> buildSelectionItems(MobileScreenDefinition definition) {
        List<MobileScreenItemDTO> items = new ArrayList<>();
        if (definition.itens() != null) {
            items.addAll(definition.itens().stream()
                    .map(item -> new MobileSelectionItemDTO(
                            item.texto(), item.url(), item.body(), item.additionalProperties()))
                    .toList());
        }

        if ("PAUTAS".equals(definition.fonteDados())) {
            if (surveyLimit < 1) {
                throw new IllegalStateException("app.mobile.survey-limit deve ser maior que zero");
            }
            String itemUrl = definition.urlItem();
            if (itemUrl == null || !itemUrl.contains("{surveyId}")) {
                throw new IllegalStateException(
                        "Telas de seleção de pautas devem definir urlItem com o parâmetro {surveyId}");
            }
            surveyService.findAll(PageRequest.of(0, surveyLimit, Sort.by(DESC, "id")))
                    .getContent()
                    .stream()
                    .map(survey -> new MobileSelectionItemDTO(
                            survey.question(),
                            itemUrl.replace("{surveyId}", survey.id().toString()),
                            null))
                    .forEach(items::add);
        } else if (definition.fonteDados() != null) {
            throw new IllegalStateException("Fonte de dados mobile não suportada: " + definition.fonteDados());
        }
        return List.copyOf(items);
    }

    private void validateDefinitions(List<MobileScreenDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("A configuração SDUI deve conter pelo menos uma tela");
        }

        for (MobileScreenDefinition definition : definitions) {
            if (definition == null
                    || definition.titulo() == null || definition.titulo().isBlank()
                    || definition.tipo() == null
                    || definition.tipo().isBlank()) {
                throw new IllegalArgumentException("Cada tela deve informar tipo e título");
            }
            if (definition.itens() == null) {
                throw new IllegalArgumentException("Cada tela deve informar a lista de itens");
            }
            if (definition.fonteDados() != null
                    && (!"SELECAO".equals(definition.tipo())
                    || !"PAUTAS".equals(definition.fonteDados())
                    || definition.urlItem() == null
                    || !definition.urlItem().contains("{surveyId}"))) {
                throw new IllegalArgumentException(
                        "Fontes dinâmicas devem ser PAUTAS em tela SELECAO e informar urlItem com {surveyId}");
            }
            for (MobileItemDefinition item : definition.itens()) {
                if (item == null) {
                    throw new IllegalArgumentException("Itens de tela não podem ser nulos");
                }
                if ("SELECAO".equals(definition.tipo())) {
                    if (item.texto() == null || item.url() == null || item.url().isBlank()) {
                        throw new IllegalArgumentException("Itens de seleção devem informar texto e URL");
                    }
                } else {
                    validateFormItem(item);
                }
            }
            validateAction(definition.botaoOk());
            validateAction(definition.botaoCancelar());
        }
    }

    private void validateFormItem(MobileItemDefinition item) {
        if (item.tipo() == null || item.tipo().isBlank()) {
            throw new IllegalArgumentException("Itens de formulário devem informar o tipo");
        }
        if ("TEXTO".equals(item.tipo())) {
            if (item.texto() == null) {
                throw new IllegalArgumentException("Itens TEXTO devem informar o texto");
            }
        } else if (List.of("INPUT_TEXTO", "INPUT_NUMERO", "INPUT_DATA", "SELECAO").contains(item.tipo())
                && (item.id() == null || item.id().isBlank() || item.titulo() == null)) {
            throw new IllegalArgumentException("Campos de formulário devem informar id e título");
        }
    }

    private void validateAction(MobileActionDTO action) {
        if (action != null && (action.texto() == null || action.texto().isBlank()
                || action.url() == null || action.url().isBlank())) {
            throw new IllegalArgumentException("Ações de tela devem informar texto e URL");
        }
    }

    private record MobileScreenDefinition(
            String tipo,
            String titulo,
            List<MobileItemDefinition> itens,
            MobileActionDTO botaoOk,
            MobileActionDTO botaoCancelar,
            String fonteDados,
            String urlItem) {
    }

    private static final class MobileItemDefinition {
        private String tipo;
        private String texto;
        private String id;
        private String titulo;
        private Object valor;
        private String url;
        private Map<String, Object> body;
        private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

        @com.fasterxml.jackson.annotation.JsonProperty("tipo")
        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("texto")
        public void setTexto(String texto) {
            this.texto = texto;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("id")
        public void setId(String id) {
            this.id = id;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("titulo")
        public void setTitulo(String titulo) {
            this.titulo = titulo;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("valor")
        public void setValor(Object valor) {
            this.valor = valor;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("url")
        public void setUrl(String url) {
            this.url = url;
        }

        @com.fasterxml.jackson.annotation.JsonProperty("body")
        public void setBody(Map<String, Object> body) {
            this.body = body;
        }

        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void addAdditionalProperty(String name, Object value) {
            additionalProperties.put(name, value);
        }

        public String tipo() {
            return tipo;
        }

        public String texto() {
            return texto;
        }

        public String id() {
            return id;
        }

        public String titulo() {
            return titulo;
        }

        public Object valor() {
            return valor;
        }

        public String url() {
            return url;
        }

        public Map<String, Object> body() {
            return body;
        }

        public Map<String, Object> additionalProperties() {
            return new LinkedHashMap<>(additionalProperties);
        }
    }
}
