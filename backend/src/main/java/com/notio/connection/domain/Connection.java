package com.notio.connection.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "connections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String provider = "CLAUDE";

    @Column(name = "auth_type", nullable = false, length = 50)
    @Builder.Default
    private String authType = "API_KEY";

    @Column(name = "display_name", nullable = false, length = 100)
    @Builder.Default
    private String displayName = "Connection";

    @Column(name = "account_label", length = 255)
    private String accountLabel;

    @Column(name = "external_account_id", length = 255)
    private String externalAccountId;

    @Column(name = "external_workspace_id", length = 255)
    private String externalWorkspaceId;

    @Column(name = "subscription_id", length = 255)
    private String subscriptionId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String capabilities = "[]";

    @Column(nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String metadata = "{}";

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
