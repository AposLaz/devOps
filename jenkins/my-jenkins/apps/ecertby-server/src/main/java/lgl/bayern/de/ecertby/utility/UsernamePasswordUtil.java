package lgl.bayern.de.ecertby.utility;

import java.security.SecureRandom;

public class UsernamePasswordUtil {

    private static final SecureRandom random = new SecureRandom();

    private UsernamePasswordUtil() {
    }

    public static String generateUsernameOrPassword(final Boolean isUsername) {

        String symbols = null;
        String charsCapitals = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String charsLow = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String pass = "pass_";
        String staticPartOfSymbols = "!_-&$%?*";
        String user = "user-";

        int number = 4;
        final StringBuilder stringBuilder = new StringBuilder();
        if (Boolean.TRUE.equals(isUsername)) {
            stringBuilder.append(user);
            symbols = "-";
        } else {
            stringBuilder.append(pass);
            symbols = staticPartOfSymbols;
        }

        for (int i = 0; i < number; i++) {
            stringBuilder.append(charsCapitals.charAt(random.nextInt(charsCapitals.length())));
            stringBuilder.append(charsLow.charAt(random.nextInt(charsLow.length())));
            stringBuilder.append(numbers.charAt(random.nextInt(numbers.length())));
            stringBuilder.append(symbols.charAt(random.nextInt(symbols.length())));
        }

        return stringBuilder.toString();
    }


}
