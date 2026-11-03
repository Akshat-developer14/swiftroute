package com.swiftroute.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "url_clicks",
        indexes = {
                @Index(name = "idx_url_clicks_url_id", columnList = "url_id"),
                @Index(name = "idx_url_clicks_clicked_at", columnList = "clicked_at")
        }
)
@NoArgsConstructor
@Getter
public class UrlClick {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_id", nullable = false, foreignKey = @ForeignKey(name = "fk_url_clicks_url_id"))
    private Url url;

    @CreatedDate
    @Column(name = "clicked_at", nullable = false, updatable = false)
    private Instant clickedAt;

    @Setter
    @Column(name = "referrer", length = 512)
    private String referrer;

    @Setter
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Setter
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public UrlClick(Url url, String referrer, String userAgent, String ipAddress) {
        this.url = url;
        this.referrer = referrer;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }
}
