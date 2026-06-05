package com.example.activityservice.feature.categories.repository;

import com.example.activityservice.feature.categories.model.Categories;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Categories, Long> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    @EntityGraph(attributePaths = "subCategories")
    List<Categories> findByParentIsNull();

    @EntityGraph(attributePaths = "subCategories")
    List<Categories> findByParentIsNullAndIsActive(Boolean isActive);

    List<Categories> findByIsActive(Boolean isActive);

    List<Categories> findByParentId(Long parentId);

    List<Categories> findByParentIdAndIsActive(Long parentId, Boolean isActive);

    boolean existsByParentId(Long parentId);

    @Query("SELECT COALESCE(SUM(c.maxPoint), 0) FROM Categories c WHERE c.parent IS NULL")
    Integer sumMaxPointBySemesterId(@Param("semesterId") Long semesterId);

    @EntityGraph(attributePaths = "subCategories")
    @Query("SELECT c FROM Categories c WHERE c.parent IS NULL AND c.isActive = true")
    List<Categories> findRootCategories(@Param("semesterId") Long semesterId);
}
