package com.dietcoach.project.service;

import com.dietcoach.project.dto.shopping.BudgetProposalRequest;
import com.dietcoach.project.dto.shopping.BudgetProposalResponse;
import com.dietcoach.project.dto.shopping.BudgetProposalResponse.BudgetIngredientDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private static final int PLAN_DAYS = 30;

    @Override
    public BudgetProposalResponse createProposal(BudgetProposalRequest request) {
        // 1. Calculate Requirements (TDEE based)
        int dailyCal = request.getTargetCalories() > 0 ? request.getTargetCalories() : 2000;
        int totalMonthCalories = dailyCal * PLAN_DAYS;

        // Ratio: Protein 40%, Carb 40%, Veggie 20% (approx by volume/cost weight)
        // Needed Grams (Approximate for shopping volume)
        // Protein source ~ 1.5kcal/g (raw), Carbs ~ 3.5kcal/g (rice raw) or 1.5 (cooked).
        // Let's use simplified shopping unit requirements.
        
        // Target Volumes per month (Aggressive diet calculation)
        int neededProteinKg = (int) Math.ceil((dailyCal * 0.30 / 1.2) * PLAN_DAYS / 1000.0); // ex: 600kcal protein -> 500g meat -> 15kg/mo
        int neededCarbKg = (int) Math.ceil((dailyCal * 0.40 / 3.0) * PLAN_DAYS / 1000.0);    // ex: 800kcal carb -> 250g raw rice -> 7.5kg/mo
        int neededVeggieKg = 3; // Fixed 3kg base (100g/day)
        int neededFruitKg = 2;  // Fixed 2kg base

        log.info("Budget Plan Req: {} kcal -> Protein {}kg, Carb {}kg", dailyCal, neededProteinKg, neededCarbKg);

        // 2. Optimization Strategy
        // Levels: 0 (Luxury) -> 1 (Standard) -> 2 (Budget) -> 3 (Survival)
        List<InternalIngredient> pool = new ArrayList<>();
        int level = 0;
        int totalCost = 0;
        int budget = request.getMonthlyBudget();
        
        // Loop to find fitting plan
        while (level <= 3) {
            pool = generatePool(level, neededProteinKg, neededCarbKg, neededVeggieKg, neededFruitKg);
            totalCost = calculateTotalCost(pool);
            
            if (totalCost <= budget) {
                break;
            }
            level++;
        }

        // 3. Build Response
        String status = "LOCKED";
        String warning = null;
        
        if (totalCost > budget) {
            status = "WARNING_OVER_BUDGET";
            warning = String.format("입력하신 예산(%d원)으로는 필요한 영양소(%dkcal 기준)를 모두 채우기 어렵습니다. \n최소한의 구성으로 맞춘 금액은 %d원입니다.", 
                    budget, dailyCal, totalCost);
        }

        return BudgetProposalResponse.builder()
                .budget(budget)
                .finalCost(totalCost)
                .status(status)
                .warningMessage(warning)
                .ingredients(pool.stream().map(this::toDto).collect(Collectors.toList()))
                .build();
    }

    private List<InternalIngredient> generatePool(int level, int pKg, int cKg, int vKg, int fKg) {
        List<InternalIngredient> list = new ArrayList<>();

        // --- Protein Strategy ---
        if (level == 0) { // Luxury: Beef + Salmon
            addSku(list, "소고기", "호주산 척아이롤 1kg", 25000, pKg / 2);
            addSku(list, "연어", "생연어 횟감 500g", 22000, pKg - (pKg / 2)); 
        } else if (level == 1) { // Standard: Chicken + Pork
            addSku(list, "닭가슴살", "냉동 닭가슴살 1kg", 11000, pKg - 2);
            addSku(list, "돼지고기", "한돈 뒷다리살 1kg", 8900, 2);
        } else if (level == 2) { // Budget: Chicken + Eggs
            addSku(list, "닭가슴살", "냉동 닭가슴살 1kg 팩", 9900, pKg - 2);
            addSku(list, "계란", "무항생제 대란 30구", 7500, 2); // 2 trays approx 3kg weight equivalent in nutrition
        } else { // Survival: Eggs + Tofu + Cheapest Chicken
            addSku(list, "닭가슴살", "브라질산 닭가슴살 2kg", 14000, pKg / 2);
            addSku(list, "계란", "대란 30구 특가", 6500, 2);
            addSku(list, "두부", "시장 두부 1kg", 3000, 2);
        }

        // --- Carb Strategy ---
        if (level == 0) { // Luxury: Sweet Potato + Multi-grain
            addSku(list, "고구마", "꿀고구마 3kg", 18000, cKg / 3);
            addSku(list, "잡곡", "혼합 15곡 4kg", 18000, cKg - (cKg / 3));
        } else if (level <= 2) { // Standard: Brown Rice
            addSku(list, "현미", "국산 현미 10kg", 29000, 1); // Bulk is cheaper
        } else { // Survival: White Rice (Bulk)
            addSku(list, "쌀", "정부미/특가 쌀 10kg", 24000, 1);
        }

        // --- Veggie/Fruit ---
        if (level == 0) {
            addSku(list, "샐러드", "손질 샐러드 1kg", 12000, vKg);
            addSku(list, "사과", "부사 사과 3kg", 25000, 1);
        } else if (level == 1) {
            addSku(list, "냉동야채", "냉동 혼합야채 2kg", 11000, 2);
            addSku(list, "바나나", "바나나 1송이", 4500, 2);
        } else {
            addSku(list, "양배추", "통 양배추 1망 (3입)", 8000, 1);
            // Fruit removed for strict budget or kept minimal
            if (level < 3) addSku(list, "냉동베리", "냉동 블루베리 1kg", 9900, 1);
        }

        return list;
    }

    private void addSku(List<InternalIngredient> list, String abstractName, String skuName, int price, int qty) {
        if (qty <= 0) return;
        list.add(new InternalIngredient(abstractName, skuName, price, qty));
    }

    private int calculateTotalCost(List<InternalIngredient> pool) {
        return pool.stream().mapToInt(i -> i.price * i.quantity).sum();
    }

    private BudgetIngredientDto toDto(InternalIngredient i) {
        return BudgetIngredientDto.builder()
                .name(i.name)
                .skuName(i.skuName)
                .price(i.price)
                .quantity(i.quantity)
                .imageUrl("") // Placeholder handled by frontend
                .link("https://www.11st.co.kr/main") // Generic link
                .build();
    }

    @Data
    @AllArgsConstructor
    private static class InternalIngredient {
        String name;
        String skuName;
        int price;
        int quantity;
    }
}
