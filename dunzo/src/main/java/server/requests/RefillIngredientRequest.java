package server.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefillIngredientRequest {
    private String ingredientName;
    private Integer quantity;
}
