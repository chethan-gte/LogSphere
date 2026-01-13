package com.example.demo.repository;

import com.example.demo.model.Employee;
import com.example.demo.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByEmployee(Employee employee);
    
    List<Task> findByEmployeeAndStatus(Employee employee, String status);
    
    List<Task> findByEmployeeAndAssignedDate(Employee employee, LocalDate date);
    
    @Query("SELECT t FROM Task t WHERE t.employee = :employee AND t.assignedDate = :date ORDER BY t.priority DESC, t.createdAt DESC")
    List<Task> findTodayTasksByEmployee(Employee employee, LocalDate date);
    
    List<Task> findByEmployeeAndStatusNot(Employee employee, String status);
}

