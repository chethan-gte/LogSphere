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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @GetMapping("/dashboard")
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
}

