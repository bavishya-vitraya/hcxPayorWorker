package hcxPayor.hcxpayorworker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResDTO {

    private String query;
    private List<FileDTO> files;

}
