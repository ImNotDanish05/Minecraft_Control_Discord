package com.example.discordcontrol.features;

import java.util.Random;
import java.util.stream.Collectors;

public final class GagFilter {
    private static final Random R = new Random();
    public static volatile boolean gagged = false;

    private GagFilter() {}

    public static String gagify(String msg) {
        StringBuilder out = new StringBuilder();
        for (String token : msg.split("(?<=\\b)|(?=\\b)")) {
            if (token.matches("[A-Za-zÀ-ÖØ-öø-ÿ]+")) {
                char c = choose();
                String repl = token.chars().mapToObj(i -> String.valueOf(c)).collect(Collectors.joining());
                if (Character.isUpperCase(token.charAt(0))) {
                    repl = repl.substring(0,1).toUpperCase() + repl.substring(1);
                }
                out.append(repl);
            } else {
                out.append(token);
            }
        }
        return out.toString();
    }

    private static char choose(){
        int p = R.nextInt(10);
        return p < 7 ? 'n' : (p < 9 ? 'm' : 'h');
    }
}
