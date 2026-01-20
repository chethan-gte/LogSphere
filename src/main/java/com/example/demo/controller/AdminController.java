package com.example.demo.controller;

import com.example.demo.model.Meeting;
import com.example.demo.model.Visitor;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.MeetingRepository;
import com.example.demo.repository.VisitorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private VisitorRepository visitorRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private com.example.demo.repository.IdleIncidentRepository idleIncidentRepository;

    @Autowired
    private com.example.demo.repository.PayrollRepository payrollRepository;

    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public String adminDashboard(Model model, HttpSession session) {
        // Check if user is logged in
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        // Get visitor statistics
        List<Visitor> allVisitors = visitorRepository.findAllOrderByCheckInTimeDesc();
        List<Visitor> checkedInVisitors = visitorRepository.findByStatus("Checked In");
        List<Visitor> checkedOutVisitors = visitorRepository.findByStatus("Checked Out");

        // Get employee statistics
        List<com.example.demo.model.Employee> presentEmployees = employeeRepository.findCurrentlyPresentEmployees();
        List<com.example.demo.model.Employee> allEmployees = employeeRepository.findAll();
        long totalEmployees = allEmployees.size();
        long presentCount = presentEmployees.size();
        long absentCount = totalEmployees - presentCount;

        // Get meeting statistics
        List<Meeting> ongoingMeetings = meetingRepository.findOngoingMeetings();
        List<Meeting> allMeetings = meetingRepository.findAll();

        model.addAttribute("userName", session.getAttribute("userName"));

        // Visitor stats
        model.addAttribute("allVisitors", allVisitors);
        model.addAttribute("checkedInVisitors", checkedInVisitors);
        model.addAttribute("checkedOutVisitors", checkedOutVisitors);
        model.addAttribute("totalVisitors", allVisitors.size());
        model.addAttribute("currentlyIn", checkedInVisitors.size());
        model.addAttribute("totalCheckedOut", checkedOutVisitors.size());

        // Employee stats
        model.addAttribute("presentEmployees", presentEmployees);
        model.addAttribute("allEmployees", allEmployees);
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("absentCount", absentCount);

        // Meeting stats
        model.addAttribute("ongoingMeetings", ongoingMeetings);
        model.addAttribute("totalMeetings", allMeetings.size());
        model.addAttribute("ongoingCount", ongoingMeetings.size());

        // Get idle employees
        List<com.example.demo.model.IdleIncident> activeIdleIncidents = idleIncidentRepository.findActiveIncidents();
        model.addAttribute("idleIncidents", activeIdleIncidents);
        model.addAttribute("idleCount", activeIdleIncidents.size());

        return "admin-dashboard";
    }

    @RequestMapping(value = "/payroll", method = RequestMethod.GET)
    public String adminPayroll(Model model, HttpSession session) {
        // Check if user is logged in
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        List<com.example.demo.model.Payroll> allPayrolls = payrollRepository.findAll();
        List<com.example.demo.model.Payroll> processedPayrolls = payrollRepository.findByStatus("PROCESSED");

        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("payrolls", allPayrolls); // Using allPayrolls for the list as per typical admin view, but
                                                     // user image shows specific rows. The user image shows "Payroll
                                                     // History", implying all.
        model.addAttribute("processedCount", processedPayrolls.size());

        return "admin-payroll";
    }

    @RequestMapping(value = "/api/employees", method = RequestMethod.GET)
    @org.springframework.web.bind.annotation.ResponseBody
    public List<com.example.demo.model.Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @RequestMapping(value = "/api/employees/{id}", method = RequestMethod.GET)
    @org.springframework.web.bind.annotation.ResponseBody
    public com.example.demo.model.Employee getEmployeeDetails(
            @org.springframework.web.bind.annotation.PathVariable Long id) {
        return employeeRepository.findById(id).orElse(null);
    }

    @RequestMapping(value = "/payroll/save", method = RequestMethod.POST)
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> savePayroll(
            @org.springframework.web.bind.annotation.RequestParam Long employeeId,
            @org.springframework.web.bind.annotation.RequestParam Double baseSalary) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            com.example.demo.model.Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                response.put("success", false);
                response.put("message", "Employee not found");
                return response;
            }

            com.example.demo.model.Payroll payroll = new com.example.demo.model.Payroll();
            payroll.setEmployee(employee);
            payroll.setBaseSalary(baseSalary);

            // Set default pay period (Current Month)
            java.time.LocalDate now = java.time.LocalDate.now();
            payroll.setPayPeriodStart(now.withDayOfMonth(1));
            payroll.setPayPeriodEnd(now.withDayOfMonth(now.lengthOfMonth()));

            payroll.setStatus("PENDING");
            payroll.setPayslipGenerated(false);

            payrollRepository.save(payroll);

            response.put("success", true);
            response.put("message", "Payroll saved successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error saving payroll: " + e.getMessage());
        }
        return response;
    }
}
