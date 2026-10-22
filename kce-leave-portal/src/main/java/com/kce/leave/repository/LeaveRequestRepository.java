package com.kce.leave.repository;

import com.kce.leave.model.LeaveRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface LeaveRequestRepository extends MongoRepository<LeaveRequest, String> {
    List<LeaveRequest> findByClassNameOrderByAppliedOnDesc(String className);
    List<LeaveRequest> findByStudentIdOrderByAppliedOnDesc(String studentId);
}
