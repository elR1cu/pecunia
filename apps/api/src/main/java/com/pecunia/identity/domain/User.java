package com.pecunia.identity.domain;

import com.pecunia.sharedkernel.UserId;
import java.util.Objects;

public final class User {

    private final UserId id;
    private final IdpIdentity idpIdentity;

    private User(UserId id, IdpIdentity idpIdentity) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.idpIdentity = Objects.requireNonNull(idpIdentity, "idpIdentity cannot be null");
    }

    public static User register(UserId id, IdpIdentity idpIdentity) {
        return new User(id, idpIdentity);
    }

    public static User reconstitute(UserId id, IdpIdentity idpIdentity) {
        return new User(id, idpIdentity);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", idpIdentity=" + idpIdentity + '}';
    }

    public UserId id() {
        return id;
    }

    public IdpIdentity idpIdentity() {
        return idpIdentity;
    }
}
