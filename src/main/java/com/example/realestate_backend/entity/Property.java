package com.example.realestate_backend.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @PositiveOrZero(message = "Price must be zero or greater")
    @NotNull(message = "Price is required")
    private Double price;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Property type is required")
    private PropertyType type;

    @NotBlank(message = "Location is required")
    private String location;

    private String imageUrl;

    @Builder.Default
    private boolean approved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private LocalDateTime dateListed;

    @PrePersist
    protected void onCreate() {
        this.dateListed = LocalDateTime.now();
    }
}