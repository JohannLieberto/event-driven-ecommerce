package notifications;

import com.intuit.karate.junit5.Karate;

class NotificationsTestRunner {

    @Karate.Test
    Karate testNotifications() {
        return Karate.run("order-status-emails").relativeTo(getClass());
    }
}