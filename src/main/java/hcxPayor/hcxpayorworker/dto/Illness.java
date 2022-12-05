package hcxPayor.hcxpayorworker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Illness {
    private int illnessCategoryId;
    private String illnessName;
    private String defaultICDCode;
}
