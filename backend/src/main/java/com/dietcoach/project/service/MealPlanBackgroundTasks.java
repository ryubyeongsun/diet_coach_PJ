package com.dietcoach.project.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MealPlanBackgroundTasks {
    private final Executor executor;

    public MealPlanBackgroundTasks(@Qualifier("mealTaskExecutor") Executor executor) {
        this.executor = executor;
    }

    public void afterCommit(Long planId, Runnable task) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("식단 생성에는 트랜잭션이 필요합니다.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    CompletableFuture.runAsync(task, executor).whenComplete((ignored, error) -> {
                        if (error != null) log.error("[MealPlan] Background generation failed planId={}", planId, error);
                    });
                } catch (RejectedExecutionException error) {
                    // The first week is already committed: do not report a misleading creation failure.
                    log.error("[MealPlan] Background queue full; remaining days not generated planId={}", planId, error);
                }
            }
        });
    }
}
