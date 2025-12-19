package com.dietcoach.project.client.ai.dto;

import java.util.List;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AiMonthlySkeletonResponse {
    private List<AiDaySkeleton> days;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class AiDaySkeleton {
        private Integer dayIndex;
        private String planDate; // "yyyy-MM-dd"
        private List<AiMealSkeleton> meals;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class AiMealSkeleton {
        private String mealTime;      // BREAKFAST|LUNCH|DINNER|SNACK
        private String menuName;      // "Oatmeal bowl"
        private List<String> ingredients; // ["oats","milk","banana"]
        private Integer calories;     // 참고값(우리는 0 저장 or memo)
    }
}
