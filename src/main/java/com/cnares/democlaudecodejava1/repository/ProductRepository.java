package com.cnares.democlaudecodejava1.repository;

import com.cnares.democlaudecodejava1.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
}
