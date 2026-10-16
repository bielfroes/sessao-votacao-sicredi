package com.froes.assembly.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfiguration {

    @Bean
    Queue votingResultQueue() {
        return QueueBuilder.durable("Queue-1").build();
    }

    @Bean
    DirectExchange votingResultExchange() {
        return new DirectExchange("Direct-Exchange", true, false);
    }

    @Bean
    Queue approvedQueue() {
        return QueueBuilder.durable("Aprovado").build();
    }

    @Bean
    Queue rejectedQueue() {
        return QueueBuilder.durable("Reprovado").build();
    }

    @Bean
    Queue tiedQueue() {
        return QueueBuilder.durable("Empate").build();
    }

    @Bean
    Binding approvedBinding(@Qualifier("approvedQueue") Queue approvedQueue,
                            @Qualifier("votingResultExchange") DirectExchange votingResultExchange) {
        return BindingBuilder.bind(approvedQueue).to(votingResultExchange).with("Aprovado");
    }

    @Bean
    Binding rejectedBinding(@Qualifier("rejectedQueue") Queue rejectedQueue,
                            @Qualifier("votingResultExchange") DirectExchange votingResultExchange) {
        return BindingBuilder.bind(rejectedQueue).to(votingResultExchange).with("Reprovado");
    }

    @Bean
    Binding tiedBinding(@Qualifier("tiedQueue") Queue tiedQueue,
                        @Qualifier("votingResultExchange") DirectExchange votingResultExchange) {
        return BindingBuilder.bind(tiedQueue).to(votingResultExchange).with("Empate");
    }
}
