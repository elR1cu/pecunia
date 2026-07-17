package com.pecunia.category.infrastructure;

import com.pecunia.category.domain.Category;
import com.pecunia.category.domain.CategoryType;
import com.pecunia.category.domain.HexColor;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryEntity {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CategoryType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;

    private String icon;

    private UUID parentId;

    private int displayOrder;

    private boolean archived;

    @Version
    private Long version;

    // read-only, handled by db
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @SuppressWarnings("java:S107") // the entity mirrors the aggregate's full state
    private CategoryEntity(
            UUID id,
            UUID ownerId,
            CategoryType type,
            String name,
            String color,
            String icon,
            UUID parentId,
            int displayOrder,
            boolean archived,
            Long version) {
        this.id = id;
        this.ownerId = ownerId;
        this.type = type;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.parentId = parentId;
        this.displayOrder = displayOrder;
        this.archived = archived;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CategoryEntity{" + "id="
                + id + ", type="
                + type + ", name='"
                + name + '\'' + ", color='"
                + color + '\'' + ", icon='"
                + icon + '\'' + ", ownerId="
                + ownerId + ", parentId="
                + parentId + ", displayOrder="
                + displayOrder + ", archived="
                + archived + ", version="
                + version + ", createdAt="
                + createdAt + '}';
    }

    public static CategoryEntity fromDomain(Category category) {
        return new CategoryEntity(
                category.id().value(),
                category.owner().value(),
                category.type(),
                category.name(),
                category.color().value(),
                category.icon().orElse(null),
                category.parent().map(CategoryId::value).orElse(null),
                category.displayOrder(),
                category.archived(),
                category.version().orElse(null));
    }

    public Category toDomain() {
        return Category.reconstitute(
                CategoryId.of(id),
                UserId.of(ownerId),
                type,
                name,
                new HexColor(color),
                icon,
                Optional.ofNullable(parentId).map(CategoryId::of).orElse(null),
                displayOrder,
                archived,
                version);
    }
}
