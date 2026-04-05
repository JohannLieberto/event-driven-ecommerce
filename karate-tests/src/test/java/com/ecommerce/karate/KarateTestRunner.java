package com.ecommerce.karate;

import com.intuit.karate.junit5.Karate;

public class KarateTestRunner {

    @Karate.Test
    Karate testAll() {
        return Karate.run("classpath:features")
                     .outputHtmlReport(true)
                     .outputJunitXml(true);
    }
}
