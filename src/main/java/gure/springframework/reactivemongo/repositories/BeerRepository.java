package gure.springframework.reactivemongo.repositories;

import gure.springframework.reactivemongo.domain.Beer;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface BeerRepository extends ReactiveMongoRepository<Beer, String> {

    Mono<Beer> findFirstByBeerName(String beerName);

    Flux<Beer> findByBeerStyle(String beerStyle);

    Flux<Beer> findAllByPriceBetween(BigDecimal min, BigDecimal max);
}
