package client;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Centralized input validation for all client screens.
 */
public class InputValidation {
	private static final Pattern EMAIL_PATTERN =Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =Pattern.compile("^05\\d{8}$");
	private static final Pattern ID_PATTERN = Pattern.compile("^\\d{1,15}$");

	/**
	 * Validates an Israeli ID number entered by the user.
	 *
	 * @param id the ID number to validate
	 * @return null if the ID is valid, otherwise an error message
	 */
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

    /**
     * Validates an email address entered by the user.
     *
     * @param email the email address to validate
     * @return null if the email is valid, otherwise an error message
     */
    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required.";
        }

        String cleanEmail = email.trim();

        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            return "Invalid email format. Example: name@example.com";
        }

        return null;
    }

    /**
     * Validates an Israeli mobile phone number.
     * The method allows numbers with spaces or hyphens and normalizes them before validation.
     *
     * @param phone the phone number to validate
     * @return null if the phone number is valid, otherwise an error message
     */
    public static String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Phone number is required.";
        }

        /*
         * Allows:
         * 0501234567
         * 050-1234567
         * 050 1234567
         */
        String cleanPhone = phone.trim()
                .replace("-", "")
                .replace(" ", "");

        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return "Invalid phone number. Phone must start with 05 and contain exactly 10 digits.";
        }

        return null;
    }

    /**
     * Validates the number of visitors entered by the user.
     *
     * @param visitorsStr the number of visitors as text
     * @param max the maximum allowed number of visitors
     * @return null if the number is valid, otherwise an error message
     */
    public static String validateVisitors(String visitorsStr, int max) {
        if (visitorsStr == null || visitorsStr.trim().isEmpty()) return "Number of visitors is required.";
        try {
            int visitors = Integer.parseInt(visitorsStr.trim());
            if (visitors <= 0) return "Number of visitors must be positive.";
            if (visitors > max) return "Maximum " + max + " visitors allowed.";
            return null;
        } catch (NumberFormatException e) { return "Visitors must be a whole number."; }
    }

    /**
     * Validates a selected visit date.
     * The date must not be empty and must not be in the past.
     *
     * @param date the selected date
     * @return null if the date is valid, otherwise an error message
     */
    public static String validateDate(LocalDate date) {
        if (date == null) return "Date is required.";
        if (date.isBefore(LocalDate.now())) return "Date cannot be in the past.";
        return null;
    }

    /**
     * Validates that a required text field is not empty.
     *
     * @param value the field value to validate
     * @param fieldName the name of the field displayed in the error message
     * @return null if the value is not empty, otherwise an error message
     */
    public static String validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) return fieldName + " is required.";
        return null;
    }

    /**
     * Validates that a numeric field contains a positive number.
     *
     * @param value the numeric value as text
     * @param fieldName the name of the field displayed in the error message
     * @return null if the value is a positive number, otherwise an error message
     */
    public static String validatePositiveNumber(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) return fieldName + " is required.";
        try {
            double num = Double.parseDouble(value.trim());
            if (num <= 0) return fieldName + " must be positive.";
            return null;
        } catch (NumberFormatException e) { return fieldName + " must be a number."; }
    }
}