package io.github.kocoding.taskmanager.team.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class TeamCodeHasher {

    public String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // 呼び出し元に throws を強制しないように非検査例外にする
            throw new IllegalArgumentException("SHA-256 が利用できません", e);
        }
    }

}
