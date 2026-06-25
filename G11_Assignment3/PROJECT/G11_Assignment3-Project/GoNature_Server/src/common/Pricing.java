package common;

/**
 * Stateless utility class that calculates the total price for a park visit booking.
 * <p>
 * Pricing rules (per visitor, based on the full park price):
 * </p>
 * <ul>
 *   <li><b>individual / family (reserved):</b> 15% discount</li>
 *   <li><b>walk-in (individual):</b> full price (no discount)</li>
 *   <li><b>organized group (reserved):</b> 25% discount; additional 12% if paid in advance;
 *       the guide enters free of charge</li>
 *   <li><b>walk-in group:</b> 10% discount; guide pays</li>
 *   <li><b>subscriber bonus:</b> additional 10% off on top of the applicable discount</li>
 * </ul>
 * <p>Active promotions approved by the department manager may apply an additional
 * percentage discount, handled separately in {@code MessageHandler}.</p>
 *
 * @author Group 11
 */
public class Pricing {
    // Full price is per park
    /**
     * Calculates the total price for a park visit booking.
     *
     * @param orderType     the booking type: {@code individual}, {@code family},
     *                      {@code walk_in}, {@code organized_group}, or {@code walk_in_group}
     * @param numVisitors   the total number of visitors including the guide (if any)
     * @param fullPrice     the park's base price per visitor
     * @param isSubscriber  {@code true} if the booker is a family-club subscriber
     *                      (applies an additional 10% discount)
     * @param paidInAdvance {@code true} for organized groups paying in advance
     *                      (applies an additional 12% discount)
     * @return the total price for all paying visitors, rounded to 2 decimal places
     */
    public static double calculatePrice(String orderType, int numVisitors, double fullPrice,
                                         boolean isSubscriber, boolean paidInAdvance) {
        double pricePerPerson = fullPrice;
        boolean guideFree = false;

        switch (orderType) {
            case "individual": case "family":
                pricePerPerson = fullPrice * 0.85;  // 15% discount
                break;
            case "walk_in":
                pricePerPerson = fullPrice;          // full price
                break;
            case "organized_group":
                pricePerPerson = fullPrice * 0.75;  // 25% discount
                if (paidInAdvance) pricePerPerson *= 0.88;  // extra 12% off
                guideFree = true;
                break;
            case "walk_in_group":
                pricePerPerson = fullPrice * 0.90;  // 10% discount
                break;
        }

        if (isSubscriber) {
            pricePerPerson *= 0.90;  // extra 10% for subscribers
        }

        int payingVisitors = guideFree ? numVisitors - 1 : numVisitors;
        if (payingVisitors < 0) payingVisitors = 0;
        return Math.round(pricePerPerson * payingVisitors * 100.0) / 100.0;
    }
}
