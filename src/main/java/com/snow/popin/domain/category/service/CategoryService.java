package com.snow.popin.domain.category.service;

import com.snow.popin.domain.category.dto.CategoryResponseDto;
import com.snow.popin.domain.category.entity.Category;
import com.snow.popin.domain.category.repository.CategoryRepository;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // 전체 카테고리 목록 조회
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
        return categories.stream()
                .map(CategoryResponseDto::from)
                .collect(Collectors.toList());
    }

    // 사용자 관심 카테고리 조회
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getUserCategories(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException.UserNotFound(userId));

        return user.getInterests().stream()
                .map(interest -> CategoryResponseDto.from(interest.getCategory()))
                .collect(Collectors.toList());
    }

    // 사용자 관심 카테고리 업데이트
    @Transactional
    public List<CategoryResponseDto> updateUserCategories(Long userId, List<String> categoryNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException.UserNotFound(userId));

        // 기존 관심사 전부 삭제 (orphanRemoval 보장)
        user.getInterests().clear();
        userRepository.flush(); // 즉시 DB 반영 → 중복 insert 방지

        // 새로운 카테고리 찾기
        Set<Category> categories = categoryRepository.findByNameIn(categoryNames);

        // User 엔티티 메서드로 관심사 재설정
        user.updateInterests(List.copyOf(categories));

        // 저장
        userRepository.save(user);

        return categories.stream()
                .map(CategoryResponseDto::from)
                .collect(Collectors.toList());
    }
}
