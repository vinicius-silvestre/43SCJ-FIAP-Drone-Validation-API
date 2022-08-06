package com.br.dronetrack.schedule;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.utils.SerializationUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.br.dronetrack.config.RabbitConfig;
import com.br.dronetrack.model.DroneDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableScheduling
public class AmqpConsumer {

	@Scheduled(fixedDelay = 6000)
	public void schedule() {
		RabbitTemplate template = new RabbitTemplate(RabbitConfig.getConnection());
		try {
			byte[] body = template.receive("drone-tracker-receptor").getBody();
			Object resp = SerializationUtils.deserialize(body);
			ObjectMapper mapper = new ObjectMapper();
	    	DroneDTO drone = mapper.convertValue(resp, DroneDTO.class); 
	    	if(verifyIfShouldSendAlert(drone)) {
	    		sendMessageQueue(drone);
	    	}
			System.out.println(resp);
		}catch (NullPointerException ex){
            System.out.println("fila vazia!");            
        }
	}
	
	public boolean verifyIfShouldSendAlert(DroneDTO drone) {
		if((drone.getTemperatura() >= 35 || drone.getTemperatura() <= 0) || (drone.getUmidade() <= 15)) {
			return true;
		}
		return false;
	}
	
	public DroneDTO sendMessageQueue(DroneDTO drone){
   	 RabbitAdmin admin = new RabbitAdmin(RabbitConfig.getConnection());
        
   	 Queue droneQueue = new Queue("drone-tracker-alert");
        admin.declareQueue(droneQueue);
        
        final String exchange = "exchange.drone";
        DirectExchange exchangeDrone = new DirectExchange(exchange);
        admin.declareExchange(exchangeDrone);
        
        admin.declareBinding(BindingBuilder.bind(droneQueue).to(exchangeDrone).with("drone-alert-key"));
        
        RabbitTemplate template = new RabbitTemplate(RabbitConfig.getConnection());
        template.convertAndSend(exchange, "drone-alert-key", drone);
       return drone;
   }
}
