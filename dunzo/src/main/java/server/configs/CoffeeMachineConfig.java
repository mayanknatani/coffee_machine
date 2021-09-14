package server.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import server.exceptions.InvalidConfigException;
import server.exceptions.RecipeNotFoundException;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is used to initialize the coffee machine.
 * A coffee Machine has three components.
 *
 * 1. Containers : Container signifies a container in the coffee machine.
 *  A container will be filled by an ingredient and will have a maximum capacity.
 *
 * 2. Recipes : Recipes are the beverages that this coffee machine can prepare.
 *
 * 3. outLets : Signifies the number of outlets that the coffee machine is going to have.
 * Coffee machine can serve beverages through multiple outlets at once.
 *
 */

@Component
@Data
@AllArgsConstructor
@Slf4j
public class CoffeeMachineConfig {
    private List<ContainerConfig> containerConfigs;
    private List<Recipe> recipes;
    private int outLets;

    @PostConstruct
    private void validate() {
        log.info("Validating coffee machine config {}", this);
        // validate that the recipes can be served from ingredient containerConfigs
        for (Recipe recipe : recipes) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                boolean found = false;
                for (ContainerConfig container : containerConfigs) {
                    if (container.getIngredientName().equals(ingredient.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new InvalidConfigException("Ingredient : " + ingredient.getName() +
                            " can not be serviced from any container");
                }
            }
        }

        // validate that each recipe has a different name
        Set<String> recipeNameSet = recipes.stream().map(Recipe::getName).collect(Collectors.toSet());
        if (recipeNameSet.size() != recipes.size()) {
            throw new InvalidConfigException("Please use unique names for different recipes.");
        }

        // validate that each container has a different ingredient
        Set<String> ingredientNameSet =
                containerConfigs.stream().map(ContainerConfig::getIngredientName).collect(Collectors.toSet());
        if (ingredientNameSet.size() != containerConfigs.size()) {
            throw new InvalidConfigException("Please use unique ingredients for different containerConfigs.");
        }

        // validate that the number of outlets is not zero
        if (outLets <= 0) {
            throw new InvalidConfigException("Number of outlets should be positive.");
        }
    }

    public Recipe getRecipe(String recipeName) {
        return recipes.stream()
                .filter(recipe -> recipe.getName().equals(recipeName))
                .findFirst()
                .orElseThrow(() -> new RecipeNotFoundException("Recipe " + recipeName + " is not found"));
    }
}
