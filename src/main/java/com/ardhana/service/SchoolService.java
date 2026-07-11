package com.ardhana.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ardhana.dto.SchoolRequest;
import com.ardhana.model.AppRole;
import com.ardhana.model.School;
import com.ardhana.model.UserProfile;
import com.ardhana.repository.SchoolRepository;
import com.ardhana.repository.UserProfileRepository;
import com.ardhana.repository.UserRoleRepository;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    
    public SchoolService(SchoolRepository schoolRepository,
                         UserProfileRepository userProfileRepository, UserRoleRepository userRoleRepository) {
        this.schoolRepository = schoolRepository;
        this.userProfileRepository = userProfileRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public List<School> getAllSchools() {
        return schoolRepository.findAllByOrderByNameAsc();
    }

    public School getSchool(String id) {
        return schoolRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("School not found"));
    }

    public School createSchool(SchoolRequest req) {
        School school = new School();
        applyRequest(school, req);
        return schoolRepository.save(school);
    }

    public School updateSchool(String id, SchoolRequest req) {
        School school = getSchool(id);
        applyRequest(school, req);
        return schoolRepository.save(school);
    }

    public void deleteSchool(String id) {
        // Unlink students before deleting
        List<UserProfile> students = userProfileRepository.findBySchoolId(id);
        students.forEach(s -> s.setSchoolId(null));
        userProfileRepository.saveAll(students);
        schoolRepository.deleteById(id);
    }

    public void assignStudentToSchool(String supabaseUid, String schoolId) {
        UserProfile profile = userProfileRepository.findBySupabaseUid(supabaseUid)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!schoolId.equals("none")) {
            getSchool(schoolId); // validate school exists
            profile.setSchoolId(schoolId);
        } else {
            profile.setSchoolId(null);
        }
        userProfileRepository.save(profile);
    }

    public List<UserProfile> getStudentsBySchool(String schoolId) {
        return userProfileRepository.findBySchoolId(schoolId);
    }

    private void applyRequest(School school, SchoolRequest req) {
        if (req.getName() != null) school.setName(req.getName());
        if (req.getAddress() != null) school.setAddress(req.getAddress());
        if (req.getCity() != null) school.setCity(req.getCity());
        if (req.getState() != null) school.setState(req.getState());
        if (req.getCountry() != null) school.setCountry(req.getCountry());
        if (req.getPhone() != null) school.setPhone(req.getPhone());
        if (req.getEmail() != null) school.setEmail(req.getEmail());
        if (req.getActive() != null) school.setActive(req.getActive());
    }
    
    /** Unassigned users eligible to add to a school — role=student only, not admins/teachers. */
    public List<UserProfile> getAllStudents() {
        Set<String> studentUserIds = userRoleRepository.findAllByRole(AppRole.student)
            .stream()
            .map(role -> role.getUserId().toString())
            .collect(Collectors.toSet());

        return userProfileRepository.findAll()
            .stream()
            .filter(u -> u.getSchoolId() == null
                && u.getSupabaseUid() != null
                && studentUserIds.contains(u.getSupabaseUid()))
            .collect(Collectors.toList());
    }
}