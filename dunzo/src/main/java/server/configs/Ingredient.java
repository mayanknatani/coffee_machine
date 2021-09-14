package server.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Each Ingredient has a name and quantity. A list of ingredients are used to make a recipe/beverage.
 */
@AllArgsConstructor
@Getter
@NoArgsConstructor
@Data
public class Ingredient {
    private String name;
    private int quantity;
}
