package com.dietcoach.project;

import com.dietcoach.project.client.ai.DietAiClient;
import com.dietcoach.project.domain.meal.MealItem;
import com.dietcoach.project.domain.meal.MealPlanDay;
import com.dietcoach.project.dto.auth.LoginRequest;
import com.dietcoach.project.security.JwtTokenProvider;
import com.dietcoach.project.service.*;
import com.dietcoach.project.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SafetyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired ObjectMapper json;
    @Autowired JwtTokenProvider tokens;
    @Autowired PasswordEncoder passwords;
    @Autowired MealDayWriter writer;
    @Autowired AuthService auth;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean DietAiClient ai;
    @MockitoBean ShoppingService shopping;
    @MockitoBean(name = "mealTaskExecutor") Executor executor;

    @BeforeEach
    void seed() {
        for (String table : List.of("meal_intakes", "refresh_tokens", "shopping_cart_items", "meal_items",
                "meal_plan_days", "meal_plans", "weight_records", "users")) db.update("DELETE FROM " + table);
        String hash = passwords.encode("Password1!");
        for (long id : List.of(1L, 2L)) {
            db.update("INSERT INTO users (id,email,password,name,height,weight,bmr,tdee,target_calories) VALUES (?,?,?,?,170,70,1500,2000,1800)",
                    id, "user" + id + "@example.com", hash, "user" + id);
        }
        db.update("INSERT INTO meal_plans (id,user_id,start_date,end_date,total_days,target_calories_per_day,monthly_budget,meals_per_day) VALUES (10,1,'2026-10-01','2026-10-30',30,1800,300000,3)");
        db.update("INSERT INTO meal_plan_days (id,meal_plan_id,plan_date,day_index,total_calories) VALUES (100,10,'2026-10-01',1,0)");
    }

    private String access(long id) { return "Bearer " + tokens.createAccessToken(id); }

    @ParameterizedTest
    @CsvSource({
        "GET,/api/users/1", "GET,/api/users/1/tdee", "GET,/api/users/1/body-status",
        "GET,/api/users/1/weights", "GET,/api/users/1/meal-plans/latest",
        "GET,/api/dashboard/trend?userId=1", "GET,/api/meal-plans/10",
        "GET,/api/meal-plans/10/ingredients", "GET,/api/meal-plans/10/shopping",
        "GET,/api/meal-plans/10/shopping-list", "GET,/api/meal-plans/days/100",
        "POST,/api/meal-plans/days/100/stamp", "POST,/api/meal-plans/days/100/regenerate",
        "POST,/api/meal-plans/days/100/meals/LUNCH/replace",
        "DELETE,/api/users/1/weights/2026-10-01"
    })
    void rejectsOtherUsersResources(String method, String url) throws Exception {
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), url).header("Authorization", access(2)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM meal_items", Integer.class));
        assertFalse(db.queryForObject("SELECT is_stamped FROM meal_plan_days WHERE id=100", Boolean.class));
        verifyNoInteractions(ai, shopping, executor);
    }

    @Test
    void rejectsOtherUsersWeightAndIntakeWrites() throws Exception {
        mvc.perform(post("/api/users/1/weights").header("Authorization", access(2)).contentType("application/json")
                .content("{\"recordDate\":\"2026-10-01\",\"weight\":65}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/meal-intakes").header("Authorization", access(2)).contentType("application/json")
                .content("{\"mealPlanDayId\":100,\"mealTime\":\"LUNCH\",\"isConsumed\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/meal-intakes").header("Authorization", access(2)).contentType("application/json")
                .content("{\"mealPlanDayId\":100,\"mealTime\":\"LUNCH\",\"isConsumed\":false}"))
                .andExpect(status().isForbidden());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM weight_records", Integer.class));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM meal_intakes", Integer.class));
    }

    @Test
    void ownerKeepsExistingReadAndWriteContracts() throws Exception {
        mvc.perform(get("/api/users/1").header("Authorization", access(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
        mvc.perform(get("/api/meal-plans/10").header("Authorization", access(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(get("/api/meal-plans/days/100").header("Authorization", access(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.dayId").value(100));
        mvc.perform(post("/api/users/1/weights").header("Authorization", access(1)).contentType("application/json")
                .content("{\"recordDate\":\"2026-10-01\",\"weight\":65}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.weight").value(65.0));
        mvc.perform(post("/api/meal-plans/days/100/stamp").header("Authorization", access(1)))
                .andExpect(status().isOk());
        assertTrue(db.queryForObject("SELECT is_stamped FROM meal_plan_days WHERE id=100", Boolean.class));
        mvc.perform(post("/api/meal-plans/days/100/stamp").header("Authorization", access(1)))
                .andExpect(status().isOk());
        assertFalse(db.queryForObject("SELECT is_stamped FROM meal_plan_days WHERE id=100", Boolean.class));
        mvc.perform(put("/api/meal-intakes").header("Authorization", access(1)).contentType("application/json")
                .content("{\"mealPlanDayId\":100,\"mealTime\":\"LUNCH\",\"isConsumed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.isConsumed").value(true));
    }

    @Test
    void refreshTokenCannotAuthenticateAnApiRequest() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokens.createRefreshToken(1L)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", access(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void sameSecondTokensAreDistinctAndOldTokensCannotBeReused() {
        assertNotEquals(tokens.createRefreshToken(1L), tokens.createRefreshToken(1L));
        LoginRequest login = new LoginRequest();
        login.setEmail("user1@example.com"); login.setPassword("Password1!");
        String original = auth.login(login).getRefreshToken();
        String replacement = auth.refresh(original).getRefreshToken();
        assertNotEquals(original, replacement);
        assertThrows(RuntimeException.class, () -> auth.refresh(original));
        assertNotNull(auth.refresh(replacement).getAccessToken());
    }

    @Test
    void onlyOneConcurrentRefreshSucceeds() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setEmail("user1@example.com"); login.setPassword("Password1!");
        String original = auth.login(login).getRefreshToken();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> refresh = () -> {
            start.await();
            try { auth.refresh(original); return true; }
            catch (com.dietcoach.project.common.error.BusinessException e) { return false; }
        };
        try {
            Future<Boolean> first = pool.submit(refresh), second = pool.submit(refresh);
            start.countDown();
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE revoked=0", Integer.class));
        } finally { pool.shutdownNow(); }
    }

    private MealItem item(String name, int calories) {
        return MealItem.builder().mealPlanDayId(100L).mealTime("LUNCH").foodName(name).calories(calories)
                .grams(100).carbs(0).protein(0).fat(0).isHighProtein(false).build();
    }

    @Test
    void dayItemsAndCalorieTotalCommitTogether() {
        writer.save(MealPlanDay.builder().id(100L).build(), List.of(item("밥", 300), item("달걀", 100)));
        assertEquals(2, db.queryForObject("SELECT COUNT(*) FROM meal_items", Integer.class));
        assertEquals(400, db.queryForObject("SELECT total_calories FROM meal_plan_days WHERE id=100", Integer.class));
    }

    @Test
    void partialDayInsertRollsBackOnFailure() {
        assertThrows(RuntimeException.class, () -> writer.save(MealPlanDay.builder().id(100L).build(),
                List.of(item("밥", 300), item(null, 100))));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM meal_items", Integer.class));
        assertEquals(0, db.queryForObject("SELECT total_calories FROM meal_plan_days WHERE id=100", Integer.class));
    }

    @Test
    void backgroundSubmissionHappensOnlyOnCommit() {
        List<Runnable> queued = new ArrayList<>();
        MealPlanBackgroundTasks tasks = new MealPlanBackgroundTasks(queued::add);
        AtomicInteger called = new AtomicInteger();
        TransactionTemplate tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            tasks.afterCommit(10L, called::incrementAndGet);
            assertTrue(queued.isEmpty());
        });
        assertEquals(1, queued.size()); queued.remove(0).run(); assertEquals(1, called.get());
        tx.executeWithoutResult(status -> { tasks.afterCommit(10L, called::incrementAndGet); status.setRollbackOnly(); });
        assertTrue(queued.isEmpty()); assertEquals(1, called.get());
        assertThrows(IllegalStateException.class, () -> tasks.afterCommit(10L, called::incrementAndGet));
    }

    @Test
    void executorSaturationDoesNotUndoCommittedFirstWeek() {
        MealPlanBackgroundTasks tasks = new MealPlanBackgroundTasks(task -> { throw new RejectedExecutionException("full"); });
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            db.update("UPDATE meal_plan_days SET total_calories=1800 WHERE id=100");
            tasks.afterCommit(10L, () -> fail("Rejected task must not execute"));
        });
        assertEquals(1800, db.queryForObject("SELECT total_calories FROM meal_plan_days WHERE id=100", Integer.class));
    }

    @Test
    void firstSevenDaysRemainSynchronousAndRemainingTwentyThreeAreSequential() throws Exception {
        // Returning null uses the existing template fallback, without external API calls.
        when(ai.generateMonthlySkeleton(anyMap())).thenReturn(null);
        String body = mvc.perform(post("/api/meal-plans").header("Authorization", access(1))
                .contentType("application/json").content("{\"startDate\":\"2026-11-01\",\"mealsPerDay\":3}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(json.readTree(body).get("success").asBoolean());
        long plan = db.queryForObject("SELECT MAX(id) FROM meal_plans", Long.class);
        assertEquals(30, db.queryForObject("SELECT COUNT(*) FROM meal_plan_days WHERE meal_plan_id=?", Integer.class, plan));
        assertEquals(7, db.queryForObject("SELECT COUNT(*) FROM meal_plan_days WHERE meal_plan_id=? AND total_calories>0", Integer.class, plan));
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(task.capture());
        task.getValue().run();
        assertEquals(30, db.queryForObject("SELECT COUNT(*) FROM meal_plan_days WHERE meal_plan_id=? AND total_calories>0", Integer.class, plan));
        ArgumentCaptor<java.util.Map<String,Object>> payloads = ArgumentCaptor.forClass(java.util.Map.class);
        verify(ai, times(5)).generateMonthlySkeleton(payloads.capture());
        assertEquals(List.of(7,7,7,7,2), payloads.getAllValues().stream().map(p -> p.get("totalDays")).toList());
        assertEquals(List.of("2026-11-01","2026-11-08","2026-11-15","2026-11-22","2026-11-29"),
                payloads.getAllValues().stream().map(p -> p.get("startDate")).toList());
    }

    @Test
    void rejectsOtherUsersProfileUpdate() throws Exception {
        mvc.perform(put("/api/users/1/profile").header("Authorization", access(2)).contentType("application/json")
                .content("{\"gender\":\"MALE\",\"birthDate\":\"2000-01-01\",\"height\":170,\"weight\":70,\"targetWeight\":65,\"activityLevel\":\"SEDENTARY\",\"goalType\":\"MAINTAIN\"}"))
                .andExpect(status().isForbidden());
        assertNull(db.queryForObject("SELECT gender FROM users WHERE id=1", String.class));
    }

    @Test
    void legacyUserCreationStillProducesALoginCompatiblePassword() throws Exception {
        mvc.perform(post("/api/users").header("Authorization", access(1)).contentType("application/json")
                .content("{\"email\":\"new@example.com\",\"password\":\"Password1!\",\"name\":\"new\",\"gender\":\"MALE\",\"birthDate\":\"2000-01-01\",\"height\":170,\"weight\":70,\"targetWeight\":65,\"activityLevel\":\"SEDENTARY\",\"goalType\":\"MAINTAIN\"}"))
                .andExpect(status().isOk());
        String stored = db.queryForObject("SELECT password FROM users WHERE email='new@example.com'", String.class);
        assertNotEquals("Password1!", stored);
        assertTrue(passwords.matches("Password1!", stored));
        LoginRequest login = new LoginRequest(); login.setEmail("new@example.com"); login.setPassword("Password1!");
        assertNotNull(auth.login(login).getAccessToken());
    }

    @Test
    void malformedInputAndInvalidTokenReturnClientErrors() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/api/shopping/search").header("Authorization", access(1)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/not-a-number").header("Authorization", access(1)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokens.createAccessToken(null)))
                .andExpect(status().isUnauthorized());
    }

    private void stubShopping(long price, int packageGram, int requiredGram, String ingredient) {
        db.update("INSERT INTO meal_items (meal_plan_day_id,meal_time,food_name,calories,grams,carbs,protein,fat,is_high_protein) VALUES (100,'LUNCH',?,300,?,0,0,0,false)", ingredient, requiredGram);
        var product = com.dietcoach.project.dto.meal.ShoppingListResponse.ProductCard.builder()
                .productName(ingredient).price(price).packageGram(packageGram).build();
        when(shopping.searchOne(eq(ingredient), anyInt(), eq((long)requiredGram)))
                .thenReturn(new ShoppingService.SearchOneResult(product,"REAL"));
    }

    @Test
    void shoppingStillRoundsUpPurchaseCountAndMultipliesCost() throws Exception {
        stubShopping(3000, 500, 1200, "닭가슴살");
        mvc.perform(get("/api/meal-plans/10/shopping?range=TODAY&date=2026-10-01").header("Authorization", access(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.budget").value(10000))
                .andExpect(jsonPath("$.data.items[0].recommendedCount").value(3))
                .andExpect(jsonPath("$.data.estimatedTotal").value(9000));
    }

    @ParameterizedTest
    @CsvSource({"11500,1", "11501,0"})
    void shoppingKeepsExisting115PercentBudgetBoundary(long price, int expectedItems) throws Exception {
        // Distinct ingredients avoid reusing the existing one-hour product cache between cases.
        stubShopping(price, 100, 100, "토마토" + price);
        mvc.perform(get("/api/meal-plans/10/shopping?range=TODAY&date=2026-10-01").header("Authorization", access(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(expectedItems));
    }
}
