package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.PaymentTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByAppTransId(String appTransId);
}
