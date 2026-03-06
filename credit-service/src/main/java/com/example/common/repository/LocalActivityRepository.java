package com.example.common.repository;

import com.example.common.entity.Activities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalActivityRepository extends JpaRepository<Activities, Long> {

}
