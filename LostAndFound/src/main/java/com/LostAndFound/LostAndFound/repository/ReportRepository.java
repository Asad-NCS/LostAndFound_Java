package com.LostAndFound.LostAndFound.repository;

import com.LostAndFound.LostAndFound.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
}