package com.dietcoach.project.dto;

import lombok.Builder;
import lombok.Data;

/**
 * API response model for shopping product.
 */
@Data
@Builder
public class ShoppingProductResponse {

    private String externalId;

    private String name;

    private int price;

    private Double gramPerUnit;

    private Double pricePer100g;

    private String imageUrl;

    private String productUrl;

    private String mallName;
}
