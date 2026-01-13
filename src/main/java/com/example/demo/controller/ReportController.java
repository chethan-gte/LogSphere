package com.example.demo.controller;

import com.example.demo.model.Attendance;
import com.example.demo.model.Visitor;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.VisitorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private VisitorRepository visitorRepository;

    @GetMapping
    public String reportsDashboard(Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        
        // Today's stats
        List<Attendance> todayAttendance = attendanceRepository.findByAttendanceDate(today);
        List<Visitor> todayVisitors = visitorRepository.findByCheckInTimeBetween(
            LocalDateTime.of(today, java.time.LocalTime.MIN),
            LocalDateTime.of(today, java.time.LocalTime.MAX)
        );
        
        // Weekly stats
        List<Attendance> weeklyAttendance = attendanceRepository.findByDateRange(startOfWeek, today);
        List<Visitor> weeklyVisitors = visitorRepository.findByCheckInTimeBetween(
            startOfWeek.atStartOfDay(),
            LocalDateTime.of(today, java.time.LocalTime.MAX)
        );
        
        // Monthly stats
        List<Attendance> monthlyAttendance = attendanceRepository.findByDateRange(startOfMonth, today);
        List<Visitor> monthlyVisitors = visitorRepository.findByCheckInTimeBetween(
            startOfMonth.atStartOfDay(),
            LocalDateTime.of(today, java.time.LocalTime.MAX)
        );
        
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("todayVisitors", todayVisitors);
        model.addAttribute("weeklyAttendance", weeklyAttendance);
        model.addAttribute("weeklyVisitors", weeklyVisitors);
        model.addAttribute("monthlyAttendance", monthlyAttendance);
        model.addAttribute("monthlyVisitors", monthlyVisitors);
        
        // Calculate totals
        double todayTotalHours = todayAttendance.stream()
            .mapToDouble(a -> a.getTotalHours() != null ? a.getTotalHours() : 0.0)
            .sum();
        double weeklyTotalHours = weeklyAttendance.stream()
            .mapToDouble(a -> a.getTotalHours() != null ? a.getTotalHours() : 0.0)
            .sum();
        double monthlyTotalHours = monthlyAttendance.stream()
            .mapToDouble(a -> a.getTotalHours() != null ? a.getTotalHours() : 0.0)
            .sum();
        
        model.addAttribute("todayTotalHours", todayTotalHours);
        model.addAttribute("weeklyTotalHours", weeklyTotalHours);
        model.addAttribute("monthlyTotalHours", monthlyTotalHours);
        
        return "reports-dashboard";
    }

    @GetMapping("/employee")
    public String employeeReport(@RequestParam(required = false) String period,
                                @RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate,
                                Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        LocalDate start;
        LocalDate end = LocalDate.now();
        
        if ("daily".equals(period)) {
            start = end;
        } else if ("weekly".equals(period)) {
            start = end.minusDays(end.getDayOfWeek().getValue() - 1);
        } else if ("monthly".equals(period)) {
            start = end.withDayOfMonth(1);
        } else if (startDate != null && endDate != null) {
            start = LocalDate.parse(startDate);
            end = LocalDate.parse(endDate);
        } else {
            start = end;
        }
        
        List<Attendance> attendanceList = attendanceRepository.findByDateRange(start, end);
        Map<String, Double> employeeHours = new HashMap<>();
        
        for (Attendance att : attendanceList) {
            String empName = att.getEmployee().getName();
            double hours = att.getTotalHours() != null ? att.getTotalHours() : 0.0;
            employeeHours.put(empName, employeeHours.getOrDefault(empName, 0.0) + hours);
        }
        
        model.addAttribute("attendanceList", attendanceList);
        model.addAttribute("employeeHours", employeeHours);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("period", period);
        
        return "employee-report";
    }

    @GetMapping("/visitor")
    public String visitorReport(@RequestParam(required = false) String period,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate,
                               Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        
        LocalDate start;
        LocalDate end = LocalDate.now();
        
        if ("daily".equals(period)) {
            start = end;
        } else if ("weekly".equals(period)) {
            start = end.minusDays(end.getDayOfWeek().getValue() - 1);
        } else if ("monthly".equals(period)) {
            start = end.withDayOfMonth(1);
        } else if (startDate != null && endDate != null) {
            start = LocalDate.parse(startDate);
            end = LocalDate.parse(endDate);
        } else {
            start = end;
        }
        
        List<Visitor> visitors = visitorRepository.findByCheckInTimeBetween(
            start.atStartOfDay(),
            end.atTime(23, 59, 59)
        );
        
        Map<String, Long> visitorFlow = new HashMap<>();
        for (Visitor v : visitors) {
            String date = v.getCheckInTime().toLocalDate().toString();
            visitorFlow.put(date, visitorFlow.getOrDefault(date, 0L) + 1);
        }
        
        model.addAttribute("visitors", visitors);
        model.addAttribute("visitorFlow", visitorFlow);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("period", period);
        
        return "visitor-report";
    }
}

