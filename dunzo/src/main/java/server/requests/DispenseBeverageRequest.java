package server.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DispenseBeverageRequest {
    private String recipeName;
    private Integer outletNumber;
}
