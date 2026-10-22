package com.kce.leave.repository;

import com.kce.leave.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {
    List<Student> findByNameIgnoreCase(String name);
    Optional<Student> findByRegisterNoIgnoreCase(String registerNo);
    boolean existsByRegisterNo(String registerNo);
}
