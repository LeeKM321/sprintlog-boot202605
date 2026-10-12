package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.dto.response.StudyRoomResponse;
import com.sprintlog.sprintlogboot.repository.StudyRoomRepository;
import com.sprintlog.sprintlogboot.websocket.StudyRoomPresence;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/study-rooms")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomRepository repository;
    private final StudyRoomPresence presence;

    @GetMapping
    public List<StudyRoomResponse> list() {
        return repository.findAll().stream()
                .map(room -> StudyRoomResponse.of(room, presence.count(room.getId())))
                .toList();
    }

}
