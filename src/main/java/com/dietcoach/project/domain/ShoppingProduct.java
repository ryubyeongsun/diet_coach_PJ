package com.dietcoach.project.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal representation of a shopping product fetched from external marketplaces.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingProduct {

    /**
     * External product identifier from marketplace (e.g. 11st productCode).
     */
    private String externalId;

    /**
     * Display name of the product.
     */
    private String title;

    /**
     * Total price in KRW.
     */
    private int price;

    /**
     * Optional: approximate gram per unit to calculate g-based price.
     */
    private Double gramPerUnit;

    /**
     * Product thumbnail image URL.
     */
    private String imageUrl;

    /**
     * Product detail page URL.
     */
    private String productUrl;

    /**
     * Marketplace name (e.g. 11st, Coupang, etc.)
     */
    private String mallName;
}
