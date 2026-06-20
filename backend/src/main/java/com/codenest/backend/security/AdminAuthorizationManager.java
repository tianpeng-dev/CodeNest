package com.codenest.backend.security;

import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {
  private final CurrentUserProvider currentUserProvider;

  public AdminAuthorizationManager(CurrentUserProvider currentUserProvider) {
    this.currentUserProvider = currentUserProvider;
  }

  @Override
  public AuthorizationDecision check(
      Supplier<Authentication> authentication, RequestAuthorizationContext context) {
    Authentication currentAuthentication = authentication.get();
    if (currentAuthentication == null
        || !currentAuthentication.isAuthenticated()
        || !(currentAuthentication.getPrincipal() instanceof Jwt)) {
      return new AuthorizationDecision(false);
    }

    CurrentUser currentUser = currentUserProvider.requireCurrentUser();
    return new AuthorizationDecision("admin".equals(currentUser.role()));
  }
}
