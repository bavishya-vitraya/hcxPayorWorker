package hcxPayor.hcxpayorworker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String referenceId;
    private String senderCode;
    private String insurerCode;
    private String messageType;
}
