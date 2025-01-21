package com.example.rbcs.domain.repository;

import com.example.rbcs.domain.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByStatusAndCreatedAtLessThanEqual(Transaction.Status status, Date date, Pageable pageable);
}