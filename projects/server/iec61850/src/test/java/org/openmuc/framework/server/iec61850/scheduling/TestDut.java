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
package org.openmuc.framework.server.iec61850.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.openmuc.framework.server.iec61850.scheduling.IEC61850ScheduleController.InvalidScheduleException;

import com.sun.tools.javac.util.Pair;

public class TestDut {
    BiConsumer<Float, Integer> monitor = ignoreNewValues();
    public IEC61850ScheduleController scheduler = reset();

    private void newValueCallBack(Float value, Integer prio) {
        monitor.accept(value, prio);
    }

    public void writeAndEnableSchedule(Schedule schedule) throws InvalidScheduleException {
        scheduler.addNewControlAction(schedule);
    }

    public void disableSchedule(String scheduleName) {
        scheduler.disableSchedule(scheduleName);
    }

    public List<Pair<Instant, Float>> watchValueChanges(Instant after, Instant until) throws InterruptedException {
        List<Pair<Instant, Float>> changes = new LinkedList<>();

        // whatever changes before "after" will be written into valueAtStart
        AtomicReference<Float> valueAtStart = new AtomicReference<>();
        valueAtStart.set(0f);
        monitor = (value, prio) -> {
            valueAtStart.set(value);
        };

        long millisUntilStart = Duration.between(Instant.now(), after).toMillis();
        if (millisUntilStart < 0) {
            millisUntilStart = 0;
        }
        Thread.sleep(millisUntilStart);
        // after "after", when value changes should be watched, all changes are to be written into the result map
        monitor = (value, prio) -> {
            if (!value.equals(changes.get(changes.size() - 1).snd)) {
                changes.add(Pair.of(Instant.now(), value));
            }
            // else, ignore (since we do not have a value change)
        };
        changes.add(Pair.of(null, valueAtStart.get()));

        // wait unit "util"
        Thread.sleep(Duration.between(Instant.now(), until).toMillis());
        // then ignore all following
        monitor = ignoreNewValues();

        return changes;
    }

    public <X> List<X> monitor(Instant start, Duration monitoringInterval, int numberOfValuesToObserve)
            throws InterruptedException {
        AtomicReference<Float> lastObservedValue = new AtomicReference<>(Float.valueOf(0));

        // this is to asynchronously update the last value written to the target
        monitor = (value, prio) -> {
            lastObservedValue.set(value);
        };

        // monitoring is done with fixed interval in the tests in alliander. we stick to this for now:
        List<Float> monitoredValues = new LinkedList<>();
        Instant now = Instant.now();
        long delay = Duration.between(now, start).toMillis();
        if (delay < 0) {
            throw new RuntimeException(
                    "Monitoring should have started already. Test setup too slow or wrong parameters: Now is " + now
                            + ", expected to start @" + start);
        }
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (monitoredValues.size() < numberOfValuesToObserve) {
                    monitoredValues.add(lastObservedValue.get());
                }
            }
        }, delay, monitoringInterval.toMillis());

        Duration waitDuration = Duration.between(Instant.now(),
                start.plus(monitoringInterval.multipliedBy(numberOfValuesToObserve)));
        Thread.sleep(waitDuration.toMillis()); // we need to sleep here, otherwise a empty list is returend!

        try {
            return (List<X>) monitoredValues;
        } catch (Exception e) {
            throw new RuntimeException("Not implemented yet: add support for booleans");
        }
    }

    public float readFloatFromSysResSchedule() {
        return 0;
    }

    public Boolean readBooleanFromSysResSchedule() {
        throw new RuntimeException("Not implemented yet: add support for booleans");
    }

    public IEC61850ScheduleController reset() {
        if (this.scheduler != null) {
            this.scheduler.shutdown();
        }
        this.scheduler = new IEC61850ScheduleController((value, prio) -> newValueCallBack(value, prio));
        return this.scheduler;
    }

    private BiConsumer<Float, Integer> ignoreNewValues() {
        return (value, prio) -> {
            // simply ignore :)
        };
    }
}
