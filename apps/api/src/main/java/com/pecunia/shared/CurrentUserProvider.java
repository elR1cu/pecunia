package com.pecunia.shared;

/**
 * Port exposing the internal identifier of the currently authenticated user.
 *
 * <p>Implemented by a security adapter that reads the {@code SecurityContext}; consumed by
 * web controllers to stamp ownership. It lives in the shared kernel — like {@link IdGenerator} —
 * so every bounded context can resolve the owner without depending on the security layer, which
 * would otherwise create a package cycle (enforced by the ArchUnit cycle rule).
 */
public interface CurrentUserProvider {

    UserId currentUserId();
}
