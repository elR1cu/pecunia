package com.pecunia.sharedkernel;

/**
 * Port exposing the internal identifier of the currently authenticated user.
 *
 * <p>Implemented by a security adapter that reads the {@code SecurityContext}; consumed by the
 * application services, which resolve the tenant owner themselves rather than receiving it from
 * an adapter — commands and queries carry no {@code UserId} (ADR-0033, enforced by ArchUnit). It
 * lives in the shared kernel — like {@link IdGenerator} — so every bounded context can resolve
 * the owner without depending on the security layer, which would otherwise create a package
 * cycle (enforced by the ArchUnit cycle rule).
 */
public interface CurrentUserProvider {

    UserId currentUserId();
}
