package server.database.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Container class is used to persist an ingredients quantity and a container's maximum capacity.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Container {
    @Id
    private String ingredientName;
    private int capacity;
    private int currentLevel;
}
