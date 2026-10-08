package ru.sovmestim.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trade_mark")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeMark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name_brand", nullable = false)
    private String nameBrand;

    private String manufacturer;

    private String country;

    @Column(name = "photo_url", columnDefinition = "text")
    private String photoUrl;
}
