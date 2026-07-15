package com.chargesquare.session.auth;

@FunctionalInterface
public interface ServiceTokenProvider {
    String issueServiceToken();
}
