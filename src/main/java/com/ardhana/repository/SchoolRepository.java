package com.ardhana.repository;

import com.ardhana.model.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SchoolRepository extends JpaRepository<School, String> {
    List<School> findAllByOrderByNameAsc();
    List<School> findByActiveOrderByNameAsc(Boolean active);
}