package com.alin.lin.service;

public interface CurrentUserService {
    String username();

    boolean hasRole(String role);

    boolean securityEnabled();
}
