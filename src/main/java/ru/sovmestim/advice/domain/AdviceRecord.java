package ru.sovmestim.advice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable audit row for every advice check: what was asked, what the engine answered, and which
 * rule/catalog versions produced it.
 */
@Entity
@Table(name = "advice_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdviceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "drug_name")
    private String drugName;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(length = 64)
    private String level;

    @Column(name = "rules_version", length = 64)
    private String rulesVersion;

    @Column(name = "mapping_version", length = 64)
    private String mappingVersion;

    @Column(name = "catalog_version", length = 64)
    private String catalogVersion;

    @Column(name = "request_json", columnDefinition = "text")
    private String requestJson;

    @Column(name = "result_json", columnDefinition = "text")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
