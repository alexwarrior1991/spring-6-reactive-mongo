package gure.springframework.reactivemongo.services;

import gure.springframework.reactivemongo.domain.Beer;
import gure.springframework.reactivemongo.model.BeerDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface BeerService {

    Mono<BeerDTO> findFirstByBeerName(String beerName);
    Flux<BeerDTO> findByBeerStyle(String beerStyle);
    Flux<BeerDTO> findAllByPriceBetween(BigDecimal min, BigDecimal max);
    Flux<BeerDTO> listBeers();
    Mono<BeerDTO> saveBeer(Mono<BeerDTO> beerDTO);
    Mono<BeerDTO> saveBeer(BeerDTO beerDTO);
    Mono<BeerDTO> getById(String beerId);
    Mono<BeerDTO> updateBeer(String beerId, BeerDTO beerDTO);
    Mono<BeerDTO> patchBeer(String beerId, BeerDTO beerDTO);
    Mono<Void> deleteBeerById(String beerId);
}
