package com.dietcoach.project.client.shopping;

public interface ShoppingClient {
    ShoppingClientResult searchProducts(String keyword, int page, int size);
}
