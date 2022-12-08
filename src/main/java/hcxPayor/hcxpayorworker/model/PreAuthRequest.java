package hcxPayor.hcxpayorworker.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "hcx_preAuthRequests")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PreAuthRequest {
    @Id
    private String id;
    private String requestObject;
    private String fhirPayload;
    private String preAuthRequest;
    private String preAuthRequestId;
    private String correlationId;
}
