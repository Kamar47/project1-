package common;

public class Pricing {
    // Full price is per park
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
