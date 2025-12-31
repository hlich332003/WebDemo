package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findOneByPhone(String phone);

    boolean existsByPhone(String phone);
}
