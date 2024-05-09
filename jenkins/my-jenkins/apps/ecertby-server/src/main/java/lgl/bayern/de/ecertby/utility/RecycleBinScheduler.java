package lgl.bayern.de.ecertby.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lgl.bayern.de.ecertby.config.SchedulerConfig;
import lgl.bayern.de.ecertby.service.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecycleBinScheduler {
    private final CertificateService certificateService;

    @Scheduled(cron = "#{@schedulerConfig.getRecycleBinCron()}")
    public void clearRecycleBin() {
        certificateService.clearDeletedCertificates();

        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = currentTime.format(formatter);
        String logMessage = "Checked and cleared recycle bin at : " + formattedTime;
        log.info(logMessage);
    }
}
