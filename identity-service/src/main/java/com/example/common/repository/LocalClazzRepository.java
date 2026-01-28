package com.example.common.repository;

import com.example.common.entity.Clazzes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalClazzRepository extends JpaRepository<Clazzes, Long> {
}
