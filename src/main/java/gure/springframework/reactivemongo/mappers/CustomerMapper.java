package gure.springframework.reactivemongo.mappers;

import gure.springframework.reactivemongo.domain.Customer;
import gure.springframework.reactivemongo.model.CustomerDTO;
import org.mapstruct.Mapper;

@Mapper
public interface CustomerMapper {

    CustomerDTO customerToCustomerDto(Customer customer);

    Customer customerDtoToCustomer(CustomerDTO customerDTO);
}
