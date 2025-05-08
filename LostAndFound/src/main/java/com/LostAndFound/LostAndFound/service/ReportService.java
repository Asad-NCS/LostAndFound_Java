package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.ReportDTO;
import com.LostAndFound.LostAndFound.model.Admin;
import com.LostAndFound.LostAndFound.model.Post;
import com.LostAndFound.LostAndFound.model.Report;
import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.repository.AdminRepository;
import com.LostAndFound.LostAndFound.repository.PostRepository;
import com.LostAndFound.LostAndFound.repository.ReportRepository;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final AdminRepository adminRepository;

    public ReportDTO createReport(ReportDTO reportDTO) {
        Report report = new Report();
        report.setReason(reportDTO.getReason());
        report.setReportedAt(LocalDateTime.now());

        User user = userRepository.findById(reportDTO.getReporterId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepository.findById(reportDTO.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found"));

        report.setUser(user);
        report.setPost(post);

        if (reportDTO.getAdminId() != null) {
            Admin admin = adminRepository.findById(reportDTO.getAdminId())
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
            report.setAdmin(admin);
        }

        return toDTO(reportRepository.save(report));
    }

    public ReportDTO getReportById(Long id) {
        return reportRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Report not found"));
    }

    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public void deleteReport(Long id) {
        reportRepository.deleteById(id);
    }

    private ReportDTO toDTO(Report report) {
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setReason(report.getReason());
        dto.setReporterId(report.getUser() != null ? report.getUser().getId() : null);
        dto.setPostId(report.getPost() != null ? report.getPost().getId() : null);
        dto.setAdminId(report.getAdmin() != null ? report.getAdmin().getId() : null);
        return dto;
    }
}
