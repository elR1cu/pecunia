package com.pecunia.category.domain;

import com.pecunia.category.domain.exception.ArchivedCategoryModificationException;
import com.pecunia.category.domain.exception.CategoryAlreadyArchivedException;
import com.pecunia.category.domain.exception.CategoryCannotBeItsOwnParentException;
import com.pecunia.sharedkernel.CategoryId;
import com.pecunia.sharedkernel.UserId;
import java.util.Objects;
import java.util.Optional;

public final class Category {

    private final CategoryId id;
    private final UserId owner;
    private final CategoryType type;
    private String name;
    private HexColor color;
    private String icon;
    private CategoryId parent;
    private int displayOrder;
    private boolean archived;
    private final Long version;

    // S107: an aggregate root legitimately carries its full state; a parameter object would
    // fragment the model without cohesion.
    @SuppressWarnings("java:S107")
    private Category(
            CategoryId id,
            UserId owner,
            CategoryType type,
            String name,
            HexColor color,
            String icon,
            CategoryId parent,
            int displayOrder,
            boolean archived,
            Long version) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.owner = Objects.requireNonNull(owner, "owner cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.color = Objects.requireNonNull(color, "color cannot be null");
        this.name = validateName(name);
        this.parent = validateParent(parent);
        this.icon = normalizeIconIfPresent(icon);
        this.displayOrder = validateDisplayOrder(displayOrder);
        this.archived = archived;
        this.version = version;
    }

    @SuppressWarnings("java:S107") // creates the aggregate before persisting it
    public static Category create(
            CategoryId id,
            UserId owner,
            CategoryType type,
            String name,
            HexColor color,
            String icon,
            CategoryId parent,
            int displayOrder) {
        return new Category(id, owner, type, name, color, icon, parent, displayOrder, false, null);
    }

    @SuppressWarnings("java:S107") // rehydrates the full aggregate state from persistence
    public static Category reconstitute(
            CategoryId id,
            UserId owner,
            CategoryType type,
            String name,
            HexColor color,
            String icon,
            CategoryId parent,
            int displayOrder,
            boolean archived,
            Long version) {
        return new Category(id, owner, type, name, color, icon, parent, displayOrder, archived, version);
    }

    public void rename(String newName) {
        validateIsNotArchived();
        this.name = validateName(newName);
    }

    public void recolor(HexColor newColor) {
        validateIsNotArchived();
        this.color = Objects.requireNonNull(newColor, "color cannot be null");
    }

    public void reicon(String newIcon) {
        validateIsNotArchived();
        this.icon = normalizeIconIfPresent(newIcon);
    }

    public void reorder(int newDisplayOrder) {
        validateIsNotArchived();
        this.displayOrder = validateDisplayOrder(newDisplayOrder);
    }

    public void archive() {
        if (archived) {
            throw new CategoryAlreadyArchivedException(id);
        }
        this.archived = true;
    }

    public void moveTo(CategoryId newParent) {
        validateIsNotArchived();
        this.parent = validateParent(newParent);
    }

    private int validateDisplayOrder(int displayOrder) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Display order cannot be negative");
        }
        return displayOrder;
    }

    private void validateIsNotArchived() {
        if (archived) {
            throw new ArchivedCategoryModificationException(id);
        }
    }

    private CategoryId validateParent(CategoryId newParent) {
        if (newParent != null && newParent.equals(id)) {
            throw new CategoryCannotBeItsOwnParentException(id);
        }
        return newParent;
    }

    private String normalizeIconIfPresent(String icon) {
        return Optional.ofNullable(icon).map(String::strip).orElse(null);
    }

    private String validateName(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        return name.strip();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Category other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Category{" + "id="
                + id + ", owner="
                + owner + ", type="
                + type + ", name='"
                + name + '\'' + ", color="
                + color + ", icon='"
                + icon + '\'' + ", parent="
                + parent + ", displayOrder="
                + displayOrder + ", archived="
                + archived + ", version="
                + version + '}';
    }

    public CategoryId id() {
        return id;
    }

    public UserId owner() {
        return owner;
    }

    public CategoryType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public Optional<String> icon() {
        return Optional.ofNullable(icon);
    }

    public HexColor color() {
        return color;
    }

    public Optional<CategoryId> parent() {
        return Optional.ofNullable(parent);
    }

    public int displayOrder() {
        return displayOrder;
    }

    public boolean archived() {
        return archived;
    }

    public Optional<Long> version() {
        return Optional.ofNullable(version);
    }
}
