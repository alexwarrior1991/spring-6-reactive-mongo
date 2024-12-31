package gure.springframework.reactivemongo.mappers;

import gure.springframework.reactivemongo.domain.Beer;
import gure.springframework.reactivemongo.model.BeerDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BeerMapper {

    BeerDTO beerToBeerDto(Beer beer);

    Beer beerDtoToBeer(BeerDTO beerDTO);
}
