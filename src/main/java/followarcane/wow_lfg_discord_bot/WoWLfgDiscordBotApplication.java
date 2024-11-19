package followarcane.wow_lfg_discord_bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@PropertySource("classpath:application.yml")
@Slf4j
public class WoWLfgDiscordBotApplication {

	// @Value anotasyonu ile ortam değişkenlerini okuyalım
	@Value("${spring.datasource.url}")
	private String dbUrl;

	@Value("${spring.datasource.username}")
	private String dbUsername;

	@Value("${spring.datasource.password}")
	private String dbPassword;

	public static void main(String[] args) {
		SpringApplication.run(WoWLfgDiscordBotApplication.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return args -> {
            log.info("Database URL: {}", dbUrl);
            log.info("Database Username: {}", dbUsername);
            log.info("Database Password: {}", dbPassword);
		};
	}
}
