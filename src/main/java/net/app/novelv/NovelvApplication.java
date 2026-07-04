package net.app.novelv;

import net.app.novelv.domain.video.CloudflareR2Properties;
import net.app.novelv.domain.video.FfmpegProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({CloudflareR2Properties.class, FfmpegProperties.class})
public class NovelvApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovelvApplication.class, args);
    }
}
