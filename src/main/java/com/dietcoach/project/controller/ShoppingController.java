package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import com.dietcoach.project.dto.ShoppingProductResponse;
import com.dietcoach.project.service.ShoppingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for shopping search and ingredient-based recommendations.
 */
@RestController
@RequestMapping("/api/shopping")
@RequiredArgsConstructor
public class ShoppingController {

    private final ShoppingService shoppingService;

    /**
     * Keyword-based product search endpoint.
     */
    @GetMapping("/search")
    public ApiResponse<List<ShoppingProductResponse>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        List<ShoppingProductResponse> result = shoppingService.search(keyword, page, size);
        return ApiResponse.ok("search completed", result);
    }

    /**
     * Ingredient-based product recommendations.
     */
    @GetMapping("/recommendations")
    public ApiResponse<List<ShoppingProductResponse>> recommend(
            @RequestParam String ingredient,
            @RequestParam(required = false) Integer neededGram
    ) {
        List<ShoppingProductResponse> result = shoppingService.recommend(ingredient, neededGram);
        return ApiResponse.ok("recommendation completed", result);
    }
}
