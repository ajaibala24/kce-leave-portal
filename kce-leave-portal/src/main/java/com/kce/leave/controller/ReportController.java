package com.kce.leave.controller;

import com.kce.leave.model.LeaveRequest;
import com.kce.leave.repository.LeaveRequestRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Teacher-only leave report: choose a date range and a status, download a PDF
 * with the college logo and name in the header.
 *
 * Page    : GET /teacher/report            (form)
 * Download: GET /teacher/report/pdf?from=&to=&status=
 */
@Controller
public class ReportController {

    public static final String COLLEGE_NAME = "KINGS COLLEGE OF ENGINEERING";
    public static final String COLLEGE_ADDRESS = "Punalkulam, Pudukkottai - 613 303, Tamil Nadu";
    public static final String COLLEGE_AFFILIATION = "Approved by AICTE  |  Affiliated to Anna University, Chennai";

    private static final Color NAVY = new Color(0x0B, 0x2A, 0x5B);
    private static final Color GOLD = new Color(0xC9, 0xA2, 0x27);
    private static final Color GREEN = new Color(0x1E, 0x8E, 0x3E);
    private static final Color RED = new Color(0xC6, 0x28, 0x28);
    private static final Color AMBER = new Color(0xE0, 0x8A, 0x00);
    private static final Color ZEBRA = new Color(0xF3, 0xF6, 0xFB);

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final LeaveRequestRepository leaves;

    public ReportController(LeaveRequestRepository leaves) {
        this.leaves = leaves;
    }

    // ---------- report form page ----------
    @GetMapping("/teacher/report")
    public String reportPage(HttpSession session, Model model) {
        String cls = (String) session.getAttribute("teacherClass");
        if (cls == null) return "redirect:/teacher/login";
        model.addAttribute("cls", cls);
        model.addAttribute("collegeName", COLLEGE_NAME);
        return "teacher-report";
    }

    // ---------- PDF download ----------
    @GetMapping("/teacher/report/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ALL") String status,
            HttpSession session) {

        String cls = (String) session.getAttribute("teacherClass");
        if (cls == null) {
            return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "/teacher/login").build();
        }
        if (from != null && to != null && to.isBefore(from)) {
            return ResponseEntity.badRequest().build();
        }

        String st = status.trim().toUpperCase();
        if (!List.of("ALL", "APPROVED", "REJECTED", "PENDING").contains(st)) st = "ALL";
        final String statusFilter = st;

        // A leave is included when it overlaps the chosen period.
        List<LeaveRequest> list = leaves.findByClassNameOrderByAppliedOnDesc(cls).stream()
                .filter(l -> from == null || (l.getToDate() != null && !l.getToDate().isBefore(from)))
                .filter(l -> to == null || (l.getFromDate() != null && !l.getFromDate().isAfter(to)))
                .filter(l -> "ALL".equals(statusFilter) || statusFilter.equals(l.getStatus()))
                .sorted(Comparator.comparing(LeaveRequest::getFromDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        byte[] pdf = buildPdf(cls, from, to, statusFilter, list);

        String safeCls = cls.replaceAll("[^A-Za-z0-9-]", "");
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_PDF);
        h.setContentDisposition(ContentDisposition.attachment()
                .filename("leave-report-" + safeCls + "-" + LocalDate.now() + ".pdf").build());
        return new ResponseEntity<>(pdf, h, HttpStatus.OK);
    }

    // ---------- PDF builder ----------
    private byte[] buildPdf(String cls, LocalDate from, LocalDate to, String status, List<LeaveRequest> list) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 28, 45);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageNumbers());
            doc.open();

            Font collegeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, NAVY);
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font infoBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

            // --- header: logo + college name ---
            PdfPTable head = new PdfPTable(new float[]{1.2f, 8f});
            head.setWidthPercentage(100);
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Image logo = loadLogo();
            if (logo != null) {
                logo.scaleToFit(62, 62);
                logoCell.addElement(logo);
            }
            head.addCell(logoCell);

            PdfPCell nameCell = new PdfPCell();
            nameCell.setBorder(Rectangle.NO_BORDER);
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nameCell.addElement(new Paragraph(COLLEGE_NAME, collegeFont));
            nameCell.addElement(new Paragraph(COLLEGE_ADDRESS, smallFont));
            nameCell.addElement(new Paragraph(COLLEGE_AFFILIATION, smallFont));
            head.addCell(nameCell);
            doc.add(head);

            doc.add(new Chunk(new LineSeparator(1.5f, 100f, GOLD, Element.ALIGN_CENTER, -2f)));

            // --- title and filters ---
            Paragraph title = new Paragraph("STUDENT LEAVE REPORT - CLASS " + cls, titleFont);
            title.setSpacingBefore(10);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            String period = (from == null && to == null) ? "All dates"
                    : (from == null ? "Up to " + D.format(to)
                    : (to == null ? "From " + D.format(from) : D.format(from) + "  to  " + D.format(to)));
            String statusLabel = switch (status) {
                case "APPROVED" -> "Approved";
                case "REJECTED" -> "Not Approved (Rejected)";
                case "PENDING" -> "Pending";
                default -> "All (Approved / Not Approved / Pending)";
            };

            long approved = list.stream().filter(l -> "APPROVED".equals(l.getStatus())).count();
            long rejected = list.stream().filter(l -> "REJECTED".equals(l.getStatus())).count();
            long pending = list.stream().filter(l -> "PENDING".equals(l.getStatus())).count();

            Paragraph info = new Paragraph();
            info.setSpacingBefore(8);
            info.setSpacingAfter(8);
            info.add(new Chunk("Period: ", infoBold));
            info.add(new Chunk(period + "      ", infoFont));
            info.add(new Chunk("Status: ", infoBold));
            info.add(new Chunk(statusLabel + "      ", infoFont));
            info.add(new Chunk("Generated on: ", infoBold));
            info.add(new Chunk(DT.format(LocalDateTime.now()), infoFont));
            doc.add(info);

            Paragraph summary = new Paragraph();
            summary.setSpacingAfter(8);
            summary.add(new Chunk("Total: " + list.size() + "     ", infoBold));
            summary.add(new Chunk("Approved: " + approved + "     ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GREEN)));
            summary.add(new Chunk("Not Approved: " + rejected + "     ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, RED)));
            summary.add(new Chunk("Pending: " + pending, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, AMBER)));
            doc.add(summary);

            // --- table ---
            if (list.isEmpty()) {
                Paragraph none = new Paragraph("No leave requests found for the selected period and status.", infoFont);
                none.setSpacingBefore(20);
                none.setAlignment(Element.ALIGN_CENTER);
                doc.add(none);
            } else {
                PdfPTable t = new PdfPTable(new float[]{0.6f, 2f, 2f, 2.4f, 0.8f, 3f, 1.6f, 1.8f, 1.5f});
                t.setWidthPercentage(100);
                t.setHeaderRows(1);
                Font hf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
                for (String c : new String[]{"S.No", "Student", "Reg No / Roll No", "Leave Dates", "Days",
                        "Reason", "Parent Mobile", "Applied On", "Status"}) {
                    PdfPCell hc = new PdfPCell(new Phrase(c, hf));
                    hc.setBackgroundColor(NAVY);
                    hc.setPadding(5);
                    hc.setHorizontalAlignment(Element.ALIGN_CENTER);
                    t.addCell(hc);
                }
                Font cf = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.BLACK);
                int i = 0;
                for (LeaveRequest l : list) {
                    Color bg = (i % 2 == 1) ? ZEBRA : Color.WHITE;
                    i++;
                    String dates = fmt(l.getFromDate()) + " to " + fmt(l.getToDate());
                    long days = (l.getFromDate() != null && l.getToDate() != null)
                            ? ChronoUnit.DAYS.between(l.getFromDate(), l.getToDate()) + 1 : 0;
                    String applied = l.getAppliedOn() == null ? "-" : DT.format(l.getAppliedOn());

                    t.addCell(cell(String.valueOf(i), cf, bg, Element.ALIGN_CENTER));
                    t.addCell(cell(nz(l.getName()), cf, bg, Element.ALIGN_LEFT));
                    t.addCell(cell(nz(l.getRegisterNo()) + " / " + nz(l.getRollNo()), cf, bg, Element.ALIGN_LEFT));
                    t.addCell(cell(dates, cf, bg, Element.ALIGN_CENTER));
                    t.addCell(cell(String.valueOf(days), cf, bg, Element.ALIGN_CENTER));
                    t.addCell(cell(nz(l.getReason()), cf, bg, Element.ALIGN_LEFT));
                    t.addCell(cell(nz(l.getParentMobile()), cf, bg, Element.ALIGN_CENTER));
                    t.addCell(cell(applied, cf, bg, Element.ALIGN_CENTER));

                    String s = nz(l.getStatus());
                    Color sc = "APPROVED".equals(s) ? GREEN : "REJECTED".equals(s) ? RED : AMBER;
                    String label = "REJECTED".equals(s) ? "NOT APPROVED" : s;
                    t.addCell(cell(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, sc), bg, Element.ALIGN_CENTER));
                }
                doc.add(t);
            }

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not build PDF", e);
        }
    }

    private static PdfPCell cell(String text, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setPadding(4);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderColor(new Color(0xD5, 0xDB, 0xE5));
        return c;
    }

    private static String fmt(LocalDate d) { return d == null ? "-" : D.format(d); }

    private static String nz(String s) { return s == null || s.isBlank() ? "-" : s; }

    /** Reads static/images/logo.png; if the file is missing the PDF is still produced (name only). */
    private static Image loadLogo() {
        try (InputStream in = new ClassPathResource("static/images/logo.png").getInputStream()) {
            return Image.getInstance(in.readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }

    /** Adds the footer text and "Page N" at the bottom of every page. */
    private static class PageNumbers extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font f = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase("Kings College of Engineering - Leave Request Portal   |   Page " + writer.getPageNumber(), f),
                    (document.right() + document.left()) / 2, document.bottom() - 20, 0);
        }
    }
}