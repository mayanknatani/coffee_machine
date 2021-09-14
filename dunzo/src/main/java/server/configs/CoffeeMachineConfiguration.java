package server.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * THIS IS SAMPLE CoffeeMachineConfig.
 * Please see description of CoffeeMachineConfig class to make sense of different fields here.
 */
@Configuration
public class CoffeeMachineConfiguration {
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
        int numOutlets = 5;
        return new CoffeeMachineConfig(containerConfigs,
                recipes,
                numOutlets);
    }
}
