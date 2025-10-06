package com.snow.popin.domain.category.service;

import com.snow.popin.domain.category.repository.CategoryRepository;
import com.snow.popin.domain.category.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryDataLoader implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            initializeCategories();
        }
    }

    private void initializeCategories() {
        List<Category> categories = List.of(
                Category.of("패션", "fashion"),
                Category.of("반려동물", "pet"),
                Category.of("게임", "game"),
                Category.of("캐릭터/IP", "character"),
                Category.of("문화/컨텐츠", "culture"),
                Category.of("연예", "entertainment"),
                Category.of("여행/레저/스포츠", "travel")
        );

        categoryRepository.saveAll(categories);
        log.info("카테고리 초기 데이터 {} 개가 생성되었습니다.", categories.size());
    }
}