package com.dietcoach.project.dto.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
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

    public static ShoppingProductResponse from(ShoppingProduct p) {
        return ShoppingProductResponse.builder()
                .externalId(p.getExternalId())
                .title(p.getTitle())
                .price(p.getPrice())
                .gramPerUnit(p.getGramPerUnit())
                .imageUrl(p.getImageUrl())
                .productUrl(p.getProductUrl())
                .mallName(p.getMallName())
                .build();
    }
}
