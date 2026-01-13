package com.example.demo.repository;

import com.example.demo.model.Employee;
import com.example.demo.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    
    List<Payroll> findByEmployee(Employee employee);
    
    List<Payroll> findByStatus(String status);
    
    @Query("SELECT p FROM Payroll p WHERE p.employee = :employee AND p.payPeriodStart <= :date AND p.payPeriodEnd >= :date")
    Optional<Payroll> findPayrollForEmployeeInPeriod(@Param("employee") Employee employee, @Param("date") LocalDate date);
    
    @Query("SELECT p FROM Payroll p WHERE p.payPeriodStart BETWEEN :startDate AND :endDate OR p.payPeriodEnd BETWEEN :startDate AND :endDate")
    List<Payroll> findPayrollsInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

