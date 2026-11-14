package io.github.kocoding.taskmanager.team.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class TeamCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 12;
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }

        return sb.toString();
    }

}
