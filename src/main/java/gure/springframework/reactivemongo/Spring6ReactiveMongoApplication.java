package gure.springframework.reactivemongo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;

@EnableReactiveMongoAuditing
@SpringBootApplication
public class Spring6ReactiveMongoApplication {

    public static void main(String[] args) {
        SpringApplication.run(Spring6ReactiveMongoApplication.class, args);
    }

}
