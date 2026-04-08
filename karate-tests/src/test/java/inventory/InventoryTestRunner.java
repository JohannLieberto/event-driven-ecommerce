package inventory;

import com.intuit.karate.junit5.Karate;

public class InventoryTestRunner {

    @Karate.Test
    Karate testInventoryReservation() {
        return Karate.run("inventory-reservation").relativeTo(getClass());
    }
}