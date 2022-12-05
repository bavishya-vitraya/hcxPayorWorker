package hcxPayor.hcxpayorworker.service;

import hcxPayor.hcxpayorworker.dto.Message;
import hcxPayor.hcxpayorworker.dto.VhiPreAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;



public interface ListenerService {
   boolean generateVhiRequest(Message msg);
     void generateHcxResponse (Message msg) throws Exception;

    VhiPreAuthRequest buildVhiPreAuthRequest(String request) throws Exception;
}
