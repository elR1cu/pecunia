package com.pecunia.identity.web.mapper;

import com.pecunia.identity.web.dto.CurrentUser;
import com.pecunia.sharedkernel.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CurrentUserMapper {

    @Mapping(target = "id", source = "userId.value")
    @Mapping(target = "username", source = "oidcUser.preferredUsername")
    @Mapping(target = "displayName", expression = "java(resolveDisplayName(oidcUser))")
    @Mapping(target = "email", source = "oidcUser.email")
    @Mapping(target = "emailVerified", source = "oidcUser.emailVerified", qualifiedByName = "defaultFalse")
    CurrentUser toDto(OidcUser oidcUser, UserId userId);

    default String resolveDisplayName(OidcUser oidcUser) {
        return oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getPreferredUsername();
    }

    @Named("defaultFalse")
    default Boolean defaultFalse(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
