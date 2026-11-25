package ru.sovmestim.catalog.domain;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A specific drug product (trade name + release form).
 */
@Entity
@Table(name = "medicine")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_release_id")
    private FormRelease formRelease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_mark_id")
    private TradeMark tradeMark;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;
}
