package com.earseo.core.controller.internal;

import com.earseo.core.dto.internal.StoryDocentRequest;
import com.earseo.core.dto.internal.StoryDocentResponse;
import com.earseo.core.service.internal.InternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CoreInternalController {

    private final InternalService internalService;

    @PostMapping("/internal/core/docent/story")
    public List<StoryDocentResponse> getStoryDocent(@RequestBody List<StoryDocentRequest> request) {
        return internalService.createStorySpotDocent(request);
    }
}
