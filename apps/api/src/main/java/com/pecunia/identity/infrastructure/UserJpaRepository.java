package com.pecunia.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByIdpIssuerAndIdpSubject(String issuer, String subject);

    @Modifying
    @Query(value = """
      INSERT INTO users (id, idp_issuer, idp_subject)
      VALUES (:id, :issuer, :subject)
      ON CONFLICT (idp_issuer, idp_subject) DO NOTHING
      """, nativeQuery = true)
    void insertIfAbsent(@Param("id") UUID id, @Param("issuer") String issuer, @Param("subject") String subject);
}
