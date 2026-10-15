package fu.ats.service;

import fu.ats.dto.JobRequest;
import fu.ats.dto.JobResponse;

import java.util.List;
import java.util.UUID;

public interface JobService {
    JobResponse save(JobRequest request);

    JobResponse findById(UUID uuid);

    List<JobResponse> findAll();
}
