package server.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import server.database.models.Container;

public interface ContainerRepository extends JpaRepository<Container, String> {
}
