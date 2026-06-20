package com.codenest.backend.security;

import java.util.function.Supplier;
import org.springframework.http.HttpMethod;
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
    if ("admin".equals(currentUser.role())) {
      return new AuthorizationDecision(true);
    }
    return new AuthorizationDecision(allowsModeratorPostAccess(currentUser, context));
  }

  private boolean allowsModeratorPostAccess(
      CurrentUser currentUser, RequestAuthorizationContext context) {
    if (!"moderator".equals(currentUser.role())) {
      return false;
    }
    String path = context.getRequest().getRequestURI();
    String contextPath = context.getRequest().getContextPath();
    if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
      path = path.substring(contextPath.length());
    }
    String method = context.getRequest().getMethod();
    return (path.equals("/admin/posts") && HttpMethod.GET.matches(method))
        || (path.matches("/admin/posts/\\d+/status") && HttpMethod.PATCH.matches(method));
  }
}
