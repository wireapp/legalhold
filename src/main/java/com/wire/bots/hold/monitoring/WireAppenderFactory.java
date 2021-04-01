package com.wire.bots.hold.monitoring;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;
import io.dropwizard.request.logging.layout.LogbackAccessRequestLayoutFactory;

/**
 * Production console appender using logging to JSON.
 */
@JsonTypeName("json-console")
public class WireAppenderFactory<T extends DeferredProcessingAware> extends AbstractAppenderFactory<T> {


    // we know that T is either ILoggingEvent or IAccessEvent
    // so tis is in a fact checked cast
    @SuppressWarnings("unchecked")
    @Override
    public Appender<T> build(
            LoggerContext loggerContext,
            String serviceName,
            LayoutFactory<T> layoutFactory,
            LevelFilterFactory<T> levelFilterFactory,
            AsyncAppenderFactory<T> asyncAppenderFactory) {

        final ConsoleAppender<T> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setTarget("System.out");

        Layout<T> layout;
        // this is quite ugly hack to achieve just a single name for the logger
        if (layoutFactory.getClass() == LogbackAccessRequestLayoutFactory.class) {
            layout = (Layout<T>) new AccessEventJsonLayout();
        } else {
            layout = (Layout<T>) new LoggingEventJsonLayout();
        }
        appender.setLayout(layout);
        appender.start();
        return appender;
    }
}
