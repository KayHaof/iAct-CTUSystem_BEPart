package com.example.feature.users.repository;

import com.example.common.Clazzes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClazzRepository extends JpaRepository<Clazzes, Long> {

}
