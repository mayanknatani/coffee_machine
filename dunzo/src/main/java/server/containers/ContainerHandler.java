package server.containers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import server.database.models.Container;
import server.database.repositories.ContainerRepository;
import server.exceptions.IngredientNotAvailableException;
import server.exceptions.IngredientNotFoundException;
import server.exceptions.InvalidCapacityException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Container handler is a class responsible to handle the container for a particular ingredient.
 * Container handlers are created through ContainerHandlerFactory and used through it only.
 * To make sure that ContainerHandlers are only created through ContainerHandlerFactory the access to constructor is made package private.
 * All operations done on a ContainerHandler are done through a single threaded executor to make sure at a time only one thread is responsible for maintaining the quantity of a particular ingredient.
 *
 * Please see:: ContainerHandlerFactory
 */
@Slf4j
public class ContainerHandler {
    private String ingredientName;
    private ExecutorService executor;
    private ContainerRepository containerRepository;

    ContainerHandler(String ingredientName, ContainerRepository containerRepository) {
        this.ingredientName = ingredientName;
        this.containerRepository = containerRepository;
        // setting threadName as ingredient name for better debugging
        executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(ingredientName).build());
    }

    public CompletableFuture<Container> subtract(int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Reserving {}, quantity {}", ingredientName, quantity);
            Container container = containerRepository.findById(ingredientName).orElseThrow(() ->
                    new IngredientNotFoundException("ingredient " + ingredientName + " not found"));
            int currentLevel = container.getCurrentLevel();
            if (currentLevel < quantity) {
                log.error("Could not reserve {}, quantity {}", ingredientName, quantity);
                throw new IngredientNotAvailableException("Ingredient not available",
                        ingredientName, quantity, currentLevel);
            }
            log.info("Reserved {}, quantity {}", ingredientName, quantity);
            container.setCurrentLevel(currentLevel - quantity);
            containerRepository.save(container);
            return container;
        }, executor);
    }

    public CompletableFuture<Container> add(int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Adding {}, quantity {}", ingredientName, quantity);
            Container container = containerRepository.findById(ingredientName).orElseThrow(() ->
                    new IngredientNotFoundException("ingredient " + ingredientName + " not found"));
            if (container.getCurrentLevel() + quantity > container.getCapacity()) {
                log.error("Capacity overloaded for ingredient {}, capacity {}",
                        ingredientName, container.getCapacity());
                throw new InvalidCapacityException("Current container can not hold more than "
                        + container.getCapacity());
            }
            log.info("Added {}, quantity {}", ingredientName, quantity);
            container.setCurrentLevel(container.getCurrentLevel() + quantity);
            containerRepository.save(container);
            return container;
        }, executor);
    }
}
