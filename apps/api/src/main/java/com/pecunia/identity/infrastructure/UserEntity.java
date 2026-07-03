package com.pecunia.identity.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String idpIssuer;

    @Column(nullable = false, updatable = false)
    private String idpSubject;

    // read-only, handled by db
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserEntity that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "UserEntity{" + "id="
                + id + ", idpIssuer='"
                + idpIssuer + '\'' + ", idpSubject='"
                + idpSubject + '\'' + ", createdAt="
                + createdAt + '}';
    }
}
