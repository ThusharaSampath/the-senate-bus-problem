package com.msccse;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    public void testGenerateExponentialDelay() {
        long mean = 1000;
        long delay = App.generateExponentialDelay(mean);
        assertTrue("Delay should be non-negative", delay >= 0);
    }
}
