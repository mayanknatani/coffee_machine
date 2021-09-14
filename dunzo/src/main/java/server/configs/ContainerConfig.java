package server.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Every container of a coffee machine has an ingredient and maximum capacity.
 * Eg: Left transparent container is for milk and can have upto 2L (capacity) of milk.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContainerConfig {
    private String ingredientName;
    private int capacity;
}
