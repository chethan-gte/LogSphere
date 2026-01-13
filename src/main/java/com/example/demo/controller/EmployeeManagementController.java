package com.example.demo.controller;

import com.example.demo.model.Employee;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.MeetingRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.service.QRCodeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/employees")
public class EmployeeManagementController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    // List of departments
    private static final List<String> DEPARTMENTS = Arrays.asList(
        "Software Development",
        "Web Development",
        "Mobile App Development",
        "Quality Assurance (QA) / Testing",
        "DevOps & Cloud Engineering",
        "UI/UX Design",
        "Data Science & Analytics",
        "Artificial Intelligence & Machine Learning",
        "Cyber Security",
        "System Administration",
        "Network Engineering",
        "IT Support / Help Desk",
        "Database Administration"
    );

    // List of designations
    private static final List<String> DESIGNATIONS = Arrays.asList(
        "Software Engineer",
        "Junior Software Developer",
        "Senior Software Developer",
        "Full Stack Developer",
        "Frontend Developer",
        "Backend Developer",
        "Java Developer",
        ".NET Developer",
        "Python Developer",
        "Mobile App Developer",
        "Game Developer",
        "QA Engineer",
        "Manual Tester",
        "Automation Tester",
        "Test Lead",
        "Quality Analyst",
        "UI Designer",
        "UX Designer",
        "Graphic Designer",
        "Product Designer",
        "Data Analyst",
        "Data Scientist",
        "Big Data Engineer",
        "Cloud Engineer",
        "DevOps Engineer",
        "ML Engineer",
        "System Administrator",
        "Network Engineer",
        "IT Support Engineer",
        "Technical Support Executive",
        "Help Desk Analyst",
        "Project Manager",
        "Scrum Master",
        "Product Owner",
        "Business Analyst",
        "Technical Lead",
        "Team Lead",
        "HR Executive",
        "HR Manager",
        "Talent Acquisition Specialist",
        "HR Business Partner",
        "Payroll Executive",
        "Office Administrator"
    );

    @GetMapping
    public String listEmployees(Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        List<Employee> employees = employeeRepository.findAll();
        model.addAttribute("employees", employees);
        model.addAttribute("employee", new Employee());
        model.addAttribute("departments", DEPARTMENTS);
        model.addAttribute("designations", DESIGNATIONS);
        return "admin-employee-management";
    }

    @PostMapping("/add")
    public String addEmployee(@ModelAttribute Employee employee, RedirectAttributes redirectAttributes, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        // Check if employee ID already exists
        Optional<Employee> existingById = employeeRepository.findByEmployeeId(employee.getEmployeeId());
        if (existingById.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Employee ID already exists!");
            return "redirect:/admin/employees";
        }

        // Check if email already exists
        Optional<Employee> existingByEmail = employeeRepository.findByEmail(employee.getEmail());
        if (existingByEmail.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email already exists!");
            return "redirect:/admin/employees";
        }

        employee.setStatus("OUT");
        employee.setTotalHoursToday(0.0);
        employee.setLateAlertSent(false);
        employee.setEarlyAlertSent(false);
        employee.setWorkMode("OFFICE");
        // Set default scheduled times: 9 AM for clock-in, 6 PM for clock-out
        if (employee.getScheduledStartTime() == null || employee.getScheduledStartTime().isEmpty()) {
            employee.setScheduledStartTime("09:00");
        }
        if (employee.getScheduledEndTime() == null || employee.getScheduledEndTime().isEmpty()) {
            employee.setScheduledEndTime("18:00");
        }
        // Set password to empty string (not used for login - employees use email + employeeId)
        // This satisfies the database requirement for the password field
        employee.setPassword("");
        // Generate QR code token for new employee
        String qrToken = qrCodeService.generateQRCodeToken();
        employee.setQrCodeToken(qrToken);
        employeeRepository.save(employee);
        redirectAttributes.addFlashAttribute("success", "Employee added successfully! Employee can now login with email and Employee ID.");
        return "redirect:/admin/employees";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        Optional<Employee> employee = employeeRepository.findById(id);
        if (employee.isEmpty()) {
            return "redirect:/admin/employees";
        }

        model.addAttribute("employee", employee.get());
        model.addAttribute("departments", DEPARTMENTS);
        model.addAttribute("designations", DESIGNATIONS);
        return "admin-employee-edit";
    }

    @PostMapping("/update/{id}")
    public String updateEmployee(@PathVariable Long id, @ModelAttribute Employee employee, RedirectAttributes redirectAttributes, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        Optional<Employee> existingEmployee = employeeRepository.findById(id);
        if (existingEmployee.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Employee not found!");
            return "redirect:/admin/employees";
        }

        Employee emp = existingEmployee.get();
        emp.setName(employee.getName());
        emp.setEmail(employee.getEmail());
        emp.setDepartment(employee.getDepartment());
        emp.setDesignation(employee.getDesignation());
        emp.setPhone(employee.getPhone());

        // Only update employee ID if it's different and doesn't exist
        if (!emp.getEmployeeId().equals(employee.getEmployeeId())) {
            Optional<Employee> existingById = employeeRepository.findByEmployeeId(employee.getEmployeeId());
            if (existingById.isPresent() && !existingById.get().getId().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "Employee ID already exists!");
                return "redirect:/admin/employees";
            }
            emp.setEmployeeId(employee.getEmployeeId());
        }

        // Set default scheduled times if not set: 9 AM for clock-in, 6 PM for clock-out
        if (employee.getScheduledStartTime() != null && !employee.getScheduledStartTime().isEmpty()) {
            emp.setScheduledStartTime(employee.getScheduledStartTime());
        } else if (emp.getScheduledStartTime() == null || emp.getScheduledStartTime().isEmpty()) {
            emp.setScheduledStartTime("09:00");
        }
        
        if (employee.getScheduledEndTime() != null && !employee.getScheduledEndTime().isEmpty()) {
            emp.setScheduledEndTime(employee.getScheduledEndTime());
        } else if (emp.getScheduledEndTime() == null || emp.getScheduledEndTime().isEmpty()) {
            emp.setScheduledEndTime("18:00");
        }

        employeeRepository.save(emp);
        redirectAttributes.addFlashAttribute("success", "Employee updated successfully!");
        return "redirect:/admin/employees";
    }

    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(id);
        if (employeeOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Employee not found!");
            return "redirect:/admin/employees";
        }

        Employee employee = employeeOpt.get();

        try {
            // Delete all related attendance records
            List<com.example.demo.model.Attendance> attendances = attendanceRepository.findByEmployee(employee);
            attendanceRepository.deleteAll(attendances);

            // Delete all related tasks
            List<com.example.demo.model.Task> tasks = taskRepository.findByEmployee(employee);
            taskRepository.deleteAll(tasks);

            // Delete or update meetings where employee is organizer
            List<com.example.demo.model.Meeting> meetings = meetingRepository.findByOrganizer(employee);
            meetingRepository.deleteAll(meetings);

            // Now delete the employee
            employeeRepository.deleteById(id);
            
            redirectAttributes.addFlashAttribute("success", 
                "Employee deleted successfully! All related records (attendance, tasks, meetings) have been removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Failed to delete employee: " + e.getMessage() + ". Please try again or contact support.");
            System.err.println("Error deleting employee: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/admin/employees";
    }
}

