package server.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Each recipe has a name and can be prepared by a list of ingredients.
 * Eg: hot_tea [name] can be prepared from ingredients (hot_water (200 ml), hot_mill (200 ml), sugar_syrup(50 ml))
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Recipe {
    private String name;
    private List<Ingredient> ingredients;
}
