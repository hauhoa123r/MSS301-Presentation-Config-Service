package fu.ats.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RefreshScope
@RestController
@RequestMapping("/api/v1/config-demo")
public class ConfigDemoController {

    private final Environment environment;

    private final String beanInstance =
            UUID.randomUUID().toString();

    @Value("${ats.job.max-page-size}")
    private int maxPageSize;


    public ConfigDemoController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping
    public Map<String, Object> getConfig() {
        return Map.of(
                "environmentValue",
                environment.getProperty("ats.job.max-page-size"),
                "injectedValue",
                maxPageSize,
                "beanInstance",
                beanInstance
        );
    }
}