package org.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dto.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

import java.io.IOException;
import java.util.*;

public class UserAuthorisationFilter extends AbstractAuthenticationProcessingFilter {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final String COOKIE_NAME = "MY_SESSION";

    public UserAuthorisationFilter() {
        super("/**");
    }

    private Map<String, String> sessionMap = new HashMap<>();

    {
        var generatedSessionId = UUID.randomUUID().toString();
        log.info("Generated session id " + generatedSessionId);
        sessionMap.put("0cdbea5a-ae96-4eaa-a0e7-bed40d3ec47c", "nikita");

        setAuthenticationFailureHandler(new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                logger.debug("Sending 401 Unauthorized error");
                response.sendError(
                        HttpStatus.UNAUTHORIZED.value(),
                        exception.getMessage()
                );
            }
        });
        setAuthenticationSuccessHandler(new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

            }
        });
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
        chain.doFilter(request, response);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        var cookies = request.getCookies();
        if (cookies == null) throw new SessionAuthenticationException("Отсутствует сессия в куки");
        var sessionFromCookie = Arrays.stream(cookies).filter((cookie -> {
            return cookie.getName().equals(COOKIE_NAME);
        })).findFirst();

        if (sessionFromCookie.isPresent()) {
            var sessionId = sessionFromCookie.get().getValue();
            var username = sessionMap.get(sessionId);
            if (username == null) {
                throw new SessionAuthenticationException("Пользователь не авторизован");
            }
            var authentication = new PreAuthenticatedAuthenticationToken(
                    new UserPrincipal(username),
                    "",
                    List.of()
            );

            return authentication;
        } else {
            throw new SessionAuthenticationException("Отсутствует сессия в куки");
        }
    }

}
