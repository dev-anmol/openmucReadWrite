/*
 * Copyright 2011-2024 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.openmuc.testing.util;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.read.ListAppender;

/**
 * A custom logger to be used in tests. Can be used to test logging behaviour.
 * <p>
 * In order to set up the logger for the class, simply use {@link TestLogger#TestLogger(Class)}. Use the class that
 * shall be used as an argument. All logging will be appended to this logger.
 * <p>
 * Use {@link #resetLogs()} to reset the list of log messages that this logger received.
 * <p>
 * Use {@link #getLoggedLinesWithLevel(Level)} or {@link #getLoggedEvents()} to make asserts on the logs.
 */
public class TestLogger extends ListAppender<ILoggingEvent> {

    public TestLogger(Class<?> classToAddLogsFrom) {
        setAsLoggerFor(classToAddLogsFrom);
    }

    private void setAsLoggerFor(Class<?> classToAddLogsFrom) {
        Logger currentLogger = (Logger) LoggerFactory.getLogger(classToAddLogsFrom);
        currentLogger.setLevel(Level.ALL);
        setContext((Context) LoggerFactory.getILoggerFactory());
        currentLogger.addAppender(this);
        start();
    }

    public void resetLogs() {
        this.list.clear();
    }

    public List<ILoggingEvent> getLoggedEvents() {
        return Collections.unmodifiableList(this.list);
    }

    public List<String> getLoggedLinesWithLevel(Level level) {
        List<String> loglines = getLoggedEvents().stream()
                .filter(event -> level.equals(event.getLevel()))
                .map(event -> event.getFormattedMessage())
                .collect(Collectors.toList());
        return loglines;
    }

}
