package com.snow.popin.domain.popup.repository;

import com.snow.popin.domain.popup.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    // 태그 이름으로 조회
    Optional<Tag> findByName(String name);

    // 태그 이름 리스트로 조회
    List<Tag> findByNameIn(List<String> names);
}
