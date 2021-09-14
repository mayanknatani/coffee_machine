package server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.configs.CoffeeMachineConfig;
import server.configs.Ingredient;
import server.configs.Recipe;
import server.containers.ContainerHandlerFactory;
import server.database.models.Container;
import server.database.repositories.ContainerRepository;
import server.exceptions.IngredientNotAvailableException;
import server.exceptions.InvalidOutletException;
import server.requests.DispenseBeverageRequest;
import server.requests.RefillIngredientRequest;
import server.responses.DispenseBeverageResponse;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CoffeeMachine is the class responsible for orchestrating following requests:
 * 1. To dispense beverage from multiple outlets.
 * 2. To refill any ingredient.
 * <p>
 * CoffeeMachine is initiated through initial CoffeeMachineConfig.
 * <p>
 * Please See:
 * CoffeeMachineConfig's description.
 * ContainerHandlerFactory's description.
 */
@Service
@Slf4j
public class CoffeeMachine {
    @Autowired
    private CoffeeMachineConfig coffeeMachineConfig;
    @Autowired
    private ContainerHandlerFactory containerHandlerFactory;
    @Autowired
    private ContainerRepository containerRepository;
    @Value("${brewTime}")
    private Integer BREW_TIME;

    private List<ExecutorService> outlets = new ArrayList<>();

    @PostConstruct
    private void setup() {
        log.info("Setting up outlet executors");
        for (int i = 0; i < coffeeMachineConfig.getOutLets(); i++) {
            // make a named executor for each thread to ease debugging.
            outlets.add(Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder().setNameFormat("outlet-" + (i + 1)).build())
            );
        }

        log.info("creating containers if it doesn't exist");
        coffeeMachineConfig.getContainerConfigs()
                .forEach(containerConfig -> {
                    if (!containerRepository.findById(containerConfig.getIngredientName()).isPresent()) {
                        Container container = new Container(containerConfig.getIngredientName(),
                                containerConfig.getCapacity(),
                                0);
                        containerRepository.save(container);
                    }
                });
    }

    /**
     * @param request a request to dispense a beverage can come from any outlet and can come from multiple outlets at once as well.
     * @return returns as future which will complete when either the beverage is successfully dispensed or \
     * beverage request couldn't be fulfilled as there were not enough ingredients.
     * The response will optionally have an error message in case it is not able to dispense.
     * This message can be used to display actions for users in case some ingredients are running low.
     */
    public CompletableFuture<DispenseBeverageResponse> dispense(DispenseBeverageRequest request) {
        String recipeName = request.getRecipeName();
        Integer outletNumber = request.getOutletNumber();

        if (outletNumber > coffeeMachineConfig.getOutLets() || outletNumber <= 0) {
            log.error("Invalid outlet number {}", outletNumber);
            throw new InvalidOutletException("invalid outlet number " + outletNumber +
                    ", total outlets " + coffeeMachineConfig.getOutLets());
        }

        return CompletableFuture.runAsync(() ->
                brew(recipeName), outlets.get(outletNumber - 1))
                .thenApply(__ -> {
                    log.info("All ingredients successfully reserved and " +
                            "beverage {} successfully dispensed from outlet {}", recipeName, outletNumber);
                    return new DispenseBeverageResponse(true);
                })
                .exceptionally(throwable -> {
                    String errorMsg = String.format("Couldn't successfully dispense beverage %s, error %s",
                            recipeName, throwable.getMessage());
                    log.error(errorMsg);
                    if (throwable.getCause().getClass() == IngredientNotAvailableException.class) {
                        // gracefully reject request as Ingredient is not available.
                        return new DispenseBeverageResponse(false, errorMsg);
                    }
                    throw new CompletionException(throwable.getCause());
                });
    }


    /**
     * @param request refill an ingredient.
     */
    public void refill(RefillIngredientRequest request) {
        containerHandlerFactory.getHandler(request.getIngredientName())
                .add(request.getQuantity()).join();
    }


    /**
     * @param recipeName reserve ingredients for a particular beverage.
     *                   If it is not able to successfully reserve the ingredients then it will throw IngredientNotAvailableException.
     */
    private void reserveIngredients(String recipeName) {
        log.info("Started reserving ingredients for {}", recipeName);
        Recipe recipe = coffeeMachineConfig.getRecipe(recipeName);

        // Try to reserve all required ingredients in parallel.
        CompletableFuture[] getAllIngredients = recipe.getIngredients()
                .stream()
                .map(ingredient -> containerHandlerFactory.getHandler(ingredient.getName())
                        .subtract(ingredient.getQuantity()))
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(getAllIngredients).join();
        } catch (Exception ex) {
            // return the reserved ingredients back because some of the ingredients were not available for the recipe.
            // Beverage can't be served so no-point wasting the ingredients.
            for (int i = 0; i < getAllIngredients.length; i++) {
                if (getAllIngredients[i].isCompletedExceptionally()) continue;
                Ingredient ingredient = recipe.getIngredients().get(i);
                containerHandlerFactory.getHandler(ingredient.getName())
                        .add(ingredient.getQuantity()).join();
            }
        }

        // should throw if any of the parallel calls to reserve the required ingredients failed.
        CompletableFuture.allOf(getAllIngredients).join();
    }


    /**
     * @param recipeName Reserve the ingredients and brew the beverage.
     *                   It will throw IngredientNotAvailableException if it is not able to serve the beverage due to unavailability of ingredients.
     */
    private void brew(String recipeName) {
        reserveIngredients(recipeName);
        log.info("Brewing Started {}", recipeName);
        try {
            Thread.sleep(BREW_TIME);
        } catch (InterruptedException e) {
            log.error("Unexpected Error in brewing ", e);
        }
        log.info("Brewing Completed {}", recipeName);
    }
}
