package hcxPayor.hcxpayorworker.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreAuthVhiResponse {

    private String claimNumber;
    private AdjudicationClaimStatus claimStatus;
    private AdjudicationStatus adjudicationStatus;
    private String claimStatusInString;
    private ClaimAdjudicationData adjudicationData;
    private String query;
    private List<FileDTO> files;
    private BigDecimal approvedAmount;

    public enum AdjudicationStatus {
        AGREE,
        MODIFY_ADMISSIBILITY,
        MODIFY_AMOUNT,
        NA
    }


    public enum AdjudicationClaimStatus {
        PRE_AUTH_APPROVED("NA"),
        PRE_AUTH_REJECTED("NA"),
        SETTLEMENT_APPROVED("NA"),
        SETTLEMENT_REJECTED("NA"),
        PRE_AUTH_QUERY("Query Initiated"),
        SETTLEMENT_QUERY("Query Initiated"),
        APPROVED("Approved"),
        REJECTED("Rejected"),
        QUERY("Query Initiated"),
        DENIAL_OF_CASHLESS("Cashless Denied"),
        ENHANCEMENT_APPROVED("Enhancement Approved"),
        ENHANCEMENT_REJECTED("Enhancement Rejected"),
        ENHANCEMENT_DOWNSIZED("Enhancement Downsized"),
        ENHANCEMENT_WITHDRAWN("Enhancement Withdrawn"),
        WITHDRAWN_CLAIM_REJECTED("Withdrawn & Claim Rejected");


        private final String status;

        AdjudicationClaimStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}
