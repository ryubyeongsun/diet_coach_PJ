package com.dietcoach.project.client.shopping;

import com.dietcoach.project.domain.ShoppingProduct;

import java.util.List;

/**
 * Abstraction over external shopping APIs (11st, Coupang, etc.).
 *
 * Implementation can be replaced without changing service layer.
 */
public interface ShoppingClient {

    /**
     * Searches products by keyword.
     *
     * @param keyword search term such as ingredient name.
     * @param page    page index starting from 1.
     * @param size    page size.
     */
    List<ShoppingProduct> searchProducts(String keyword, int page, int size);
}
