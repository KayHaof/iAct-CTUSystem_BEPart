package com.example.common.repository;

import com.example.common.entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalCategoryRepository extends JpaRepository<Categories, Long> {
}
