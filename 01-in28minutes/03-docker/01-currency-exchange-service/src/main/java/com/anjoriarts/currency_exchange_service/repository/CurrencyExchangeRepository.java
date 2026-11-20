package com.anjoriarts.currency_exchange_service.repository;

import com.anjoriarts.currency_exchange_service.bean.CurrencyExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyExchangeRepository extends JpaRepository<CurrencyExchange, Long> {

    // Spring Data builds a lookup using both currency fields from this method name.
    CurrencyExchange findByFromAndTo(String from, String to);
}
