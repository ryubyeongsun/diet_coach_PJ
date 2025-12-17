package com.dietcoach.project.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingProductsResponse {
    private String source; // "REAL" | "MOCK"
    private List<ShoppingProductResponse> products;
}
