package com.froes.assembly.event.consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class MessengerConsumerService {

    private final ConnectionFactory connectionFactory;
    private final Set<String> activeQueues = new HashSet<>();
    private final List<Channel> channels = new ArrayList<>();
    private Connection connection;

    public MessengerConsumerService(
            @Value("${spring.rabbitmq.host:localhost}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password) {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
    }

    public synchronized void directConsumer() {
        subscribe("Queue-1");
        subscribe("Aprovado");
        subscribe("Reprovado");
        subscribe("Empate");
    }

    private void subscribe(String queueName) {
        if (activeQueues.contains(queueName)) {
            return;
        }
        try {
            if (connection == null || !connection.isOpen()) {
                connection = connectionFactory.newConnection();
            }
            Channel channel = connection.createChannel();
            channel.basicConsume(queueName, true, (consumerTag, delivery) ->
                    log.info("Recebidos {} bytes da fila {}", delivery.getBody().length, queueName),
                    consumerTag -> { });
            channels.add(channel);
            activeQueues.add(queueName);
        } catch (IOException | TimeoutException exception) {
            throw new IllegalStateException("Could not subscribe to RabbitMQ queue " + queueName, exception);
        }
    }

    @PreDestroy
    synchronized void close() {
        for (Channel channel : channels) {
            try {
                channel.close();
            } catch (IOException | TimeoutException exception) {
                log.warn("Não foi possível fechar um canal consumidor do RabbitMQ");
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException exception) {
                log.warn("Não foi possível fechar a conexão consumidora do RabbitMQ");
            }
        }
    }
}
