package ru.sovmestim.config;

import java.time.Clock;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Central time source. Every application timestamp comes from the single {@link Clock} bean, so code
 * and token expiry can be controlled in tests. The same clock feeds Spring Data JPA auditing, which
 * stamps the {@code @LastModifiedDate} fields of the audited entities.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class TimeConfig {

    /**
     * The application clock, in UTC.
     *
     * @return the clock used for all timestamps
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Feeds the application clock to Spring Data JPA auditing.
     *
     * @param clock the application clock
     * @return the provider that stamps audited entities
     */
    @Bean
    DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }
}
