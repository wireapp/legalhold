package com.wire.bots.hold.utils;

import org.junit.Test;

import java.text.ParseException;

public class CollectorTest {

    @Test
    public void date() throws ParseException {
        String s = Collector.toDate("2019-07-04T10:36:02.693Z");
    }

    @Test
    public void time() throws ParseException {
        String s = Collector.toTime("2019-07-04T10:36:02.693Z");
    }
}