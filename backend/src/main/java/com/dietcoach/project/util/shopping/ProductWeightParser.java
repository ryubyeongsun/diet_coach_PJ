package com.dietcoach.project.util.shopping;

import org.springframework.stereotype.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProductWeightParser {

    // Matches numbers followed by units. 
    // (?i) case insensitive
    // (\d+(?:[.,]\d+)?) : Number (integer or decimal, supports 1.5 or 1,5)
    // \s* : optional space
    // (kg|g|ml|l|lb|oz) : Unit
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(kg|g|ml|l|lb|oz)");
    private static final Pattern MULTIPLY_PATTERN = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(kg|g|ml|l)\\s*[xX*]\\s*(\\d+)");

    public int parseWeightInGrams(String title) {
        if (title == null || title.isBlank()) return 0;
        
        Matcher multiMatcher = MULTIPLY_PATTERN.matcher(title);
        int maxGrams = 0;
        boolean found = false;

        while (multiMatcher.find()) {
            try {
                String numStr = multiMatcher.group(1).replace(",", ".");
                double val = Double.parseDouble(numStr);
                String unit = multiMatcher.group(2).toLowerCase();
                int multiplier = Integer.parseInt(multiMatcher.group(3));

                int grams = 0;
                switch (unit) {
                    case "kg":
                    case "l":
                        grams = (int) (val * 1000);
                        break;
                    case "g":
                    case "ml":
                        grams = (int) val;
                        break;
                }
                grams *= multiplier;

                if (grams > maxGrams) {
                    maxGrams = grams;
                    found = true;
                }
            } catch (Exception e) {
                // ignore parsing errors
            }
        }

        Matcher matcher = WEIGHT_PATTERN.matcher(title);

        // Strategy: Iterate all matches.
        // We prefer the largest value found, assuming the title might contain "100g x 10ea (1kg)"
        // In this case, 1000g is better than 100g as the package size.
        // However, if it says "Chicken 100g", we get 100g.
        
        while (matcher.find()) {
            try {
                String numStr = matcher.group(1).replace(",", ".");
                double val = Double.parseDouble(numStr);
                String unit = matcher.group(2).toLowerCase();
                
                int grams = 0;
                switch (unit) {
                    case "kg":
                    case "l":
                        grams = (int) (val * 1000);
                        break;
                    case "g":
                    case "ml":
                        grams = (int) val;
                        break;
                    case "lb":
                         grams = (int) (val * 453.592);
                         break;
                    case "oz":
                         grams = (int) (val * 28.3495);
                         break;
                }
                
                // Sanity check: if grams is very small (e.g. 0g sugar), ignore? 
                // But maybe it's saffron 1g.
                if (grams > maxGrams) {
                    maxGrams = grams;
                    found = true;
                }
            } catch (Exception e) {
                // ignore parsing errors
            }
        }
        
        return found ? maxGrams : 0;
    }
}
