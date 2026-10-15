package fu.ats.controller;

import fu.ats.dto.JobRequest;
import fu.ats.dto.JobResponse;
import fu.ats.dto.ResponseApi;
import fu.ats.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;
    private final ConfigurableEnvironment environment;

    @GetMapping("/demo")
    public ResponseEntity<ResponseApi> demo() {
        String key = "ats.demo.message";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activeProfiles", environment.getActiveProfiles());
        data.put("message", environment.getProperty(key));
        List<JobResponse> jobs = jobService.findAll();
        data.put("totalJobs", jobs.size());
        data.put("jobs", jobs);

        return ResponseEntity.ok(
                ResponseApi.builder()
                        .code(HttpStatus.OK)
                        .message("Config hiện tại của service")
                        .data(data)
                        .build()
        );
    }

    @PostMapping
    public ResponseEntity<ResponseApi> create(@Valid @RequestBody JobRequest request) {
        JobResponse jobResponse = jobService.save(request);

        return ResponseEntity.ok(ResponseApi.builder()
                .message("Job created")
                .code(HttpStatus.CREATED)
                 .data(jobResponse)
                .build());
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<ResponseApi> read(@Valid @PathVariable(name = "id") UUID uuid) {
//        return ResponseEntity.ok(ResponseApi.builder().code(HttpStatus.ACCEPTED).message("")
//                .data(jobService.findById(uuid)).build());
//    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> read(@Valid @PathVariable(name = "id") UUID uuid) {
        return ResponseEntity.ok(jobService.findById(uuid));
    }
}
