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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hr")
public class HRController {

    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private VisitorRepository visitorRepository;
    
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    
    @Autowired
    private PayrollRepository payrollRepository;
    
    @Autowired
    private JobOpeningRepository jobOpeningRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private com.example.demo.service.EmailService emailService;
    
    @Autowired
    private com.example.demo.repository.IdleIncidentRepository idleIncidentRepository;
    
    @Autowired
    private com.example.demo.repository.SuggestionRepository suggestionRepository;
    
    @Autowired
    private com.example.demo.repository.EmployeeActivityRepository employeeActivityRepository;

    @GetMapping("/login")
    public String showHRLoginForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && "HR".equals(user.getRole())) {
            return "redirect:/hr/dashboard";
        }
        return "hr-login";
    }

    @PostMapping("/login")
    public String hrLogin(@RequestParam String email, 
                         @RequestParam String password,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!"HR".equals(user.getRole())) {
                redirectAttributes.addFlashAttribute("error", "Access denied. HR login only.");
                return "redirect:/hr/login";
            }
            if (user.getPassword().equals(password)) {
                session.setAttribute("user", user);
                session.setAttribute("userName", user.getName());
                session.setAttribute("userRole", user.getRole());
                return "redirect:/hr/dashboard";
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid email or password!");
                return "redirect:/hr/login";
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password!");
            return "redirect:/hr/login";
        }
    }

    @GetMapping("/dashboard")
    public String hrDashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate startOfYear = today.withDayOfYear(1);

        // 1. Employee Overview
        List<Employee> allEmployees = employeeRepository.findAll();
        long totalEmployees = allEmployees.size();
        long activeEmployees = allEmployees.stream().filter(e -> e.getIsActive() != null && e.getIsActive()).count();
        long inactiveEmployees = totalEmployees - activeEmployees;
        long newJoineesThisMonth = allEmployees.stream()
            .filter(e -> e.getJoinDate() != null && 
                    e.getJoinDate().isAfter(startOfMonth.minusDays(1)) && 
                    e.getJoinDate().isBefore(endOfMonth.plusDays(1)))
            .count();
        long employeesOnProbation = allEmployees.stream()
            .filter(e -> e.getOnProbation() != null && e.getOnProbation())
            .count();
        
        // Department-wise count
        Map<String, Long> departmentCount = allEmployees.stream()
            .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));

        // 2. Attendance & Time Management
        List<Employee> presentEmployees = employeeRepository.findCurrentlyPresentEmployees();
        long presentCount = presentEmployees.size();
        long absentCount = totalEmployees - presentCount;
        
        List<Attendance> todayAttendance = attendanceRepository.findByAttendanceDate(today);
        long lateCount = todayAttendance.stream().filter(a -> "LATE".equals(a.getStatus())).count();
        long wfhRequests = allEmployees.stream()
            .filter(e -> "WORK_FROM_HOME".equals(e.getWorkMode()))
            .count();
        
        // Monthly attendance
        List<Attendance> monthlyAttendance = attendanceRepository.findByDateRange(startOfMonth, endOfMonth);
        Map<String, Long> attendanceStatusCount = monthlyAttendance.stream()
            .collect(Collectors.groupingBy(Attendance::getStatus, Collectors.counting()));

        // 3. Leave Management
        List<LeaveRequest> pendingLeaves = leaveRequestRepository.findByStatus("PENDING");
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByStatus("APPROVED");
        List<LeaveRequest> rejectedLeaves = leaveRequestRepository.findByStatus("REJECTED");
        List<LeaveRequest> monthlyLeaves = leaveRequestRepository.findLeavesInDateRange(startOfMonth, endOfMonth);

        // 4. Task & Performance - Per Employee
        List<Task> allTasks = taskRepository.findAll();
        long totalTasks = allTasks.size();
        long completedTasks = allTasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long overdueTasks = allTasks.stream()
            .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today) && !"COMPLETED".equals(t.getStatus()))
            .count();
        double taskCompletionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0.0;
        
        // Calculate task performance per employee
        Map<Employee, Map<String, Long>> employeeTaskStats = new HashMap<>();
        for (Employee emp : allEmployees) {
            List<Task> empTasks = taskRepository.findByEmployee(emp);
            Map<String, Long> stats = new HashMap<>();
            stats.put("total", (long) empTasks.size());
            stats.put("completed", empTasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count());
            stats.put("inProgress", empTasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count());
            stats.put("pending", empTasks.stream().filter(t -> "PENDING".equals(t.getStatus())).count());
            stats.put("cancelled", empTasks.stream().filter(t -> "CANCELLED".equals(t.getStatus())).count());
            
            // Calculate completion rate for this employee
            long empTotal = stats.get("total");
            long empCompleted = stats.get("completed");
            double empCompletionRate = empTotal > 0 ? (empCompleted * 100.0 / empTotal) : 0.0;
            stats.put("completionRate", (long) empCompletionRate);
            
            if (empTotal > 0) { // Only include employees with tasks
                employeeTaskStats.put(emp, stats);
            }
        }
        
        // Top performers (by task completion rate and completed tasks)
        List<Map.Entry<Employee, Map<String, Long>>> topPerformersList = employeeTaskStats.entrySet().stream()
            .sorted((e1, e2) -> {
                // Sort by completion rate first, then by number of completed tasks
                Long rate1 = e1.getValue().get("completionRate");
                Long rate2 = e2.getValue().get("completionRate");
                int rateCompare = rate2.compareTo(rate1);
                if (rateCompare != 0) return rateCompare;
                return Long.compare(e2.getValue().get("completed"), e1.getValue().get("completed"));
            })
            .limit(10)
            .collect(Collectors.toList());
        
        // Create a list of employee task performance data for the view
        List<Map<String, Object>> employeeTaskPerformance = new ArrayList<>();
        for (Map.Entry<Employee, Map<String, Long>> entry : topPerformersList) {
            Map<String, Object> perfData = new HashMap<>();
            perfData.put("employee", entry.getKey());
            perfData.put("stats", entry.getValue());
            employeeTaskPerformance.add(perfData);
        }

        // 5. Recruitment & Onboarding
        List<JobOpening> openJobs = jobOpeningRepository.findByStatus("OPEN");
        List<Candidate> allCandidates = candidateRepository.findAll();
        List<Candidate> selectedCandidates = candidateRepository.findByStatus("SELECTED");
        List<Candidate> rejectedCandidates = candidateRepository.findByStatus("REJECTED");
        List<Candidate> interviewedCandidates = candidateRepository.findByStatus("INTERVIEWED");

        // 6. Payroll & Compensation
        List<Payroll> pendingPayrolls = payrollRepository.findByStatus("PENDING");
        List<Payroll> processedPayrolls = payrollRepository.findByStatus("PROCESSED");
        // Show all payrolls in the HR dashboard so HR can always see newly added records,
        // regardless of the current month filter. This avoids confusion where a saved
        // payroll is not visible because its pay period falls outside the current month.
        List<Payroll> monthlyPayrolls = payrollRepository.findAll();

        // 7. Notifications & Alerts
        List<Employee> expiringContracts = allEmployees.stream()
            .filter(e -> e.getProbationEndDate() != null && 
                    e.getProbationEndDate().isAfter(today) && 
                    e.getProbationEndDate().isBefore(today.plusDays(30)))
            .collect(Collectors.toList());

        // Add all to model
        model.addAttribute("userName", user.getName());
        model.addAttribute("userRole", user.getRole());
        
        // Employee Overview
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("activeEmployees", activeEmployees);
        model.addAttribute("inactiveEmployees", inactiveEmployees);
        model.addAttribute("newJoineesThisMonth", newJoineesThisMonth);
        model.addAttribute("employeesOnProbation", employeesOnProbation);
        model.addAttribute("departmentCount", departmentCount);
        model.addAttribute("allEmployees", allEmployees);
        
        // Attendance
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("absentCount", absentCount);
        model.addAttribute("lateCount", lateCount);
        model.addAttribute("wfhRequests", wfhRequests);
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("monthlyAttendance", monthlyAttendance);
        model.addAttribute("attendanceStatusCount", attendanceStatusCount);
        model.addAttribute("presentEmployees", presentEmployees);
        
        // Leave Management
        model.addAttribute("pendingLeaves", pendingLeaves);
        model.addAttribute("approvedLeaves", approvedLeaves);
        model.addAttribute("rejectedLeaves", rejectedLeaves);
        model.addAttribute("monthlyLeaves", monthlyLeaves);
        
        // Task & Performance
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("taskCompletionRate", taskCompletionRate);
        model.addAttribute("employeeTaskPerformance", employeeTaskPerformance);
        model.addAttribute("employeeTaskStats", employeeTaskStats);
        model.addAttribute("allTasks", allTasks);
        
        // Recruitment
        model.addAttribute("openJobs", openJobs);
        model.addAttribute("allCandidates", allCandidates);
        model.addAttribute("selectedCandidates", selectedCandidates);
        model.addAttribute("rejectedCandidates", rejectedCandidates);
        model.addAttribute("interviewedCandidates", interviewedCandidates);
        
        // Payroll
        model.addAttribute("pendingPayrolls", pendingPayrolls);
        model.addAttribute("processedPayrolls", processedPayrolls);
        model.addAttribute("monthlyPayrolls", monthlyPayrolls);
        
        // Notifications
        model.addAttribute("expiringContracts", expiringContracts);
        
        // Visitor stats
        List<Visitor> allVisitors = visitorRepository.findAllOrderByCheckInTimeDesc();
        List<Visitor> checkedInVisitors = visitorRepository.findByStatus("Checked In");
        model.addAttribute("totalVisitors", allVisitors.size());
        model.addAttribute("currentlyIn", checkedInVisitors.size());
        
        // Idle employee notifications
        List<com.example.demo.model.IdleIncident> activeIdleIncidents = idleIncidentRepository.findActiveIncidents();
        model.addAttribute("idleIncidents", activeIdleIncidents);
        model.addAttribute("idleCount", activeIdleIncidents.size());
        
        // Employee Suggestions
        List<com.example.demo.model.Suggestion> allSuggestions = suggestionRepository.findAllByOrderByCreatedAtDesc();
        List<com.example.demo.model.Suggestion> pendingSuggestions = suggestionRepository.findByStatusOrderByCreatedAtDesc("PENDING");
        List<com.example.demo.model.Suggestion> hrSuggestions = suggestionRepository.findBySuggestionTypeOrderByCreatedAtDesc("HR");
        model.addAttribute("allSuggestions", allSuggestions);
        model.addAttribute("pendingSuggestions", pendingSuggestions);
        model.addAttribute("hrSuggestions", hrSuggestions);
        model.addAttribute("suggestionCount", pendingSuggestions.size());
        
        // Non-Productive Activities (Last 24 hours)
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<com.example.demo.model.EmployeeActivity> nonProductiveActivities = employeeActivityRepository.findNonProductiveActivities(last24Hours);
        List<com.example.demo.model.EmployeeActivity> gamingActivities = employeeActivityRepository.findByActivityTypeAndStartedAtAfter("GAMING", last24Hours);
        List<com.example.demo.model.EmployeeActivity> socialMediaActivities = employeeActivityRepository.findByActivityTypeAndStartedAtAfter("SOCIAL_MEDIA", last24Hours);
        List<com.example.demo.model.EmployeeActivity> videoActivities = employeeActivityRepository.findByActivityTypeAndStartedAtAfter("VIDEO", last24Hours);
        
        // Group by employee
        Map<Employee, List<com.example.demo.model.EmployeeActivity>> nonProductiveByEmployee = nonProductiveActivities.stream()
            .collect(Collectors.groupingBy(com.example.demo.model.EmployeeActivity::getEmployee));
        
        // Calculate total non-productive time per employee
        Map<Employee, Long> nonProductiveTimeByEmployee = new HashMap<>();
        for (Map.Entry<Employee, List<com.example.demo.model.EmployeeActivity>> entry : nonProductiveByEmployee.entrySet()) {
            long totalMinutes = entry.getValue().stream()
                .mapToLong(a -> a.getDurationMinutes() != null ? a.getDurationMinutes() : 0)
                .sum();
            nonProductiveTimeByEmployee.put(entry.getKey(), totalMinutes);
        }
        
        model.addAttribute("nonProductiveActivities", nonProductiveActivities);
        model.addAttribute("gamingActivities", gamingActivities);
        model.addAttribute("socialMediaActivities", socialMediaActivities);
        model.addAttribute("videoActivities", videoActivities);
        model.addAttribute("nonProductiveByEmployee", nonProductiveByEmployee);
        model.addAttribute("nonProductiveTimeByEmployee", nonProductiveTimeByEmployee);
        model.addAttribute("nonProductiveCount", nonProductiveActivities.size());
        
        return "hr-dashboard";
    }
    
    @PostMapping("/leave/approve/{id}")
    public String approveLeave(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        Optional<LeaveRequest> leaveRequestOpt = leaveRequestRepository.findById(id);
        if (!leaveRequestOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Leave request not found!");
            return "redirect:/hr/dashboard";
        }
        
        LeaveRequest leaveRequest = leaveRequestOpt.get();
        if (!"PENDING".equals(leaveRequest.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "This leave request has already been processed!");
            return "redirect:/hr/dashboard";
        }
        
        // Update leave request status
        leaveRequest.setStatus("APPROVED");
        leaveRequest.setApprovedBy(user.getName());
        leaveRequest.setApprovalDate(LocalDateTime.now());
        leaveRequestRepository.save(leaveRequest);
        
        // Update employee leave balance (deduct approved days)
        Employee employee = leaveRequest.getEmployee();
        Integer currentBalance = employee.getLeaveBalance() != null ? employee.getLeaveBalance() : 0;
        Integer newBalance = currentBalance - leaveRequest.getNumberOfDays();
        if (newBalance < 0) newBalance = 0; // Prevent negative balance
        employee.setLeaveBalance(newBalance);
        employeeRepository.save(employee);
        
        // Send email notification
        try {
            emailService.sendLeaveApprovalNotification(
                employee.getEmail(),
                employee.getName(),
                leaveRequest.getLeaveType(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getNumberOfDays()
            );
        } catch (Exception e) {
            System.err.println("Failed to send leave approval email: " + e.getMessage());
        }
        
        redirectAttributes.addFlashAttribute("success", "Leave request approved successfully! Email notification sent to employee.");
        return "redirect:/hr/dashboard";
    }
    
    @PostMapping("/leave/reject/{id}")
    public String rejectLeave(@PathVariable Long id, 
                             @RequestParam(required = false) String rejectionReason,
                             HttpSession session, 
                             RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        Optional<LeaveRequest> leaveRequestOpt = leaveRequestRepository.findById(id);
        if (!leaveRequestOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Leave request not found!");
            return "redirect:/hr/dashboard";
        }
        
        LeaveRequest leaveRequest = leaveRequestOpt.get();
        if (!"PENDING".equals(leaveRequest.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "This leave request has already been processed!");
            return "redirect:/hr/dashboard";
        }
        
        // Update leave request status
        leaveRequest.setStatus("REJECTED");
        leaveRequest.setApprovedBy(user.getName());
        leaveRequest.setApprovalDate(LocalDateTime.now());
        leaveRequest.setRejectionReason(rejectionReason);
        leaveRequestRepository.save(leaveRequest);
        
        // Send email notification
        Employee employee = leaveRequest.getEmployee();
        try {
            emailService.sendLeaveRejectionNotification(
                employee.getEmail(),
                employee.getName(),
                leaveRequest.getLeaveType(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                rejectionReason
            );
        } catch (Exception e) {
            System.err.println("Failed to send leave rejection email: " + e.getMessage());
        }
        
        redirectAttributes.addFlashAttribute("success", "Leave request rejected. Email notification sent to employee.");
        return "redirect:/hr/dashboard";
    }
    
    @GetMapping("/recruitment/candidate/add")
    public String showAddCandidateForm(@RequestParam(required = false) Long jobId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        List<JobOpening> openJobs = jobOpeningRepository.findByStatus("OPEN");
        model.addAttribute("openJobs", openJobs);
        model.addAttribute("selectedJobId", jobId);
        
        return "hr-add-candidate";
    }
    
    @PostMapping("/recruitment/candidate/add")
    public String addCandidate(@RequestParam String name,
                              @RequestParam String email,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String gender,
                              @RequestParam(required = false) String dateOfBirth,
                              @RequestParam(required = false) String jobTitle,
                              @RequestParam(required = false) String department,
                              @RequestParam(required = false) String employmentType,
                              @RequestParam(required = false) Long jobOpeningId,
                              @RequestParam(required = false) String highestQualification,
                              @RequestParam(required = false) String specialization,
                              @RequestParam(required = false) String collegeUniversity,
                              @RequestParam(required = false) Integer yearOfPassing,
                              @RequestParam(required = false) String primarySkills,
                              @RequestParam(required = false) String secondarySkills,
                              @RequestParam(required = false) Double totalExperience,
                              @RequestParam(required = false) Double relevantExperience,
                              @RequestParam(required = false) String portfolioLink,
                              @RequestParam(required = false) String status,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        try {
            Candidate candidate = new Candidate();
            candidate.setName(name);
            candidate.setEmail(email);
            candidate.setPhone(phone);
            candidate.setGender(gender);
            
            if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                candidate.setDateOfBirth(LocalDate.parse(dateOfBirth));
            }
            
            candidate.setJobTitle(jobTitle);
            candidate.setDepartment(department);
            candidate.setEmploymentType(employmentType);
            
            if (jobOpeningId != null) {
                Optional<JobOpening> jobOpt = jobOpeningRepository.findById(jobOpeningId);
                jobOpt.ifPresent(candidate::setJobOpening);
            }
            
            candidate.setHighestQualification(highestQualification);
            candidate.setSpecialization(specialization);
            candidate.setCollegeUniversity(collegeUniversity);
            candidate.setYearOfPassing(yearOfPassing);
            candidate.setPrimarySkills(primarySkills);
            candidate.setSecondarySkills(secondarySkills);
            candidate.setTotalExperience(totalExperience);
            candidate.setRelevantExperience(relevantExperience);
            candidate.setPortfolioLink(portfolioLink);
            candidate.setStatus(status != null ? status : "APPLIED");
            
            candidateRepository.save(candidate);
            
            redirectAttributes.addFlashAttribute("success", "Candidate added successfully!");
            return "redirect:/hr/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding candidate: " + e.getMessage());
            return "redirect:/hr/recruitment/candidate/add";
        }
    }
    
    @GetMapping("/recruitment/candidate/view/{id}")
    public String viewCandidate(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        Optional<Candidate> candidateOpt = candidateRepository.findById(id);
        if (!candidateOpt.isPresent()) {
            return "redirect:/hr/dashboard";
        }
        
        model.addAttribute("candidate", candidateOpt.get());
        return "hr-view-candidate";
    }
    
    @GetMapping("/recruitment/candidate/edit/{id}")
    public String editCandidate(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        Optional<Candidate> candidateOpt = candidateRepository.findById(id);
        if (!candidateOpt.isPresent()) {
            return "redirect:/hr/dashboard";
        }
        
        List<JobOpening> openJobs = jobOpeningRepository.findByStatus("OPEN");
        model.addAttribute("candidate", candidateOpt.get());
        model.addAttribute("openJobs", openJobs);
        
        return "hr-edit-candidate";
    }
    
    @PostMapping("/recruitment/candidate/update/{id}")
    public String updateCandidate(@PathVariable Long id,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String interviewDate,
                                 @RequestParam(required = false) String interviewTime,
                                 @RequestParam(required = false) String interviewerName,
                                 @RequestParam(required = false) String interviewRound,
                                 @RequestParam(required = false) String interviewFeedback,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        Optional<Candidate> candidateOpt = candidateRepository.findById(id);
        if (!candidateOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Candidate not found!");
            return "redirect:/hr/dashboard";
        }
        
        Candidate candidate = candidateOpt.get();
        
        if (status != null) {
            candidate.setStatus(status);
        }
        
        if (interviewDate != null && !interviewDate.isEmpty()) {
            try {
                LocalDateTime interviewDateTime = LocalDateTime.parse(interviewDate + "T" + (interviewTime != null ? interviewTime : "10:00"));
                candidate.setInterviewDate(interviewDateTime);
            } catch (Exception e) {
                System.err.println("Error parsing interview date: " + e.getMessage());
            }
        }
        
        candidate.setInterviewTime(interviewTime);
        candidate.setInterviewerName(interviewerName);
        candidate.setInterviewRound(interviewRound);
        candidate.setInterviewFeedback(interviewFeedback);
        
        candidateRepository.save(candidate);
        
        redirectAttributes.addFlashAttribute("success", "Candidate updated successfully!");
        return "redirect:/hr/dashboard";
    }
    
    @GetMapping("/recruitment/job/create")
    public String showCreateJobForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        return "hr-create-job";
    }
    
    @PostMapping("/recruitment/job/create")
    public String createJobOpening(@RequestParam String title,
                                  @RequestParam String department,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false) String requirements,
                                  @RequestParam(required = false) Integer numberOfPositions,
                                  @RequestParam(required = false) String experienceRequired,
                                  @RequestParam(required = false) String closingDate,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        try {
            JobOpening jobOpening = new JobOpening();
            jobOpening.setTitle(title);
            jobOpening.setDepartment(department);
            jobOpening.setDescription(description);
            jobOpening.setRequirements(requirements);
            jobOpening.setNumberOfPositions(numberOfPositions != null ? numberOfPositions : 1);
            jobOpening.setExperienceRequired(experienceRequired);
            jobOpening.setStatus("OPEN");
            
            if (closingDate != null && !closingDate.isEmpty()) {
                jobOpening.setClosingDate(LocalDate.parse(closingDate));
            }
            
            jobOpeningRepository.save(jobOpening);
            
            redirectAttributes.addFlashAttribute("success", "Job opening created successfully!");
            return "redirect:/hr/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating job opening: " + e.getMessage());
            return "redirect:/hr/recruitment/job/create";
        }
    }
    
    @GetMapping("/payroll/add")
    public String showAddPayrollForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        List<Employee> allEmployees = employeeRepository.findAll();
        model.addAttribute("employees", allEmployees);
        
        return "hr-add-payroll";
    }
    
    @PostMapping("/payroll/add")
    public String addPayroll(@RequestParam Long employeeId,
                           @RequestParam String payPeriodStart,
                           @RequestParam String payPeriodEnd,
                           @RequestParam Double baseSalary,
                           @RequestParam(required = false) Double bonus,
                           @RequestParam(required = false) Double incentive,
                           @RequestParam(required = false) Double overtimePay,
                           @RequestParam(required = false) Double allowances,
                           @RequestParam(required = false) Double taxDeduction,
                           @RequestParam(required = false) Double otherDeductions,
                           @RequestParam(required = false) String paymentDate,
                           @RequestParam(required = false) String status,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        try {
            Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
            if (!employeeOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Employee not found!");
                return "redirect:/hr/payroll/add";
            }
            
            Payroll payroll = new Payroll();
            payroll.setEmployee(employeeOpt.get());
            payroll.setPayPeriodStart(LocalDate.parse(payPeriodStart));
            payroll.setPayPeriodEnd(LocalDate.parse(payPeriodEnd));
            payroll.setBaseSalary(baseSalary);
            payroll.setBonus(bonus != null ? bonus : 0.0);
            payroll.setIncentive(incentive != null ? incentive : 0.0);
            payroll.setOvertimePay(overtimePay != null ? overtimePay : 0.0);
            payroll.setAllowances(allowances != null ? allowances : 0.0);
            payroll.setTaxDeduction(taxDeduction != null ? taxDeduction : 0.0);
            payroll.setOtherDeductions(otherDeductions != null ? otherDeductions : 0.0);
            payroll.setStatus(status != null ? status : "PENDING");
            
            if (paymentDate != null && !paymentDate.isEmpty()) {
                payroll.setPaymentDate(LocalDate.parse(paymentDate));
            }
            
            // Calculate net salary manually to ensure it's set
            double gross = payroll.getBaseSalary() + payroll.getBonus() + payroll.getIncentive() + 
                          payroll.getOvertimePay() + payroll.getAllowances();
            double net = gross - payroll.getTaxDeduction() - payroll.getOtherDeductions();
            payroll.setNetSalary(net);
            
            Payroll savedPayroll = payrollRepository.save(payroll);
            System.out.println("Payroll saved with ID: " + savedPayroll.getId());
            
            redirectAttributes.addFlashAttribute("success", "Payroll added successfully! Net Salary: $" + String.format("%.2f", net));
            return "redirect:/hr/dashboard";
        } catch (Exception e) {
            System.err.println("Error adding payroll: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error adding payroll: " + e.getMessage());
            return "redirect:/hr/payroll/add";
        }
    }
    
    @GetMapping("/payroll/view/{id}")
    public String viewPayroll(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        Optional<Payroll> payrollOpt = payrollRepository.findById(id);
        if (!payrollOpt.isPresent()) {
            return "redirect:/hr/dashboard";
        }
        
        model.addAttribute("payroll", payrollOpt.get());
        return "hr-view-payroll";
    }
    
    @GetMapping("/payroll/payslip/{id}")
    public String generatePayslip(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        try {
            Optional<Payroll> payrollOpt = payrollRepository.findById(id);
            if (!payrollOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Payroll not found!");
                return "redirect:/hr/dashboard";
            }
            
            Payroll payroll = payrollOpt.get();
            
            // Ensure employee is loaded
            if (payroll.getEmployee() == null) {
                redirectAttributes.addFlashAttribute("error", "Employee information not found for this payroll!");
                return "redirect:/hr/dashboard";
            }
            
            payroll.setPayslipGenerated(true);
            payrollRepository.save(payroll);
            
            model.addAttribute("payroll", payroll);
            return "hr-payslip";
        } catch (Exception e) {
            System.err.println("Error generating payslip: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error generating payslip: " + e.getMessage());
            return "redirect:/hr/dashboard";
        }
    }
    
    @GetMapping("/payroll/generate-all")
    public String generateAllPayslips(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        List<Payroll> pendingPayrolls = payrollRepository.findByStatus("PENDING");
        for (Payroll payroll : pendingPayrolls) {
            payroll.setPayslipGenerated(true);
            payroll.setStatus("PROCESSED");
            payrollRepository.save(payroll);
        }
        
        redirectAttributes.addFlashAttribute("success", "Generated payslips for " + pendingPayrolls.size() + " payroll records!");
        return "redirect:/hr/dashboard";
    }
    
    @PostMapping("/suggestion/respond/{id}")
    public String respondToSuggestion(@PathVariable Long id,
                                    @RequestParam String response,
                                    @RequestParam String status,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"HR".equals(user.getRole())) {
            return "redirect:/hr/login";
        }
        
        try {
            Optional<com.example.demo.model.Suggestion> suggestionOpt = suggestionRepository.findById(id);
            if (!suggestionOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Suggestion not found!");
                return "redirect:/hr/dashboard";
            }
            
            com.example.demo.model.Suggestion suggestion = suggestionOpt.get();
            suggestion.setResponse(response);
            suggestion.setStatus(status);
            suggestion.setRespondedBy(user.getName());
            suggestion.setRespondedAt(LocalDateTime.now());
            
            suggestionRepository.save(suggestion);
            
            redirectAttributes.addFlashAttribute("success", "Response submitted successfully!");
            return "redirect:/hr/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error responding to suggestion: " + e.getMessage());
            return "redirect:/hr/dashboard";
        }
    }
}
