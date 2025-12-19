package com.dietcoach.project.service;

import com.dietcoach.project.dto.ShoppingProductsResponse;

public interface ShoppingService {
    ShoppingProductsResponse search(String keyword, int page, int size);
}
