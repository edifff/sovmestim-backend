package ru.sovmestim.patient.domain;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * ICD-10 code.
 */
@Entity
@Table(name = "mkb")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mkb {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String code;

    @Column(nullable = false)
    private String name;
}
