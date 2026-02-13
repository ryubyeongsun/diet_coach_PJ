package com.dietcoach.project.client.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingClientResult {
    private List<ShoppingProduct> products;
    private String source; // "REAL" | "MOCK"
}
