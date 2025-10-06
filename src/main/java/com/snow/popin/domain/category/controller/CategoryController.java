package com.snow.popin.domain.category.controller;

import com.snow.popin.domain.category.dto.CategoryResponseDto;
import com.snow.popin.domain.category.service.CategoryService;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserUtil userUtil;

    // 전체 카테고리 목록 조회
    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        List<CategoryResponseDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    // 사용자 관심 카테고리 조회
    @GetMapping("/me")
    public ResponseEntity<List<CategoryResponseDto>> getMyCategories() {
        Long userId = userUtil.getCurrentUserId();
        List<CategoryResponseDto> categories = categoryService.getUserCategories(userId);
        return ResponseEntity.ok(categories);
    }

    // 사용자 관심 카테고리 업데이트
    @PutMapping("/me")
    public ResponseEntity<List<CategoryResponseDto>> updateMyCategories(
            @RequestBody List<String> categoryNames
    ) {
        Long userId = userUtil.getCurrentUserId();
        List<CategoryResponseDto> updated = categoryService.updateUserCategories(userId, categoryNames);
        return ResponseEntity.ok(updated);
    }
}
