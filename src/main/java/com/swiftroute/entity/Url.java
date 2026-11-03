package com.swiftroute.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "urls",
        indexes = {
                @Index(name = "idx_urls_short_code", columnList = "short_code"),
                @Index(name = "idx_urls_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor
@Getter
public class Url extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Setter
    @Column(name = "short_code", nullable = false, unique = true, length = 12)
    private String shortCode;

    @Setter
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_urls_user_id"))
    private User user;

    public Url(String originalUrl, String shortCode, Instant expiresAt, User user) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Url other)) return false;
        return shortCode != null && shortCode.equals(other.shortCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortCode);
    }
}
