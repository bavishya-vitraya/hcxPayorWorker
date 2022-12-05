package hcxPayor.hcxpayorworker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClaimAdjudicationData {
    private BigDecimal totalEstimatedAmount;
    private BigDecimal admissibleAmount;
    private BigDecimal approvedAmount;
    private BigDecimal coPayment;
    private BigDecimal deduction;
    private BigDecimal payableByInsurer;
    private String modificationReason;
    private List<Object> costBreakup;
}
