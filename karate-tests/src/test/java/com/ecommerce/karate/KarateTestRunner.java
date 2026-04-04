package com.ecommerce.karate;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.Test;

public class KarateTestRunner {

    @Test
    void testAll() {
        Karate.run("classpath:features")
            .relativeTo(getClass())
            .outputHtmlReport(true)
            .outputJunitXml(true);
    }
}
