package com.kce.leave.controller;

import com.kce.leave.config.PasswordUtil;
import com.kce.leave.model.LeaveRequest;
import com.kce.leave.model.Student;
import com.kce.leave.model.Teacher;
import com.kce.leave.repository.LeaveRequestRepository;
import com.kce.leave.repository.StudentRepository;
import com.kce.leave.repository.TeacherRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class PortalController {
    private final LeaveRequestRepository leaves;
    private final StudentRepository students;
    private final TeacherRepository teachers;

    public PortalController(LeaveRequestRepository leaves, StudentRepository students, TeacherRepository teachers) {
        this.leaves = leaves; this.students = students; this.teachers = teachers;
    }

    private Student currentStudent(HttpSession session) {
        String id = (String) session.getAttribute("studentId");
        return id == null ? null : students.findById(id).orElse(null);
    }

    @GetMapping("/")
    public String home() { return "index"; }

    // ================= STUDENT =================
    @GetMapping("/student/login")
    public String studentLoginPage() { return "student-login"; }

    @PostMapping("/student/login")
    public String studentLogin(@RequestParam String username, @RequestParam String password,
                               HttpSession session, Model model) {
        String regNo = password.trim();
        Optional<Student> match = students.findByRegisterNoIgnoreCase(regNo)
                .filter(s -> s.getPasswordHash().equals(PasswordUtil.hash(regNo.toLowerCase())))
                .filter(s -> nameMatches(s.getName(), username));
        if (match.isPresent()) {
            session.setAttribute("studentId", match.get().getId());
            return "redirect:/student/leave";
        }
        model.addAttribute("error", "Invalid username or password.");
        return "student-login";
    }

    /** Username may be the full name or just the first part (case and spaces ignored). */
    private static boolean nameMatches(String stored, String typed) {
        String a = stored.toLowerCase().replaceAll("[^a-z]", "");
        String b = typed.toLowerCase().replaceAll("[^a-z]", "");
        return !b.isEmpty() && a.startsWith(b);
    }

    @GetMapping("/student/leave")
    public String leaveForm(HttpSession session, Model model) {
        Student s = currentStudent(session);
        if (s == null) return "redirect:/student/login";
        model.addAttribute("student", s);
        return "student-form";
    }

    @PostMapping("/student/leave")
    public String submitLeave(@RequestParam String rollNo, @RequestParam String reason,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                              @RequestParam String parentMobile,
                              HttpSession session, Model model, RedirectAttributes ra) {
        Student s = currentStudent(session);
        if (s == null) return "redirect:/student/login";

        String error = null;
        if (rollNo.isBlank()) error = "Roll number is required.";
        else if (reason.isBlank()) error = "Please enter the reason for leave.";
        else if (toDate.isBefore(fromDate)) error = "End date cannot be before the start date.";
        else if (!parentMobile.trim().matches("\\d{10}")) error = "Parent mobile number must be 10 digits.";

        if (error != null) {
            model.addAttribute("student", s);
            model.addAttribute("error", error);
            model.addAttribute("rollNo", rollNo); model.addAttribute("reason", reason);
            model.addAttribute("fromDate", fromDate); model.addAttribute("toDate", toDate);
            model.addAttribute("parentMobile", parentMobile);
            return "student-form";
        }

        LeaveRequest l = new LeaveRequest();
        l.setStudentId(s.getId());
        l.setName(s.getName());
        l.setClassName(s.getClassName());
        l.setRegisterNo(s.getRegisterNo());
        l.setRollNo(rollNo.trim());
        l.setReason(reason.trim());
        l.setFromDate(fromDate);
        l.setToDate(toDate);
        l.setParentMobile(parentMobile.trim());
        leaves.save(l);
        ra.addFlashAttribute("success", "Your leave request has been submitted to the class teacher.");
        return "redirect:/student/status";
    }

    @GetMapping("/student/status")
    public String status(HttpSession session, Model model) {
        Student s = currentStudent(session);
        if (s == null) return "redirect:/student/login";
        model.addAttribute("student", s);
        model.addAttribute("requests", leaves.findByStudentIdOrderByAppliedOnDesc(s.getId()));
        return "student-status";
    }

    @GetMapping("/student/logout")
    public String studentLogout(HttpSession session) { session.invalidate(); return "redirect:/"; }

    // ================= TEACHER =================
    @GetMapping("/teacher/login")
    public String teacherLoginPage() { return "teacher-login"; }

    @PostMapping("/teacher/login")
    public String teacherLogin(@RequestParam String className, @RequestParam String password,
                               HttpSession session, Model model) {
        String cls = className.trim().toUpperCase();
        Optional<Teacher> t = teachers.findByClassName(cls);
        if (t.isPresent() && t.get().getPasswordHash().equals(PasswordUtil.hash(password.trim()))) {
            session.setAttribute("teacherClass", cls);
            return "redirect:/teacher/dashboard";
        }
        model.addAttribute("error", "Invalid class or password.");
        return "teacher-login";
    }

    @GetMapping("/teacher/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String cls = (String) session.getAttribute("teacherClass");
        if (cls == null) return "redirect:/teacher/login";
        List<LeaveRequest> list = leaves.findByClassNameOrderByAppliedOnDesc(cls);
        model.addAttribute("cls", cls);
        model.addAttribute("requests", list);
        model.addAttribute("total", list.size());
        model.addAttribute("pending", list.stream().filter(l -> "PENDING".equals(l.getStatus())).count());
        model.addAttribute("approved", list.stream().filter(l -> "APPROVED".equals(l.getStatus())).count());
        model.addAttribute("rejected", list.stream().filter(l -> "REJECTED".equals(l.getStatus())).count());
        return "teacher-dashboard";
    }

    @PostMapping("/teacher/leave/{id}/{action}")
    public String decide(@PathVariable String id, @PathVariable String action, HttpSession session) {
        String cls = (String) session.getAttribute("teacherClass");
        if (cls == null) return "redirect:/teacher/login";
        leaves.findById(id).ifPresent(l -> {
            if (cls.equals(l.getClassName())) {   // teacher can only decide for own class
                if ("approve".equals(action)) l.setStatus("APPROVED");
                else if ("reject".equals(action)) l.setStatus("REJECTED");
                leaves.save(l);
            }
        });
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/teacher/logout")
    public String teacherLogout(HttpSession session) { session.invalidate(); return "redirect:/"; }
}
