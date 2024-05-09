package lgl.bayern.de.ecertby.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lgl.bayern.de.ecertby.config.SchedulerConfig;
import lgl.bayern.de.ecertby.service.ObjectLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ObjectLockScheduler {
    private final SchedulerConfig schedulerConfig;
    private final ObjectLockService objectLockService;

    @Scheduled(cron = "#{@schedulerConfig.getClearObjectLocksCron()}")
    public void clearObjectLocks() {
        objectLockService.deleteAllLocks();

        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = currentTime.format(formatter);
        String logMessage = "Deleted all object Locks at : " + formattedTime;
        log.info(logMessage);
    }
}
