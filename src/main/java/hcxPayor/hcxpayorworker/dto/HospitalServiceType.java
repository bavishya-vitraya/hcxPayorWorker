package hcxPayor.hcxpayorworker.dto;


import hcxPayor.hcxpayorworker.payorEnum.ServiceType;
import hcxPayor.hcxpayorworker.payorEnum.VitrayaRoomCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HospitalServiceType {
    private VitrayaRoomCategory vitrayaRoomCategory;
    private String roomType;
    private String insurerRoomType;
    private boolean singlePrivateAC;
    private BigDecimal roomTariffPerDay;
    private ServiceType serviceType;
}
