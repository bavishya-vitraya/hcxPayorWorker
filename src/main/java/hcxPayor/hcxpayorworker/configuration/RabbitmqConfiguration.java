package hcxPayor.hcxpayorworker.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitmqConfiguration {
    @Value("${queue.req.name}")
    private String reqQueue;

    @Value("${queue.res.name}")
    private String resQueue;

    @Value("${queue.exchange.name}")
    private String exchange;

    @Value("${queue.reqrouting.key}")
    private String reqroutingKey;

    @Value("${queue.resrouting.key}")
    private String resroutingKey;

    // spring bean for rabbitmq queue
    @Bean
    public Queue reqQueue() {
        return new Queue(reqQueue);
    }

    // spring bean for queue (store json messages)
    @Bean
    public Queue resQueue() {
        return new Queue(resQueue);
    }

    // spring bean for rabbitmq exchange
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    // binding between queue and exchange using routing key
    @Bean
    public Binding reqBinding() {
        return BindingBuilder
                .bind(reqQueue())
                .to(exchange())
                .with(reqroutingKey);
    }

    // binding between json queue and exchange using routing key
    @Bean
    public Binding resBinding() {
        return BindingBuilder
                .bind(resQueue())
                .to(exchange())
                .with(resroutingKey);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

}
