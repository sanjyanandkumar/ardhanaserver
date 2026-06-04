package com.ardhana.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ardhana.dto.SchoolRequest;
import com.ardhana.model.School;
import com.ardhana.model.UserProfile;
import com.ardhana.service.SchoolService;

@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    // ← Must be BEFORE /{id} to avoid path conflict
    @GetMapping("/students/all")
    public ResponseEntity<List<UserProfile>> getAllStudents() {
        return ResponseEntity.ok(schoolService.getAllStudents());
    }

    @GetMapping
    public ResponseEntity<List<School>> getAllSchools() {
        return ResponseEntity.ok(schoolService.getAllSchools());
    }

    @GetMapping("/{id}")
    public ResponseEntity<School> getSchool(@PathVariable String id) {
        return ResponseEntity.ok(schoolService.getSchool(id));
    }

    @PostMapping
    public ResponseEntity<School> createSchool(@RequestBody SchoolRequest req) {
        return ResponseEntity.ok(schoolService.createSchool(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<School> updateSchool(@PathVariable String id,
                                                @RequestBody SchoolRequest req) {
        return ResponseEntity.ok(schoolService.updateSchool(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchool(@PathVariable String id) {
        schoolService.deleteSchool(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<List<UserProfile>> getStudents(@PathVariable String id) {
        return ResponseEntity.ok(schoolService.getStudentsBySchool(id));
    }

    @PutMapping("/{schoolId}/students/{supabaseUid}")
    public ResponseEntity<Void> assignStudent(@PathVariable String schoolId,
                                               @PathVariable String supabaseUid) {
        schoolService.assignStudentToSchool(supabaseUid, schoolId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{schoolId}/students/{supabaseUid}")
    public ResponseEntity<Void> removeStudent(@PathVariable String schoolId,
                                               @PathVariable String supabaseUid) {
        schoolService.assignStudentToSchool(supabaseUid, "none");
        return ResponseEntity.ok().build();
    }
}