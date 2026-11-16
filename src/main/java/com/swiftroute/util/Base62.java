package com.swiftroute.util;

import java.security.SecureRandom;

public final class Base62 {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 7;

    private Base62() {};

    public static String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int randomIndex = RANDOM.nextInt(BASE);
            sb.append(ALPHABET.charAt(randomIndex));
        }
        return sb.toString();
    }
}
