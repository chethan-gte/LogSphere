package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "pay_period_start", nullable = false)
    private LocalDate payPeriodStart;

    @Column(name = "pay_period_end", nullable = false)
    private LocalDate payPeriodEnd;

    @Column(name = "base_salary", nullable = false)
    private Double baseSalary;

    @Column(name = "bonus")
    private Double bonus = 0.0;

    @Column(name = "incentive")
    private Double incentive = 0.0;

    @Column(name = "overtime_pay")
    private Double overtimePay = 0.0;

    @Column(name = "allowances")
    private Double allowances = 0.0;

    @Column(name = "tax_deduction")
    private Double taxDeduction = 0.0;

    @Column(name = "other_deductions")
    private Double otherDeductions = 0.0;

    @Column(name = "net_salary", nullable = false)
    private Double netSalary;

    @Column(name = "status")
    private String status = "PENDING"; // PENDING, PROCESSED, PAID

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payslip_generated")
    private Boolean payslipGenerated = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        calculateNetSalary();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateNetSalary();
    }

    private void calculateNetSalary() {
        if (baseSalary == null) {
            return; // Don't calculate if baseSalary is not set yet
        }
        double gross = (baseSalary != null ? baseSalary : 0.0) + 
                      (bonus != null ? bonus : 0.0) + 
                      (incentive != null ? incentive : 0.0) + 
                      (overtimePay != null ? overtimePay : 0.0) + 
                      (allowances != null ? allowances : 0.0);
        netSalary = gross - (taxDeduction != null ? taxDeduction : 0.0) - (otherDeductions != null ? otherDeductions : 0.0);
    }

    // Constructors
    public Payroll() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getPayPeriodStart() {
        return payPeriodStart;
    }

    public void setPayPeriodStart(LocalDate payPeriodStart) {
        this.payPeriodStart = payPeriodStart;
    }

    public LocalDate getPayPeriodEnd() {
        return payPeriodEnd;
    }

    public void setPayPeriodEnd(LocalDate payPeriodEnd) {
        this.payPeriodEnd = payPeriodEnd;
    }

    public Double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(Double baseSalary) {
        this.baseSalary = baseSalary;
    }

    public Double getBonus() {
        return bonus;
    }

    public void setBonus(Double bonus) {
        this.bonus = bonus;
    }

    public Double getIncentive() {
        return incentive;
    }

    public void setIncentive(Double incentive) {
        this.incentive = incentive;
    }

    public Double getOvertimePay() {
        return overtimePay;
    }

    public void setOvertimePay(Double overtimePay) {
        this.overtimePay = overtimePay;
    }

    public Double getAllowances() {
        return allowances;
    }

    public void setAllowances(Double allowances) {
        this.allowances = allowances;
    }

    public Double getTaxDeduction() {
        return taxDeduction;
    }

    public void setTaxDeduction(Double taxDeduction) {
        this.taxDeduction = taxDeduction;
    }

    public Double getOtherDeductions() {
        return otherDeductions;
    }

    public void setOtherDeductions(Double otherDeductions) {
        this.otherDeductions = otherDeductions;
    }

    public Double getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(Double netSalary) {
        this.netSalary = netSalary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Boolean getPayslipGenerated() {
        return payslipGenerated;
    }

    public void setPayslipGenerated(Boolean payslipGenerated) {
        this.payslipGenerated = payslipGenerated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

