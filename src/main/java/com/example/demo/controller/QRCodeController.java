package com.example.demo.controller;

import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.QRCodeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/employee/qrcode")
public class QRCodeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private QRCodeService qrCodeService;

    @GetMapping
    public String showQRCode(HttpSession session, Model model) {
        Employee employee = (Employee) session.getAttribute("employee");
        if (employee == null) {
            return "redirect:/employee/login";
        }

        // Generate or retrieve QR code token
        if (employee.getQrCodeToken() == null || employee.getQrCodeToken().isEmpty()) {
            String token = qrCodeService.generateQRCodeToken();
            employee.setQrCodeToken(token);
            employee = employeeRepository.save(employee);
            session.setAttribute("employee", employee);
        }

        // Generate QR code data
        String qrData = qrCodeService.generateQRCodeData(employee.getEmployeeId(), employee.getQrCodeToken());
        String qrCodeImage = qrCodeService.generateQRCodeImage(qrData);

        model.addAttribute("employee", employee);
        model.addAttribute("qrCodeImage", qrCodeImage);
        model.addAttribute("qrData", qrData);

        return "employee-qrcode";
    }

    @PostMapping("/scan")
    @ResponseBody
    public ResponseEntity<?> scanQRCode(@RequestParam String qrData, @RequestParam String workMode) {
        try {
            // Parse QR code data: LOGSPHERE:EMPLOYEE_ID:TOKEN
            String[] parts = qrData.split(":");
            if (parts.length != 3 || !"LOGSPHERE".equals(parts[0])) {
                return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"Invalid QR code format\"}");
            }

            String employeeId = parts[1];
            String token = parts[2];

            Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);
            if (employeeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"Employee not found\"}");
            }

            Employee employee = employeeOpt.get();
            if (!token.equals(employee.getQrCodeToken())) {
                return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"Invalid QR code token\"}");
            }

            // Return success with employee info
            return ResponseEntity.ok().body("{\"success\": true, \"employeeId\": \"" + employeeId + 
                    "\", \"employeeName\": \"" + employee.getName() + "\", \"workMode\": \"" + workMode + "\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"Error processing QR code: " + 
                    e.getMessage() + "\"}");
        }
    }
}
