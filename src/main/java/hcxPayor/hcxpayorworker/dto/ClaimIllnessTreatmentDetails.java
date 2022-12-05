package hcxPayor.hcxpayorworker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClaimIllnessTreatmentDetails {
    private long claimId;
    private String chronicIllnessDetails;
    private long procedureCorporateMappingId;
    private long procedureId;
    private String lineOfTreatmentDetails;
    private Integer leftImplant;
    private Integer rightImplant;
    private String dateOfDiagnosis;
    private String doctorsDetails;
    private ChronicIllnessDetailsJSON chronicIllnessDetailsJSON;
}
