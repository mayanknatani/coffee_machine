package server.containers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.configs.CoffeeMachineConfig;
import server.database.repositories.ContainerRepository;
import server.exceptions.IngredientNotFoundException;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * ContainerHandlerFactory class is responsible to create handlers for all the different containers that CoffeeMachine has.
 * ContainerHandlerFactory makes sure that there is "one and only one" handler per container.
 * Multiple threads can increase/decrease quantity of different-different containers in-parallel.
 * Every ContainerHandler makes sure that concurrent access to add or subtract quantity of that particular container are ThreadSafe.
 * <p>
 * Please See: ContainerHandler
 */
@Service
@Slf4j
public class ContainerHandlerFactory {
    private Map<String, ContainerHandler> handlers = new HashMap<>();
    @Autowired
    private CoffeeMachineConfig coffeeMachineConfig;
    @Autowired
    private ContainerRepository containerRepository;

    @PostConstruct
    public void setHandlers() {
        // create container handlers for all containerConfigs
        coffeeMachineConfig.getContainerConfigs()
                .forEach(container -> {
                    handlers.put(container.getIngredientName(),
                            new ContainerHandler(container.getIngredientName(), containerRepository));
                });
    }

    public ContainerHandler getHandler(String ingredientName) {
        if (!handlers.containsKey(ingredientName))
            throw new IngredientNotFoundException("No such ingredient " + ingredientName);
        return handlers.get(ingredientName);
    }
}
