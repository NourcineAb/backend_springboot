package com.enterprise.poodle.controller;

import com.enterprise.poodle.dto.request.SectionRequest;
import com.enterprise.poodle.dto.response.SectionResponse;
import com.enterprise.poodle.service.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @PostMapping("/courses/{courseId}/sections")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SectionResponse> createSection(
            @PathVariable Long courseId,
            @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sectionService.createSection(courseId, request));
    }

    @GetMapping("/courses/{courseId}/sections")
    public ResponseEntity<List<SectionResponse>> getSectionsByCourse(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(sectionService.getSectionsByCourse(courseId));
    }

    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SectionResponse> updateSection(
            @PathVariable Long sectionId,
            @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.ok(sectionService.updateSection(sectionId, request));
    }

    @DeleteMapping("/sections/{sectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSection(@PathVariable Long sectionId) {
        sectionService.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }
}
