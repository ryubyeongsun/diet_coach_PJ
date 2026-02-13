package com.dietcoach.project.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingProductResponse {
    private String externalId;
    private String title;
    private int price;
    private Double gramPerUnit;
    private String imageUrl;
    private String productUrl;
    private String mallName;
}
