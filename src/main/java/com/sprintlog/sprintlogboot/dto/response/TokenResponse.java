package com.sprintlog.sprintlogboot.dto.response;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken
) {
    @Deprecated
    public static TokenResponse bearer(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, "Bearer", expiresInSeconds, null);
    }

    public static TokenResponse bearer(String accessToken, long expiresInSeconds, String refresh) {
        return new TokenResponse(accessToken, "Bearer", expiresInSeconds, refresh);
    }
}
