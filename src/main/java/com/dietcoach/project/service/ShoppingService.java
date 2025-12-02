package com.dietcoach.project.service;

import com.dietcoach.project.dto.ShoppingProductResponse;

import java.util.List;

/**
 * Business layer for shopping search and ingredient-based recommendations.
 */
public interface ShoppingService {

    /**
     * Performs a keyword based product search.
     */
    List<ShoppingProductResponse> search(String keyword, Integer page, Integer size);

    /**
     * Recommends products for a given ingredient and required gram amount.
     */
    List<ShoppingProductResponse> recommend(String ingredient, Integer neededGram);
}
