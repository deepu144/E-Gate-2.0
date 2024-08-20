package com.kce.egate.config;

import com.kce.egate.util.BackupUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BackupScheduler {
    private final BackupUtils backupUtils;

    @Scheduled(cron = "0 0 3 ? * MON")
    public void scheduleWeeklyBackup() {
        backupUtils.backupDB();
    }
}
