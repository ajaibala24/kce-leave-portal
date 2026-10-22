package com.kce.leave.repository;

import com.kce.leave.model.Teacher;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface TeacherRepository extends MongoRepository<Teacher, String> {
    Optional<Teacher> findByClassName(String className);
}
