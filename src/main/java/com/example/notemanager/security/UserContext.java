package com.example.notemanager.security;

import com.example.notemanager.model.User;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserContext {
    private User cachedUser;

    public User getCachedUser() {
        return cachedUser;
    }

    public void setCachedUser(User user) {
        this.cachedUser = user;
    }
}