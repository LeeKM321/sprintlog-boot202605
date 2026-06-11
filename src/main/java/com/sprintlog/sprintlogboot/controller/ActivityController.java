package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import com.sprintlog.sprintlogboot.service.ActivityDashboard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityRepository repository;
    private final ActivityDashboard dashboard;

    // 모든 활동 목록(페이징)
    @GetMapping
    public ResponseEntity<List<LearningActivity>> getAll(
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        Comparator<LearningActivity> comparator = switch (sort) {
            case "minutes" -> Comparator.comparingInt(LearningActivity::getMinutes);
            case "title" -> Comparator.comparing(LearningActivity::getTitle);
            default -> Comparator.comparing(LearningActivity::getId);
        };


        List<LearningActivity> list = repository.findAll().stream()
                .sorted(comparator)
                .skip((long) page * size) // 0페이지면 0개 건너뛰고 size개, 1페이지면 size개 건너뛰고 size개
                .limit(size)
                .toList();

        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LearningActivity> getById(@PathVariable Long id) {
        Optional<LearningActivity> first = repository.findFirst(activity -> activity.getId() == id);
        if (first.isPresent()) {
            return ResponseEntity.ok().body(first.get());
        }
        return ResponseEntity.notFound().build();
    }

    // 카테고리별로 그룹화된 활동 목록
    @GetMapping("/dashboard")
    public ResponseEntity<Map<ActivityCategory, List<LearningActivity>>> getDashboard() {
        Map<ActivityCategory, List<LearningActivity>> map = dashboard.groupByCategory();
        return ResponseEntity.ok().body(map);
    }


    // 활동 수 요약 정보 (전체 / 강의 / 실습 / 독서) -> ActivityDashboard
    @GetMapping("/summary")
    public ResponseEntity<ActivityDashboard.Summary> getSummary() {
        return ResponseEntity.ok().body(dashboard.summarize());
    }

    // 태그로 활동을 필터링
    @GetMapping("/search")
    public ResponseEntity<List<LearningActivity>> searchByTag(@RequestParam String tag,
                                                              @RequestParam String name,
                                                              @RequestParam int age) {

        log.info("RequestParam을 통해 얻어낸 값: {}, {}, {}", tag, name, age);

        List<LearningActivity> list = dashboard.filterByTag(tag);
        return ResponseEntity.ok().body(list);
    }


}











