package com.pecunia.identity.api.mapper;

import com.pecunia.identity.api.dto.CurrentUser;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CurrentUserMapper {

    @Mapping(target = "id", source = "subject", qualifiedByName = "stringToUuid")
    @Mapping(target = "username", source = "preferredUsername")
    @Mapping(target = "displayName", expression = "java(resolveDisplayName(oidcUser))")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "emailVerified", source = "emailVerified", qualifiedByName = "defaultFalse")
    CurrentUser toDto(OidcUser oidcUser);

    @Named("stringToUuid")
    default UUID stringToUuid(String subject) {
        return UUID.fromString(subject);
    }

    default String resolveDisplayName(OidcUser oidcUser) {
        return oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername();
    }

    @Named("defaultFalse")
    default Boolean defaultFalse(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
