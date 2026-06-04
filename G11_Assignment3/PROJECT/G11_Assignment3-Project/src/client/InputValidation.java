package client;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Centralized input validation for all client screens.
 */
public class InputValidation {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9\\-+() ]{7,20}$");
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{1,15}$");

    public static String validateId(String id) {
        if (id == null || id.trim().isEmpty()) return "ID number is required.";
        if (!ID_PATTERN.matcher(id.trim()).matches()) return "ID must contain only digits.";
        if (id.trim().length() > 9) return "ID number must be up to 9 digits.";
        if (!isValidIsraeliId(id.trim())) return "Invalid Israeli ID number.";
        return null;
    }

    /**
     * Validates Israeli ID number (Teudat Zehut) using the checksum algorithm.
     * The ID is 9 digits. Each digit is multiplied by 1 or 2 alternately.
     * If result > 9, subtract 9. Sum must be divisible by 10.
     */
    public static boolean isValidIsraeliId(String id) {
        // Pad with leading zeros to 9 digits
        while (id.length() < 9) id = "0" + id;
        if (id.length() != 9) return false;

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = Character.getNumericValue(id.charAt(i));
            int multiplier = (i % 2 == 0) ? 1 : 2;
            int result = digit * multiplier;
            if (result > 9) result -= 9;
            sum += result;
        }
        return sum % 10 == 0;
    }

    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) return "Email is required.";
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) return "Invalid email format.";
        return null;
    }

    public static String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return null; // phone is optional
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) return "Invalid phone format.";
        return null;
    }

    public static String validateVisitors(String visitorsStr, int max) {
        if (visitorsStr == null || visitorsStr.trim().isEmpty()) return "Number of visitors is required.";
        try {
            int visitors = Integer.parseInt(visitorsStr.trim());
            if (visitors <= 0) return "Number of visitors must be positive.";
            if (visitors > max) return "Maximum " + max + " visitors allowed.";
            return null;
        } catch (NumberFormatException e) { return "Visitors must be a whole number."; }
    }

    public static String validateDate(LocalDate date) {
        if (date == null) return "Date is required.";
        if (date.isBefore(LocalDate.now())) return "Date cannot be in the past.";
        return null;
    }

    public static String validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) return fieldName + " is required.";
        return null;
    }

    public static String validatePositiveNumber(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) return fieldName + " is required.";
        try {
            double num = Double.parseDouble(value.trim());
            if (num <= 0) return fieldName + " must be positive.";
            return null;
        } catch (NumberFormatException e) { return fieldName + " must be a number."; }
    }
}