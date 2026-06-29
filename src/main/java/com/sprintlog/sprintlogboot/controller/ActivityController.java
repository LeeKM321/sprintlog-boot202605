package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.aspect.LogExecutionTime;
import com.sprintlog.sprintlogboot.domain.*;
import com.sprintlog.sprintlogboot.dto.request.UpdateActivityRequest;
import com.sprintlog.sprintlogboot.dto.response.ActivityResponse;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.dto.request.CreateActivityRequest;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import com.sprintlog.sprintlogboot.repository.UserRepository;
import com.sprintlog.sprintlogboot.service.ActivityDashboard;
import com.sprintlog.sprintlogboot.service.FileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping({"/api/v1/activities", "/api/activities"}) // 경로를 둘로 받아서 기존의 요청도 해결할 수 있도록.
@Tag(name = "활동(Activity)", description = "학습 활동 조회, 생성, 수정, 삭제 API")
public class ActivityController implements ActivityControllerDocs {

    private final ActivityRepository repository;
    private final ActivityDashboard dashboard;
    private final FileService fileService;

    private final UserRepository userRepository;

    // 모든 활동 목록(페이징)
    @GetMapping
    public ResponseEntity<List<EntityModel<ActivityResponse>>> getAll(
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long ownerId
    ) {
        Comparator<LearningActivity> comparator = switch (sort) {
            case "minutes" -> Comparator.comparingInt(LearningActivity::getMinutes);
            case "title" -> Comparator.comparing(LearningActivity::getTitle);
            default -> Comparator.comparing(LearningActivity::getId);
        };

        List<LearningActivity> source
                = (ownerId != null) ? repository.findByOwnerId(ownerId) : repository.findAll();


        List<EntityModel<ActivityResponse>> list = source.stream()
                .sorted(comparator)
                .skip((long) page * size) // 0페이지면 0개 건너뛰고 size개, 1페이지면 size개 건너뛰고 size개
                .limit(size)
                .map(this::toModel)
                .toList();

        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/{id}")
    @LogExecutionTime
    public ResponseEntity<EntityModel<ActivityResponse>> getById(@PathVariable Long id) {
        LearningActivity activity = repository.findById(id)
                .orElseThrow(() -> new ActivityNotFoundException(id));
        return ResponseEntity.ok().body(toModel(activity));
    }

    // 카테고리별로 그룹화된 활동 목록
    @GetMapping("/dashboard")
    public ResponseEntity<Map<ActivityCategory, List<LearningActivity>>> getDashboard() {
        Map<ActivityCategory, List<LearningActivity>> map = dashboard.groupByCategory();
        return ResponseEntity.ok().body(map);
    }


    // 활동 수 요약 정보 (전체 / 강의 / 실습 / 독서) -> ActivityDashboard
    @RequestMapping(value = "/summary", method = RequestMethod.GET)
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
        return ResponseEntity.ok()
                .header("Deprecation", "true")
                .header("Sunset", "Thu, 31 Dec 2026 23:59:59 GMT")
                .header("Link",
                        "<https://docs.sprintlog.example/guides/migration#search>; rel=\"deprecation\"")

                .body(list);
    }

    //  변경 작업: -- 생성(POST) / 수정(PUT) / 삭제(DELETE) ---
    @PostMapping
    public ResponseEntity<EntityModel<ActivityResponse>> create(
            @Valid @RequestPart("data") CreateActivityRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        LearningActivity activity = toActivity(request);

        if (file != null && !file.isEmpty()) {
            String savedFileName = fileService.saveFile(file);
            activity.attachFile(savedFileName);
        }

        LearningActivity saved = repository.save(activity);

        // 성공 시 201 Created + Location 헤더(생성된 자원의 주소)를 함께 응답한다.
        URI location = URI.create("/api/activities/" + saved.getId());
        return ResponseEntity.created(location).body(toModel(activity));
    }

    // 활동 수정. 자원 식별은 Path(/{id}), 변경할 내용은 본문(UpdateActivityRequest)
    // 대상이 없으면 404, 있으면 제목, 공개여부를 변경하고 200.
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<ActivityResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateActivityRequest request) {

        LearningActivity activity = repository.findById(id)
                .orElseThrow(() -> new ActivityNotFoundException(id));

        activity.changeTitle(request.title());
        if (request.visibility() == Visibility.PUBLIC) {
            activity.openToPublic();
        } else {
            activity.hideFromPublic();
        }
        // JPA가 적용된 상태에서의 update는 findById로 조회해 온 Entity를 setter로 변경
        // 변경 후 명시적으로 save()를 호출하면 영속성 컨텍스트의 변경 감지(dirty checking)에 의해 update 쿼리가 날아감
        repository.save(activity);
        return ResponseEntity.ok().body(toModel(activity));
    }


    // 활동 삭제. 성공 시 본문 없이 204 No Content, 대상이 없으면 404.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // 해당 id에 대한 데이터 존재 여부 확인
        if (!repository.existsById(id)) {
            throw new ActivityNotFoundException(id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- 응답 DTO + HATEOAS 링크 만들기 --------------------------------------------
    private EntityModel<ActivityResponse> toModel(LearningActivity activity) {
        long id = activity.getId();
        return EntityModel.of(
                ActivityResponse.from(activity),
                linkTo(methodOn(ActivityController.class).getById(id)).withSelfRel(),
                linkTo(ActivityController.class).withRel("activities"),
                linkTo(methodOn(ActivityTagController.class).getTags(id)).withRel("tags")
        );
    }

    // 평탄화 후 — 하위 타입 생성 switch 가 사라졌다.
    // 종류(type)와 종류별 필드를 그대로 단일 생성자에 넘기면 된다(엔티티가 category 로 구분).
    private LearningActivity toActivity(CreateActivityRequest request) {
        LearningActivity activity = new LearningActivity(
                request.type(), request.title(), request.minutes(), request.visibility(),
                request.instructorName(), request.completionRate(), request.bookTitle());

        if (request.tags() != null) {
            request.tags().forEach(activity::addTag);
        }
        return activity;
    }


    @GetMapping("test-find")
    public void testFindByUserId(@RequestParam Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user 정보 없음"));

        log.info("찾은 user의 정보: {}", user.getNickname());

        // 실제 user 테이블에는 activities에 대한 정보는 아예 없음. 하지만 User 클래스에는 활동 List가 존재함
        // 그래서 JPA는 getActivities()를 호출하면 해당 user_id로 활동들을 조회해서 User 객체 안에 추가함.
        // 활동 List에 객체를 add하거나 delete한다고 해서 그것이 DB에 반영되지는 않는다는 점! (연관관계의 주인이 아님!)
        // 조회 용도로 사용하는 것은 전혀 문제가 없지만 가상 컬럼을 조작하면 실제 DB와 괴리가 발생한다는 점을 알고 계세요.
        // 실제로 단방향 연관관계를 매핑하고 나서 양방향은 추후에 고려합니다. (필요하면 구현하고, 아니라면 단방향으로만 유지)
        List<LearningActivity> activities = user.getActivities();
        for (LearningActivity activity : activities) {
            log.info("활동 객체 제목: {}", activity.getTitle());
        }

    }


}











