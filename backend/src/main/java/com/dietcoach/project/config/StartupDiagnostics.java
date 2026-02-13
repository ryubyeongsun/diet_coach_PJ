package com.dietcoach.project.config;

import java.net.URL;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.dietcoach.project.client.ai.DietAiClient;
import com.dietcoach.project.service.MealPlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDiagnostics implements ApplicationRunner {

    private final ApplicationContext applicationContext;
    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[Startup] activeProfiles={}", String.join(",", environment.getActiveProfiles()));

        BuildProperties build = buildPropertiesProvider.getIfAvailable();
        if (build != null) {
            log.info("[Startup] build.version={}, build.time={}", build.getVersion(), build.getTime());
        } else {
            log.info("[Startup] build.info=not-found (build-info.properties missing)");
        }

        logBeanSummary(MealPlanService.class);
        logBeanSummary(DietAiClient.class);

        logCodeSource("MealPlanServiceImpl", com.dietcoach.project.service.MealPlanServiceImpl.class);
        logCodeSource("DietAiClient", DietAiClient.class);
    }

    private void logBeanSummary(Class<?> type) {
        Map<String, ?> beans = applicationContext.getBeansOfType(type);
        if (beans.isEmpty()) {
            log.warn("[Startup] beanMissing type={}", type.getName());
            return;
        }
        beans.forEach((name, bean) ->
                log.info("[Startup] bean type={} name={} class={}", type.getName(), name, bean.getClass().getName()));
    }

    private void logCodeSource(String label, Class<?> type) {
        try {
            URL location = type.getProtectionDomain().getCodeSource().getLocation();
            log.info("[Startup] codeSource {}={}", label, location);
        } catch (Exception e) {
            log.warn("[Startup] codeSource {}=unavailable reason={}", label, e.getMessage());
        }
    }
}
