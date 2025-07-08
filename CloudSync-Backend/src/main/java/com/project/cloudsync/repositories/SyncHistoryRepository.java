package com.project.cloudsync.repositories;

import com.project.cloudsync.entities.SyncHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncHistoryRepository extends JpaRepository<SyncHistory, Long> {
}
