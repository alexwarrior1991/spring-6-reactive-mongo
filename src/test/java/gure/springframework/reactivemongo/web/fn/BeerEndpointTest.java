package gure.springframework.reactivemongo.web.fn;

import static gure.springframework.reactivemongo.web.fn.BeerRouterConfig.BEER_PATH_PRICE_RANGE;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOAuth2Login;

import gure.springframework.reactivemongo.domain.Beer;
import gure.springframework.reactivemongo.model.BeerDTO;
import gure.springframework.reactivemongo.services.BeerService;
import gure.springframework.reactivemongo.services.BeerServiceImplTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
@AutoConfigureWebTestClient
class BeerEndpointTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void testPatchIdNotFound() {
        webTestClient
                .mutateWith(mockOAuth2Login())
                .patch()
                .uri(BeerRouterConfig.BEER_PATH_ID, 999)
                .body(Mono.just(BeerServiceImplTest.getTestBeer()), BeerDTO.class)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testPatchIdFound() {
        BeerDTO beerDTO = getSavedTestBeer();

        webTestClient
                .mutateWith(mockOAuth2Login())
                .patch()
                .uri(BeerRouterConfig.BEER_PATH_ID, beerDTO.getId())
                .body(Mono.just(beerDTO), BeerDTO.class)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void testDeleteNotFound() {
        webTestClient
                .mutateWith(mockOAuth2Login())
                .delete()
                .uri(BeerRouterConfig.BEER_PATH_ID, 999)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(999)
    void testDeleteBeer() {
        BeerDTO beerDTO = getSavedTestBeer();

        webTestClient
                .mutateWith(mockOAuth2Login())
                .delete()
                .uri(BeerRouterConfig.BEER_PATH_ID, beerDTO.getId())
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    @Order(4)
    void testUpdateBeerBadRequest() {
        BeerDTO testBeer = getSavedTestBeer();
        testBeer.setBeerStyle("");

        webTestClient
                .mutateWith(mockOAuth2Login())
                .put()
                .uri(BeerRouterConfig.BEER_PATH_ID, testBeer)
                .body(Mono.just(testBeer), BeerDTO.class)
                .exchange()
                .expectStatus().isBadRequest()
                // Consume the response body when the response status is BAD_REQUEST
                .expectBody(Map.class)
                .consumeWith(body -> {
                    // Print out the response body
                    System.out.println(body.getResponseBody());
                    // Or use logger:
                    // log.error(body.getResponseBody().toString());
                });
    }



    @Test
    void testUpdateBeerNotFound() {
        webTestClient
                .mutateWith(mockOAuth2Login())
                .put()
                .uri(BeerRouterConfig.BEER_PATH_ID, 999)
                .body(Mono.just(BeerServiceImplTest.getTestBeer()), BeerDTO.class)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(3)
    void testUpdateBeer() {

        BeerDTO beerDTO = getSavedTestBeer();
        beerDTO.setBeerName("New");

        webTestClient
                .mutateWith(mockOAuth2Login())
                .put()
                .uri(BeerRouterConfig.BEER_PATH_ID, beerDTO.getId())
                .body(Mono.just(beerDTO), BeerDTO.class)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void testCreateBeerBadData() {
        Beer testBeer = BeerServiceImplTest.getTestBeer();
        testBeer.setBeerName("");

        webTestClient
                .mutateWith(mockOAuth2Login())
                .post().uri(BeerRouterConfig.BEER_PATH)
                .body(Mono.just(testBeer), BeerDTO.class)
                .header("Content-Type", "application/json")
                .exchange()
                .expectStatus().isBadRequest()
                // Consume the response body when the response status is BAD_REQUEST
                .expectBody(Map.class)
                .consumeWith(body -> {
                    // Print out the response body
                    System.out.println(body.getResponseBody());
                    // Or use logger:
                    // log.error(body.getResponseBody().toString());
                });
    }

    @Test
    void testCreateBeer() {
        BeerDTO testDto = getSavedTestBeer();

        webTestClient
                .mutateWith(mockOAuth2Login())
                .post().uri(BeerRouterConfig.BEER_PATH)
                .body(Mono.just(testDto), BeerDTO.class)
                .header("Content-Type", "application/json")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("location");
    }

    @Test
    void testGetByIdNotFound() {
        webTestClient
                .mutateWith(mockOAuth2Login())
                .get().uri(BeerRouterConfig.BEER_PATH_ID, 999)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(1)
    void testGetById() {
        BeerDTO beerDTO = getSavedTestBeer();

        webTestClient
                .mutateWith(mockOAuth2Login())
                .get().uri(BeerRouterConfig.BEER_PATH_ID, beerDTO.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-type", "application/json")
                .expectBody(BeerDTO.class);
    }

    @Test
    @Order(2)
    void testListBeersByStyle() {
        final String BEER_STYLE = "TEST";
        BeerDTO testDto = getSavedTestBeer();
        testDto.setBeerStyle(BEER_STYLE);

        //create test data
        webTestClient
                .mutateWith(mockOAuth2Login())
                .post().uri(BeerRouterConfig.BEER_PATH)
                .body(Mono.just(testDto), BeerDTO.class)
                .header("Content-Type", "application/json")
                .exchange();

        webTestClient
                .mutateWith(mockOAuth2Login())
                .get().uri(UriComponentsBuilder
                        .fromPath(BeerRouterConfig.BEER_PATH)
                        .queryParam("beerStyle", BEER_STYLE).build().toUri()
                )
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-type", "application/json")
                .expectBody().jsonPath("$.size()").value(equalTo(1));
    }

    @Test
    @Order(2)
    void testListBeers() {
        webTestClient
                .mutateWith(mockOAuth2Login())
                .get().uri(BeerRouterConfig.BEER_PATH)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-type", "application/json")
                .expectBody()
                .jsonPath("$.size()").value(greaterThan(1))
                .jsonPath("$[*]")    // Extract the full list
                .value(beerList -> {
                    List<Map<String, Object>> beers = (List<Map<String, Object>>) beerList;
                    beers.forEach(beer -> System.out.println("Beer: " + beer));
                });
    }

    @Test
    @Order(2)
    void testGetBeersByPriceRange() {

        BigDecimal min = new BigDecimal(10);
        BigDecimal max = new BigDecimal(13);


        webTestClient
                .mutateWith(mockOAuth2Login())
                .get()
                .uri(uriBuilder ->
                        uriBuilder.path(BEER_PATH_PRICE_RANGE)
                                .queryParam("min", min.toString())
                                .queryParam("max", max.toString())
                                .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "application/json")
                .expectBody()
                .jsonPath("$.size()").value(greaterThan(1))
                .jsonPath("$[0].beerName").exists()
                .jsonPath("$[*]").value(beerList -> {
                    List<Map<String, Object>> beers = (List<Map<String, Object>>) beerList;
                    beers.forEach(beer -> System.out.println("Beer: " + beer));
                });;


    }

    public BeerDTO getSavedTestBeer() {
        FluxExchangeResult<BeerDTO> beerDTOFluxExchangeResult = webTestClient
                .mutateWith(mockOAuth2Login())
                .post().uri(BeerRouterConfig.BEER_PATH)
                .body(Mono.just(BeerServiceImplTest.getTestBeerDto()), BeerDTO.class)
                .header("Content-Type", "application/json")
                .exchange()
                .returnResult(BeerDTO.class);

        List<String> location = beerDTOFluxExchangeResult.getResponseHeaders().get("Location");

        return webTestClient
                .mutateWith(mockOAuth2Login())
                .get().uri(BeerRouterConfig.BEER_PATH)
                .exchange().returnResult(BeerDTO.class).getResponseBody().blockFirst();
    }

}