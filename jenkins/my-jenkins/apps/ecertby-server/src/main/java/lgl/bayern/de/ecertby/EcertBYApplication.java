package lgl.bayern.de.ecertby;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableCaching
@EntityScan({"com.eurodyn.qlack.fuse", "com.eurodyn.qlack.util", "lgl.bayern.de.ecertby"})
@ComponentScan({"com.eurodyn.qlack.fuse", "com.eurodyn.qlack.util", "lgl.bayern.de.ecertby"})
@EnableJpaRepositories({"com.eurodyn.qlack.fuse", "com.eurodyn.qlack.util", "lgl.bayern.de.ecertby"})
@EnableScheduling
public class EcertBYApplication {

  public static void main(String[] args) {
    SpringApplication.run(EcertBYApplication.class, args);
  }

  @PostConstruct
  void started() {
    log.info("Timezone name  : {}", TimeZone.getDefault().getDisplayName());
    log.info("Timezone ID    : {}", TimeZone.getDefault().getID());
    log.info("Timezone offset: {} minutes", TimeUnit.MILLISECONDS.toMinutes(TimeZone.getDefault().getRawOffset()));
    log.info("Application started.");
  }

}
