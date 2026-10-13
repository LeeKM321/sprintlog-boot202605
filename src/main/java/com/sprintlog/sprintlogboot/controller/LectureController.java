package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.client.LectureCatalogClient;
import com.sprintlog.sprintlogboot.client.LectureInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final LectureCatalogClient lectureCatalog;

    @GetMapping("/{id}")
    public LectureInfo lecture(@PathVariable long id) {
        return lectureCatalog.find(id);
    }

}













