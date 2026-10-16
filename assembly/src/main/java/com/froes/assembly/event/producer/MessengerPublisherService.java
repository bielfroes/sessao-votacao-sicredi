package com.froes.assembly.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.froes.assembly.dto.ResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessengerPublisherService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void send(String message) {
        rabbitTemplate.convertAndSend("", "Queue-1", message);
    }

    public void directPublisher(ResultDTO result) {
        try {
            publishSerializedResult("legacy-result", result.result(),
                    objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar o resultado da votação", exception);
        }
    }

    public void publishSerializedResult(String messageId, String routingKey, String payload) {
        MessageProperties properties = jsonMessageProperties();
        properties.setMessageId(messageId);
        rabbitTemplate.send("Direct-Exchange", routingKey,
                new Message(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8), properties));
    }

    private MessageProperties jsonMessageProperties() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return properties;
    }
}
