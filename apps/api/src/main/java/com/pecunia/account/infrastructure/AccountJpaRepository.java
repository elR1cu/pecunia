package com.pecunia.account.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<AccountEntity> findAllByOwnerIdOrderByCreatedAtAsc(UUID ownerId);
}
