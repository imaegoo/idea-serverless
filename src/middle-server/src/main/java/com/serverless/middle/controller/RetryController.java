package com.serverless.middle.controller;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RetryController {

    public static void doRetry(int count, Long interval, Supplier<Boolean> supplier, Supplier<Boolean> afterSleep) throws Exception {
        while (!supplier.get()) {
            if (count == 0) {
                throw new Exception("retry over");
            }
            if (interval > 0) {
                TimeUnit.MILLISECONDS.sleep(interval);
            }
            if (afterSleep != null && afterSleep.get()) {
                break;
            }
            count--;
        }
    }

    public static void doRetry(int count, Long interval, Supplier<Boolean> supplier) throws Exception {
        doRetry(count, interval, supplier, null);
    }
}
