package server.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DispenseBeverageResponse {
    private Boolean dispensed;
    private String errorMsg;

    public DispenseBeverageResponse(Boolean dispensed) {
        this.dispensed = dispensed;
    }
}
