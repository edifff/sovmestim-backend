package ru.sovmestim.advice.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.advice.domain.DangerLevel;
import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.advice.repository.DangerLevelRepository;

/**
 * Seeds the danger-level dictionary owned by the advice service. The dictionary is needed to store
 * cached interactions; it is independent of the catalog demo switch.
 */
@Component
@RequiredArgsConstructor
public class AdviceDemoDataSeeder implements ApplicationRunner {

    private final DangerLevelRepository dangerLevelRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (AdviceLevel level : AdviceLevel.values()) {
            dangerLevelRepository
                    .findByNameIgnoreCase(level.name())
                    .orElseGet(() -> dangerLevelRepository.save(
                            DangerLevel.builder().name(level.name()).build()));
        }
    }
}
