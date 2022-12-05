package hcxPayor.hcxpayorworker.model;

import hcxPayor.hcxpayorworker.dto.PreAuthVhiResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "preAuthResponse")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PreAuthResponse {
    @Id
    private String responseId;
    private String senderCode;
    private String insurerCode;
    private String responseType;
    private PreAuthVhiResponse preAuthVhiResponse;
}
