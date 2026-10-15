package fu.ats.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config-demo")
public class ConfigDemoController {

    @Value("${ats.job.max-page-size}")
    private int maxPageSize;

    @GetMapping
    public int getMaxPageSize() {
        return maxPageSize;
    }
}