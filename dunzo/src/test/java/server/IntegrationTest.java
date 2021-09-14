package server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import server.configs.CoffeeMachineConfig;
import server.configs.ContainerConfig;
import server.configs.Ingredient;
import server.configs.Recipe;
import server.database.models.Container;
import server.database.repositories.ContainerRepository;
import server.exceptions.IngredientNotFoundException;
import server.exceptions.InvalidCapacityException;
import server.exceptions.InvalidOutletException;
import server.exceptions.RecipeNotFoundException;
import server.requests.DispenseBeverageRequest;
import server.requests.RefillIngredientRequest;
import server.responses.DispenseBeverageResponse;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IntegrationTest {
    @TestConfiguration
    public static class TestCoffeeMachineConfig {
        @Bean
        public CoffeeMachineConfig coffeeMachineConfig() {
            List<ContainerConfig> containerConfigs = Arrays.asList(
                    new ContainerConfig("HOT_MILK", 500),
                    new ContainerConfig("HOT_WATER", 500),
                    new ContainerConfig("SUGAR_SYRUP", 100),
                    new ContainerConfig("GINGER_SYRUP", 100),
                    new ContainerConfig("TEA_LEAVES_SYRUP", 100),
                    new ContainerConfig("GREEN_MIXTURE", 100)
            );
            List<Recipe> recipes = Arrays.asList(
                    new Recipe("HOT_TEA",
                            Arrays.asList(
                                    new Ingredient("HOT_WATER", 200),
                                    new Ingredient("HOT_MILK", 100),
                                    new Ingredient("GINGER_SYRUP", 10),
                                    new Ingredient("SUGAR_SYRUP", 10),
                                    new Ingredient("TEA_LEAVES_SYRUP", 30)
                            )),
                    new Recipe("HOT_MASALA_TEA",
                            Arrays.asList(
                                    new Ingredient("HOT_WATER", 100),
                                    new Ingredient("HOT_MILK", 400),
                                    new Ingredient("GINGER_SYRUP", 30),
                                    new Ingredient("SUGAR_SYRUP", 50),
                                    new Ingredient("TEA_LEAVES_SYRUP", 30)
                            )),
                    new Recipe("BLACK_TEA",
                            Arrays.asList(
                                    new Ingredient("HOT_WATER", 300),
                                    new Ingredient("GINGER_SYRUP", 30),
                                    new Ingredient("SUGAR_SYRUP", 50),
                                    new Ingredient("TEA_LEAVES_SYRUP", 30)
                            )),
                    new Recipe("GREEN_TEA",
                            Arrays.asList(
                                    new Ingredient("HOT_WATER", 100),
                                    new Ingredient("GINGER_SYRUP", 30),
                                    new Ingredient("SUGAR_SYRUP", 50),
                                    new Ingredient("GREEN_MIXTURE", 30)
                            ))
            );
            int numOutlets = 3;
            return new CoffeeMachineConfig(containerConfigs,
                    recipes,
                    numOutlets);

        }
    }

    @Autowired
    private ApplicationContext ctx;

    @Test
    public void testCoffeeMachine() {
        CoffeeMachine coffeeMachine = ctx.getBean(CoffeeMachine.class);
        CoffeeMachineConfig coffeeMachineConfig = ctx.getBean(CoffeeMachineConfig.class);
        ContainerRepository containerRepository = ctx.getBean(ContainerRepository.class);

        // fill up the machine
        Map<String, Ingredient> ingredients = new HashMap<>();

        ingredients.put("HOT_MILK", new Ingredient("HOT_MILK", 500));
        ingredients.put("HOT_WATER", new Ingredient("HOT_WATER", 500));
        ingredients.put("SUGAR_SYRUP", new Ingredient("SUGAR_SYRUP", 100));
        ingredients.put("GINGER_SYRUP", new Ingredient("GINGER_SYRUP", 100));
        ingredients.put("TEA_LEAVES_SYRUP", new Ingredient("TEA_LEAVES_SYRUP", 100));
        ingredients.put("GREEN_MIXTURE", new Ingredient("GREEN_MIXTURE", 100));

        for (Ingredient ingredient : ingredients.values()) {
            coffeeMachine.refill(new RefillIngredientRequest(ingredient.getName(), ingredient.getQuantity()));
        }

        List<DispenseBeverageRequest> dispenseBeverageRequests = Arrays.asList(
                new DispenseBeverageRequest("HOT_TEA", 1),
                new DispenseBeverageRequest("HOT_MASALA_TEA", 2),
                new DispenseBeverageRequest("BLACK_TEA", 3),
                new DispenseBeverageRequest("GREEN_TEA", 1)
        );


        // make parallel requests to dispense beverages from all of the available outlets.
        // Some of the drinks will be dispensed, others will fail due to unavailability of ingredients.
        List<CompletableFuture<DispenseBeverageResponse>> dispenseFutures = new ArrayList<>();
        for (DispenseBeverageRequest request : dispenseBeverageRequests) {
            dispenseFutures.add(coffeeMachine.dispense(request));
        }


        // wait till all of the dispense futures complete
        CompletableFuture.allOf(dispenseFutures.toArray(new CompletableFuture[0])).join();

        Map<String, Integer> consumed = new HashMap<>();
        // compute total consumption
        for (int i = 0; i < dispenseFutures.size(); i++) {
            if (dispenseFutures.get(i).join().getDispensed()) {
                // successfully dispensed the beverage
                Recipe recipe = coffeeMachineConfig.getRecipe(dispenseBeverageRequests.get(i).getRecipeName());
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (!consumed.containsKey(ingredient.getName())) consumed.put(ingredient.getName(), 0);
                    consumed.put(ingredient.getName(), consumed.get(ingredient.getName()) + ingredient.getQuantity());
                }
            }
        }

        // compute remaining availability
        List<Container> containers = containerRepository.findAll();
        Map<String, Integer> remaining = new HashMap<>();
        for (Container container : containers) {
            remaining.put(container.getIngredientName(), container.getCurrentLevel());
        }

        // assert that total remaining and consumed is equal to the total ingredients provided
        for (Ingredient ingredient : ingredients.values()) {
            assert ingredient.getQuantity() == (remaining.getOrDefault(ingredient.getName(), 0) +
                    consumed.getOrDefault(ingredient.getName(), 0));
        }
    }

    @Test
    public void testCoffeeMachine_InvalidOutletNumber() {
        CoffeeMachine coffeeMachine = ctx.getBean(CoffeeMachine.class);
        boolean isInvalidOutLet = false;
        try {
            coffeeMachine.dispense(new DispenseBeverageRequest("HOT_TEA", 0)).join();
        } catch (InvalidOutletException ex) {
            isInvalidOutLet = true;
        }
        assert isInvalidOutLet;
    }

    @Test
    public void testCoffeeMachine_InvalidCapacityException() {
        CoffeeMachine coffeeMachine = ctx.getBean(CoffeeMachine.class);
        boolean isInvalidCapacity = false;
        try {
            coffeeMachine.refill(new RefillIngredientRequest("HOT_WATER", 50000));
        } catch (CompletionException ex) {
            try {
                throw ex.getCause();
            } catch (InvalidCapacityException throwable) {
                isInvalidCapacity = true;
            } catch (Throwable throwable) {
                // do nothing
            }
        }
        assert isInvalidCapacity;
    }

    @Test
    public void testCoffeeMachine_RecipeNotFoundException() {
        CoffeeMachine coffeeMachine = ctx.getBean(CoffeeMachine.class);
        boolean recipeNotFound = false;
        try {
            coffeeMachine.dispense(new DispenseBeverageRequest("HOT_TEAZZ", 1)).join();
        } catch (CompletionException ex) {
            try {
                throw ex.getCause();
            } catch (RecipeNotFoundException throwable) {
                recipeNotFound = true;
            } catch (Throwable throwable) {
                // do nothing
            }
        }
        assert recipeNotFound;
    }

    @Test
    public void testCoffeeMachine_IngredientNotFoundException() {
        CoffeeMachine coffeeMachine = ctx.getBean(CoffeeMachine.class);
        boolean ingredientNotFound = false;
        try {
            coffeeMachine.refill(new RefillIngredientRequest("HOT_WATERZZ", 10));
        } catch (IngredientNotFoundException ex) {
            ingredientNotFound = true;
        }
        assert ingredientNotFound;
    }
}
