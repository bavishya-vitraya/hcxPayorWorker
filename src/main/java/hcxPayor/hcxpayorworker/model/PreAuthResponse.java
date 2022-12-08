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
    private String id;
    private String senderCode;
    private String insurerCode;
    private String responseType;
    private PreAuthVhiResponse preAuthVhiResponse;
    private String correlationId;
    private String outputFhirResponse;
}
