# Budget-Based Shopping List System

## Overview
This feature allows users to generate a grocery shopping list that strictly adheres to a monthly budget. It prioritizes the budget over specific meal preferences, ensuring financial constraints are met first.

## Key Components

### Backend
*   **Service**: `BudgetServiceImpl`
    *   **Logic**:
        1.  Creates an initial ingredient pool based on nutritional needs (Protein, Carb, Veggie, Fruit).
        2.  Maps ingredients to "Mock" 11st SKUs (Simulating real API).
        3.  Calculates total cost.
        4.  **Optimization Loop**: If cost > budget, applies reduction rules (reduce expensive proteins, fruits, veggies) iteratively until the budget is met.
*   **Controller**: `ShoppingController`
    *   Endpoint: `POST /api/shopping/budget-proposal`
*   **DTOs**: `BudgetProposalRequest`, `BudgetProposalResponse`.

### Frontend
*   **Page**: `BudgetSetupPage.vue` (`/shopping/budget`)
    *   Input: Monthly Budget, Calories, Meals/Day.
    *   Output: Locked Shopping List with SKU details and Total Cost.
    *   Status: Shows "LOCKED" when the list fits the budget.
*   **API**: `createBudgetProposal` in `shoppingApi.js`.

## Usage
1.  Navigate to "Shopping List" page.
2.  Click "💸 예산으로 시작하기".
3.  Enter budget and calories.
4.  Click "Confirm".
5.  View the optimized list and click "Create Meal Plan" (Link to meal plan page).

## Future Improvements
*   Replace Mock SKU data with real 11st API integration.
*   Implement "Create Meal Plan" to actually lock these ingredients in the database for the Meal Plan generator to use.
