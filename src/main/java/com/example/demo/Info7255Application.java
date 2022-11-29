package com.example.demo;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.example.service.IndexingService;

@SpringBootApplication(scanBasePackages = "com.example.api")
@ComponentScan({"com.example.api", "com.example.queuing", "com.example.service"})
public class Info7255Application {
	public static final String EXCHANGE = "INFO7255_EXCHANGE";
	public static final String QUEUE = "INFO7255_QUEUE";
	public static final String ROUTING_KEY = "INFO7255_KEY";
	
	@Bean
	public Queue queue()
	{
		return new Queue(QUEUE, false);
	}
	
	@Bean
	public TopicExchange exchange()
	{
		return new TopicExchange(EXCHANGE);
	}
	
	@Bean
	public Binding binding(Queue queue, TopicExchange exchange) 
	{
        return BindingBuilder.bind(queue).to(exchange).with(QUEUE);
	}
	
	@Bean
	MessageListenerAdapter listenerAdapter(IndexingService receiver) {
		return new MessageListenerAdapter(receiver, "receiveMessage");
	}

	@Bean
	SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
											 MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(QUEUE);
		container.setMessageListener(listenerAdapter);
		return container;
	}

	
	public static void main(String[] args) {
		SpringApplication.run(Info7255Application.class, args);
	}

}
