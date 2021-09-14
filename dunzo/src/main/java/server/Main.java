package server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class Main {
    // boot the spring boot applications and load all beans.
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
