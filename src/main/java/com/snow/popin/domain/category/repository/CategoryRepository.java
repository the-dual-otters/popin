package com.snow.popin.domain.category.repository;

import com.snow.popin.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    java.util.List<Category> findAllByOrderByIdAsc();

    Set<Category> findByNameIn(List<String> names);
}