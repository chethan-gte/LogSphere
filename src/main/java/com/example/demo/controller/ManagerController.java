package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private VisitorRepository visitorRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private MeetingRepository meetingRepository;
    
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private FeedbackRepository feedbackRepository;
    
    @Autowired
    private TeamBadgeRepository teamBadgeRepository;
    
    @Autowired
    private TeamRuleRepository teamRuleRepository;
    
    @Autowired
    private IdleIncidentRepository idleIncidentRepository;
    
    @Autowired
    private SuggestionRepository suggestionRepository;
    
    @Autowired
    private EmployeeActivityRepository employeeActivityRepository;

    @GetMapping("/login")
    public String showManagerLoginForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && "MANAGER".equals(user.getRole())) {
            return "redirect:/manager/dashboard";
        }
        return "manager-login";
    }

    @PostMapping("/login")
    public String managerLogin(@RequestParam String email, 
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!"MANAGER".equals(user.getRole())) {
                redirectAttributes.addFlashAttribute("error", "Access denied. Manager login only.");
                return "redirect:/manager/login";
            }
            if (user.getPassword().equals(password)) {
                session.setAttribute("user", user);
                session.setAttribute("userName", user.getName());
                session.setAttribute("userRole", user.getRole());
                return "redirect:/manager/dashboard";
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid email or password!");
                return "redirect:/manager/login";
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password!");
            return "redirect:/manager/login";
        }
    }

    @GetMapping("/dashboard")
    public String managerDashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        
        // Get all employees
        List<Employee> allEmployees = employeeRepository.findAll();
        List<Employee> presentEmployees = employeeRepository.findCurrentlyPresentEmployees();
        
        // 1. Team Workload Distribution
        List<Task> allTasks = taskRepository.findAll();
        Map<Employee, Long> taskCountByEmployee = allTasks.stream()
            .filter(t -> !"COMPLETED".equals(t.getStatus()))
            .collect(Collectors.groupingBy(Task::getEmployee, Collectors.counting()));
        
        List<Employee> overloadedEmployees = new ArrayList<>();
        List<Employee> balancedEmployees = new ArrayList<>();
        List<Employee> underutilizedEmployees = new ArrayList<>();
        
        long avgTasks = taskCountByEmployee.isEmpty() ? 0 : 
            (long) taskCountByEmployee.values().stream().mapToLong(Long::longValue).average().orElse(0);
        
        for (Employee emp : allEmployees) {
            long taskCount = taskCountByEmployee.getOrDefault(emp, 0L);
            if (taskCount > avgTasks * 1.5) {
                overloadedEmployees.add(emp);
            } else if (taskCount < avgTasks * 0.5) {
                underutilizedEmployees.add(emp);
            } else {
                balancedEmployees.add(emp);
            }
        }
        
        // 2. Daily Team Planning Board - Tasks by priority and status
        List<Task> pendingTasks = allTasks.stream()
            .filter(t -> "PENDING".equals(t.getStatus()))
            .sorted(Comparator.comparing(Task::getPriority).reversed())
            .collect(Collectors.toList());
        
        List<Task> inProgressTasks = allTasks.stream()
            .filter(t -> "IN_PROGRESS".equals(t.getStatus()))
            .collect(Collectors.toList());
        
        List<Task> completedTasks = allTasks.stream()
            .filter(t -> "COMPLETED".equals(t.getStatus()))
            .collect(Collectors.toList());
        
        List<Task> overdueTasks = allTasks.stream()
            .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today) && !"COMPLETED".equals(t.getStatus()))
            .collect(Collectors.toList());
        
        // 3. Smart Task Reallocation Suggestions
        List<Map<String, Object>> reallocationSuggestions = new ArrayList<>();
        for (Task task : overdueTasks) {
            if (task.getEmployee() != null) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("task", task);
                suggestion.put("reason", "Task deadline is near");
                suggestion.put("suggestedEmployee", underutilizedEmployees.isEmpty() ? null : underutilizedEmployees.get(0));
                reallocationSuggestions.add(suggestion);
            }
        }
        
        // 4. Goals & Milestones
        List<Goal> teamGoals = goalRepository.findByGoalType("TEAM");
        List<Goal> onTrackGoals = teamGoals.stream().filter(g -> "ON_TRACK".equals(g.getStatus())).collect(Collectors.toList());
        List<Goal> atRiskGoals = teamGoals.stream().filter(g -> "AT_RISK".equals(g.getStatus())).collect(Collectors.toList());
        List<Goal> behindScheduleGoals = teamGoals.stream().filter(g -> "BEHIND_SCHEDULE".equals(g.getStatus())).collect(Collectors.toList());
        
        // 5. Team Forecast
        List<Task> upcomingTasks = allTasks.stream()
            .filter(t -> t.getDueDate() != null && t.getDueDate().isAfter(today) && !"COMPLETED".equals(t.getStatus()))
            .sorted(Comparator.comparing(Task::getDueDate))
            .limit(10)
            .collect(Collectors.toList());
        
        // 6. Feedback Console
        List<Feedback> allFeedbacks = feedbackRepository.findAll();
        Map<Employee, List<Feedback>> feedbacksByEmployee = allFeedbacks.stream()
            .collect(Collectors.groupingBy(Feedback::getEmployee));
        
        List<Feedback> upcomingFollowUps = allFeedbacks.stream()
            .filter(f -> f.getFollowUpDate() != null && 
                    f.getFollowUpDate().isAfter(now) && 
                    f.getFollowUpDate().isBefore(now.plusDays(7)))
            .sorted(Comparator.comparing(Feedback::getFollowUpDate))
            .collect(Collectors.toList());
        
        // 7. Team Availability Planner
        List<LeaveRequest> activeLeaves = leaveRequestRepository.findActiveLeavesOnDate(today);
        List<Meeting> todayMeetings = meetingRepository.findByDateRange(
            now.withHour(0).withMinute(0), 
            now.withHour(23).withMinute(59)
        );
        
        Map<Employee, String> employeeAvailability = new HashMap<>();
        for (Employee emp : allEmployees) {
            boolean onLeave = activeLeaves.stream().anyMatch(l -> l.getEmployee().getId().equals(emp.getId()));
            boolean inMeeting = todayMeetings.stream().anyMatch(m -> m.getOrganizer() != null && m.getOrganizer().getId().equals(emp.getId()));
            long highPriorityTasks = allTasks.stream()
                .filter(t -> t.getEmployee() != null && t.getEmployee().getId().equals(emp.getId()) && 
                        "HIGH".equals(t.getPriority()) && !"COMPLETED".equals(t.getStatus()))
                .count();
            
            if (onLeave) {
                employeeAvailability.put(emp, "ON_LEAVE");
            } else if (inMeeting) {
                employeeAvailability.put(emp, "IN_MEETING");
            } else if (highPriorityTasks > 0) {
                employeeAvailability.put(emp, "BUSY");
            } else {
                employeeAvailability.put(emp, "AVAILABLE");
            }
        }
        
        // 8. Behavioral Insights
        List<Attendance> recentAttendance = attendanceRepository.findByDateRange(today.minusDays(30), today);
        Map<Employee, Double> avgHoursByEmployee = recentAttendance.stream()
            .collect(Collectors.groupingBy(
                Attendance::getEmployee,
                Collectors.averagingDouble(Attendance::getTotalHours)
            ));
        
        // 9. Motivation & Recognition
        List<TeamBadge> allBadges = teamBadgeRepository.findAll();
        List<TeamBadge> recentBadges = allBadges.stream()
            .filter(b -> b.getIsVisible() != null && b.getIsVisible())
            .sorted(Comparator.comparing(TeamBadge::getAwardedDate).reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        // 10. Custom Team Rules
        List<TeamRule> activeRules = teamRuleRepository.findByIsActiveTrue();
        
        // Add all to model
        model.addAttribute("userName", user.getName());
        model.addAttribute("userRole", user.getRole());
        
        // Workload Distribution
        model.addAttribute("overloadedEmployees", overloadedEmployees);
        model.addAttribute("balancedEmployees", balancedEmployees);
        model.addAttribute("underutilizedEmployees", underutilizedEmployees);
        model.addAttribute("avgTasks", avgTasks);
        
        // Daily Planning
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("inProgressTasks", inProgressTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("allTasks", allTasks);
        
        // Task Reallocation
        model.addAttribute("reallocationSuggestions", reallocationSuggestions);
        
        // Goals
        model.addAttribute("teamGoals", teamGoals);
        model.addAttribute("onTrackGoals", onTrackGoals);
        model.addAttribute("atRiskGoals", atRiskGoals);
        model.addAttribute("behindScheduleGoals", behindScheduleGoals);
        
        // Forecast
        model.addAttribute("upcomingTasks", upcomingTasks);
        
        // Feedback
        model.addAttribute("feedbacksByEmployee", feedbacksByEmployee);
        model.addAttribute("upcomingFollowUps", upcomingFollowUps);
        model.addAttribute("allFeedbacks", allFeedbacks);
        
        // Availability
        model.addAttribute("employeeAvailability", employeeAvailability);
        model.addAttribute("activeLeaves", activeLeaves);
        model.addAttribute("todayMeetings", todayMeetings);
        
        // Behavioral Insights
        model.addAttribute("avgHoursByEmployee", avgHoursByEmployee);
        model.addAttribute("recentAttendance", recentAttendance);
        
        // Recognition
        model.addAttribute("recentBadges", recentBadges);
        
        // Rules
        model.addAttribute("activeRules", activeRules);
        
        // General
        model.addAttribute("allEmployees", allEmployees);
        model.addAttribute("presentEmployees", presentEmployees);
        
        // Idle employee notifications
        List<com.example.demo.model.IdleIncident> activeIdleIncidents = idleIncidentRepository.findActiveIncidents();
        model.addAttribute("idleIncidents", activeIdleIncidents);
        model.addAttribute("idleCount", activeIdleIncidents.size());
        
        // Employee Suggestions
        List<com.example.demo.model.Suggestion> allSuggestions = suggestionRepository.findAllByOrderByCreatedAtDesc();
        List<com.example.demo.model.Suggestion> pendingSuggestions = suggestionRepository.findByStatusOrderByCreatedAtDesc("PENDING");
        List<com.example.demo.model.Suggestion> managerSuggestions = suggestionRepository.findBySuggestionTypeOrderByCreatedAtDesc("MANAGER");
        model.addAttribute("allSuggestions", allSuggestions);
        model.addAttribute("pendingSuggestions", pendingSuggestions);
        model.addAttribute("managerSuggestions", managerSuggestions);
        model.addAttribute("suggestionCount", pendingSuggestions.size());
        
        // Non-Productive Activities (Last 24 hours)
        LocalDateTime last24Hours = now.minusHours(24);
        List<com.example.demo.model.EmployeeActivity> nonProductiveActivities = employeeActivityRepository.findNonProductiveActivities(last24Hours);
        List<com.example.demo.model.EmployeeActivity> gamingActivities = employeeActivityRepository.findByActivityTypeAndStartedAtAfter("GAMING", last24Hours);
        List<com.example.demo.model.EmployeeActivity> socialMediaActivities = employeeActivityRepository.findByActivityTypeAndStartedAtAfter("SOCIAL_MEDIA", last24Hours);
        List<com.example.demo.model.EmployeeActivity> videoActivities = employeeActivityRepository.findByActivityTypeAndStartedAtAfter("VIDEO", last24Hours);
        
        // Group by employee
        Map<Employee, List<com.example.demo.model.EmployeeActivity>> nonProductiveByEmployee = nonProductiveActivities.stream()
            .collect(Collectors.groupingBy(com.example.demo.model.EmployeeActivity::getEmployee));
        
        model.addAttribute("nonProductiveActivities", nonProductiveActivities);
        model.addAttribute("gamingActivities", gamingActivities);
        model.addAttribute("socialMediaActivities", socialMediaActivities);
        model.addAttribute("videoActivities", videoActivities);
        model.addAttribute("nonProductiveByEmployee", nonProductiveByEmployee);
        model.addAttribute("nonProductiveCount", nonProductiveActivities.size());
        
        // Add task model for form
        model.addAttribute("task", new Task());
        
        return "manager-dashboard";
    }
    
    @RequestMapping(value = "/tasks/add", method = RequestMethod.POST)
    public String addTaskForEmployee(@RequestParam String title,
                                    @RequestParam(required = false) String description,
                                    @RequestParam String priority,
                                    @RequestParam(required = false) String dueDate,
                                    @RequestParam Long employeeId,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Employee not found!");
            return "redirect:/manager/dashboard";
        }
        
        Employee employee = employeeOpt.get();
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setEmployee(employee);
        task.setAssignedDate(LocalDate.now());
        task.setStatus("PENDING");
        task.setProgressPercentage(0);
        
        // Parse due date if provided
        if (dueDate != null && !dueDate.isEmpty()) {
            try {
                task.setDueDate(LocalDate.parse(dueDate));
            } catch (Exception e) {
                // Invalid date format, ignore
            }
        }
        
        taskRepository.save(task);
        
        redirectAttributes.addFlashAttribute("success", "Task assigned to " + employee.getName() + " successfully!");
        return "redirect:/manager/dashboard";
    }
    
    @GetMapping("/feedback/add")
    public String showAddFeedbackForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        List<Employee> allEmployees = employeeRepository.findAll();
        model.addAttribute("employees", allEmployees);
        
        return "manager-add-feedback";
    }
    
    @PostMapping("/feedback/add")
    public String addFeedback(@RequestParam Long employeeId,
                             @RequestParam String notes,
                             @RequestParam(required = false) String improvementSuggestions,
                             @RequestParam(required = false) String followUpDate,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        try {
            Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
            if (!employeeOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Employee not found!");
                return "redirect:/manager/feedback/add";
            }
            
            Feedback feedback = new Feedback();
            feedback.setEmployee(employeeOpt.get());
            Optional<User> managerUser = userRepository.findById(user.getId());
            managerUser.ifPresent(feedback::setManager);
            feedback.setNotes(notes);
            feedback.setImprovementSuggestions(improvementSuggestions);
            
            if (followUpDate != null && !followUpDate.isEmpty()) {
                feedback.setFollowUpDate(LocalDate.parse(followUpDate).atStartOfDay());
            }
            
            feedbackRepository.save(feedback);
            
            redirectAttributes.addFlashAttribute("success", "Feedback added successfully!");
            return "redirect:/manager/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding feedback: " + e.getMessage());
            return "redirect:/manager/feedback/add";
        }
    }
    
    @GetMapping("/badge/award")
    public String showAwardBadgeForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        List<Employee> allEmployees = employeeRepository.findAll();
        model.addAttribute("employees", allEmployees);
        
        return "manager-award-badge";
    }
    
    @PostMapping("/badge/award")
    public String awardBadge(@RequestParam Long employeeId,
                            @RequestParam String badgeName,
                            @RequestParam(required = false) String description,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        try {
            Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
            if (!employeeOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Employee not found!");
                return "redirect:/manager/badge/award";
            }
            
            TeamBadge badge = new TeamBadge();
            badge.setEmployee(employeeOpt.get());
            badge.setBadgeName(badgeName);
            badge.setAppreciationNote(description != null ? description : "");
            Optional<User> managerUser = userRepository.findById(user.getId());
            managerUser.ifPresent(badge::setAwardedBy);
            badge.setAwardedDate(LocalDateTime.now());
            badge.setIsVisible(true);
            
            teamBadgeRepository.save(badge);
            
            redirectAttributes.addFlashAttribute("success", "Badge awarded successfully!");
            return "redirect:/manager/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error awarding badge: " + e.getMessage());
            return "redirect:/manager/badge/award";
        }
    }
    
    @GetMapping("/feedback/view/{id}")
    public String viewFeedback(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        Optional<Feedback> feedbackOpt = feedbackRepository.findById(id);
        if (!feedbackOpt.isPresent()) {
            return "redirect:/manager/dashboard";
        }
        
        model.addAttribute("feedback", feedbackOpt.get());
        return "manager-view-feedback";
    }
    
    @GetMapping("/rule/create")
    public String showCreateRuleForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        return "manager-create-rule";
    }
    
    @PostMapping("/rule/create")
    public String createRule(@RequestParam String ruleName,
                           @RequestParam String description,
                           @RequestParam String ruleType,
                           @RequestParam(required = false) String value,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        try {
            TeamRule rule = new TeamRule();
            rule.setRuleName(ruleName);
            rule.setDescription(description);
            rule.setRuleType(ruleType);
            rule.setRuleValue(value);
            rule.setIsActive(true);
            
            Optional<User> managerUser = userRepository.findById(user.getId());
            managerUser.ifPresent(rule::setCreatedBy);
            
            teamRuleRepository.save(rule);
            
            redirectAttributes.addFlashAttribute("success", "Rule created successfully!");
            return "redirect:/manager/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating rule: " + e.getMessage());
            return "redirect:/manager/rule/create";
        }
    }
    
    @GetMapping("/rule/edit/{id}")
    public String showEditRuleForm(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        Optional<TeamRule> ruleOpt = teamRuleRepository.findById(id);
        if (!ruleOpt.isPresent()) {
            return "redirect:/manager/dashboard";
        }
        
        model.addAttribute("rule", ruleOpt.get());
        return "manager-edit-rule";
    }
    
    @PostMapping("/rule/update/{id}")
    public String updateRule(@PathVariable Long id,
                           @RequestParam String ruleName,
                           @RequestParam String description,
                           @RequestParam String ruleType,
                           @RequestParam(required = false) String value,
                           @RequestParam(required = false, defaultValue = "false") String isActive,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        try {
            Optional<TeamRule> ruleOpt = teamRuleRepository.findById(id);
            if (!ruleOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Rule not found!");
                return "redirect:/manager/dashboard";
            }
            
            TeamRule rule = ruleOpt.get();
            rule.setRuleName(ruleName);
            rule.setDescription(description);
            rule.setRuleType(ruleType);
            rule.setRuleValue(value);
            // Checkbox sends "true" if checked, null if unchecked
            rule.setIsActive("true".equals(isActive));
            
            teamRuleRepository.save(rule);
            
            redirectAttributes.addFlashAttribute("success", "Rule updated successfully!");
            return "redirect:/manager/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating rule: " + e.getMessage());
            return "redirect:/manager/rule/edit/" + id;
        }
    }
    
    @PostMapping("/rule/delete/{id}")
    public String deleteRule(@PathVariable Long id,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        try {
            Optional<TeamRule> ruleOpt = teamRuleRepository.findById(id);
            if (!ruleOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Rule not found!");
                return "redirect:/manager/dashboard";
            }
            
            teamRuleRepository.delete(ruleOpt.get());
            
            redirectAttributes.addFlashAttribute("success", "Rule deleted successfully!");
            return "redirect:/manager/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting rule: " + e.getMessage());
            return "redirect:/manager/dashboard";
        }
    }
    
    @PostMapping("/suggestion/respond/{id}")
    public String respondToSuggestion(@PathVariable Long id,
                                    @RequestParam String response,
                                    @RequestParam String status,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return "redirect:/manager/login";
        }
        
        try {
            Optional<com.example.demo.model.Suggestion> suggestionOpt = suggestionRepository.findById(id);
            if (!suggestionOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Suggestion not found!");
                return "redirect:/manager/dashboard";
            }
            
            com.example.demo.model.Suggestion suggestion = suggestionOpt.get();
            suggestion.setResponse(response);
            suggestion.setStatus(status);
            suggestion.setRespondedBy(user.getName());
            suggestion.setRespondedAt(LocalDateTime.now());
            
            suggestionRepository.save(suggestion);
            
            redirectAttributes.addFlashAttribute("success", "Response submitted successfully!");
            return "redirect:/manager/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error responding to suggestion: " + e.getMessage());
            return "redirect:/manager/dashboard";
        }
    }
}
