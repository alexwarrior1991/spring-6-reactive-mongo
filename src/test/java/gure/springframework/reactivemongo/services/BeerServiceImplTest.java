package gure.springframework.reactivemongo.services;

import gure.springframework.reactivemongo.domain.Beer;
import gure.springframework.reactivemongo.mappers.BeerMapper;
import gure.springframework.reactivemongo.mappers.BeerMapperImpl;
import gure.springframework.reactivemongo.model.BeerDTO;
import gure.springframework.reactivemongo.repositories.BeerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class BeerServiceImplTest {

    @Autowired
    BeerService beerService;

    @Autowired
    BeerMapper beerMapper;

    @Autowired
    BeerRepository beerRepository;

    BeerDTO beerDTO;

    @BeforeEach
    void setUp() {
        beerDTO = beerMapper.beerToBeerDto(getTestBeer());
    }

    @Test
    void testFindAllByPriceBetween() {

        // Given
        BigDecimal min = new BigDecimal("10.00");
        BigDecimal max = new BigDecimal("15.00");
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        beerService.findAllByPriceBetween(min, max)
                .subscribe(dto -> {
                    System.out.println(dto.getPrice());
                    atomicBoolean.set(true);
                });

        await().untilTrue(atomicBoolean);
    }

    @Test
    void testFindAllByPriceBetween_noDataInRange() {
        // Given
        BigDecimal min = new BigDecimal("5000.00");
        BigDecimal max = new BigDecimal("6000.00");

        // This should be true if there are no items in the price range
        AtomicBoolean noItemsInRange = new AtomicBoolean(true);

        beerService.findAllByPriceBetween(min, max)
                .doOnNext(dto -> {
                    noItemsInRange.set(false); // Item found in range, so set to false
                })
                .blockLast(); // Await completion of processing

        // Assert that no items were found in the price range
        assertTrue(noItemsInRange.get());
    }

    @Test
    void testFindByBeerStyle() {
        BeerDTO beerDTO1 = getSavedBeerDto();
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        beerService.findByBeerStyle(beerDTO1.getBeerStyle())
                .doOnNext(dto -> System.out.println(dto.toString())) // prints each BeerDTO
                .doOnComplete(() -> atomicBoolean.set(true)) // set the flag to true when all BeerDTOs have been processed
                .subscribe();

        await().untilTrue(atomicBoolean);
    }

    @Test
    void testFindByBeerStyle2() {
        BeerDTO beerDTO1 = getSavedBeerDto();
        CountDownLatch latch = new CountDownLatch(1);

        beerService.findByBeerStyle(beerDTO1.getBeerStyle())
                .doOnNext(dto -> System.out.println(dto.toString())) // print each BeerDTO
                .doOnComplete(latch::countDown) // Count down the latch when all BeerDTOs have been processed
                .subscribe();

        try {
            latch.await(); // Wait until latch has counted down to 0
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    @Test
    void findFirstByBeerNameTest() {
        BeerDTO beerDto = getSavedBeerDto();

        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        Mono<BeerDTO> foundDto = beerService.findFirstByBeerName(beerDto.getBeerName());

        foundDto.subscribe(dto -> {
            System.out.println(dto.toString());
            atomicBoolean.set(true);
        });

        await().untilTrue(atomicBoolean);
    }

    @Test
    @DisplayName("Test Save Beer Using Subscriber")
    void saveBeerUseSubscriber() {

        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        AtomicReference<BeerDTO> atomicDto = new AtomicReference<>();

        Mono<BeerDTO> savedMono = beerService.saveBeer(Mono.just(beerDTO));

        savedMono.subscribe(savedDto -> {
            System.out.println(savedDto.getId());
            atomicBoolean.set(true);
            atomicDto.set(savedDto);
        });

        await().untilTrue(atomicBoolean);

        BeerDTO persistedDto = atomicDto.get();
        assertThat(persistedDto).isNotNull();
        assertThat(persistedDto.getId()).isNotNull();
    }

    @Test
    @DisplayName("Test Save Beer Using Block")
    void testSaveBeerUseBlock() {
        BeerDTO savedDto = beerService.saveBeer(Mono.just(getTestBeerDto())).block();
        assertThat(savedDto).isNotNull();
        assertThat(savedDto.getId()).isNotNull();
    }

    @Test
    @DisplayName("Test Update Beer Using Block")
    void testUpdateBlocking() {
        final String newName = "New Beer Name"; // use final so cannot mutate
        BeerDTO savedBeerDto = getSavedBeerDto();
        savedBeerDto.setBeerName(newName);

        BeerDTO updatedDto = beerService.saveBeer(Mono.just(savedBeerDto)).block();

        //verify exists in db
        BeerDTO fetchedDto = beerService.getById(updatedDto.getId()).block();
        assertThat(fetchedDto.getBeerName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("Test Update Using Reactive Streams")
    void testUpdateStreaming() {
        final String newName = "New Beer Name";  // use final so cannot mutate

        AtomicReference<BeerDTO> atomicDto = new AtomicReference<>();

        beerService.saveBeer(Mono.just(getTestBeerDto()))
                .map(savedBeerDto -> {
                    savedBeerDto.setBeerName(newName);
                    return savedBeerDto;
                })
                .flatMap(beerService::saveBeer) // save updated beer
                .flatMap(savedUpdatedDto -> beerService.getById(savedUpdatedDto.getId())) // get from db
                .subscribe(dtoFromDb -> {
                    atomicDto.set(dtoFromDb);
                });

        await().until(() -> atomicDto.get() != null);
        assertThat(atomicDto.get().getBeerName()).isEqualTo(newName);
    }

    @Test
    void testDeleteBeer() {
        BeerDTO beerToDelete = getSavedBeerDto();

        beerService.deleteBeerById(beerToDelete.getId()).block();

        Mono<BeerDTO> expectedEmptyBeerMono = beerService.getById(beerToDelete.getId());

        BeerDTO emptyBeer = expectedEmptyBeerMono.block();

        assertThat(emptyBeer).isNull();
    }

    @Test
    void saveBeer() {

        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        Mono<BeerDTO> savedMono = beerService.saveBeer(Mono.just(beerDTO));

        savedMono.subscribe(savedDto -> {
            System.out.println(savedDto.getId());
            atomicBoolean.set(true);
        });

        await().untilTrue(atomicBoolean);

    }

    public BeerDTO getSavedBeerDto(){
        return beerService.saveBeer(Mono.just(getTestBeerDto())).block();
    }

    public static BeerDTO getTestBeerDto(){
        return new BeerMapperImpl().beerToBeerDto(getTestBeer());
    }

    public static Beer getTestBeer() {
        return Beer.builder()
                .beerName("Space Dust")
                .beerStyle("IPA")
                .price(BigDecimal.TEN)
                .quantityOnHand(12)
                .upc("123213")
                .build();
    }

}