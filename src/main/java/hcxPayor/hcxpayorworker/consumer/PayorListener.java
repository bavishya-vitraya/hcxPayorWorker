package hcxPayor.hcxpayorworker.consumer;

import hcxPayor.hcxpayorworker.dto.Message;
import hcxPayor.hcxpayorworker.model.PreAuthRequest;
import hcxPayor.hcxpayorworker.repository.PreAuthRequestRepo;
import hcxPayor.hcxpayorworker.service.ListenerService;
import hcxPayor.hcxpayorworker.service.impl.ListenerServiceImpl;
import hcxPayor.hcxpayorworker.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PayorListener {

    @Autowired
    private ListenerService listenerService;


    @RabbitListener(queues = "hcx_payor_request")
    public void recievedRequest(Message msg) throws Exception {
        log.info("msg {}", msg);

         boolean result = listenerService.generateVhiRequest(msg);
    }

    @RabbitListener(queues = Constants.RES_QUEUE)
    public void recievedResponse(Message msg) throws Exception {
        listenerService.generateHcxResponse(msg);
    }
}
