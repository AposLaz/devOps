package lgl.bayern.de.ecertby.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerConfig {

    @Value("${scheduler.clear-object-locks-cron}")
    private String clearObjectLocksCron;

    @Value("${scheduler.clear-recycle-bin-cron}")
    private String clearRecycleBinCron;

    public String getClearObjectLocksCron() {
        return clearObjectLocksCron;
    }

    public String getRecycleBinCron() {
        return clearRecycleBinCron;
    }
}
