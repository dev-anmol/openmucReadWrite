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

import static java.time.Duration.between;
import static java.time.Duration.of;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.server.iec61850.scheduling.IEC61850ScheduleController.InvalidScheduleException;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.IEC61850ScheduleNodeSnapshot;
import org.openmuc.framework.server.iec61850.serverSchedulingAdapter.ServerModelAccess;
import org.slf4j.Logger;

import com.beanit.iec61850bean.Fc;
import com.sun.tools.javac.util.Pair;

/**
 * Holds tests related to 61850 schedule execution
 */
public class SchedulingDataIEC61850ExecutionTest {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SchedulingDataIEC61850ExecutionTest.class);
    float RESERVE_SCHEDULE_VALUE = 0;

    ScheduleFactory scheduleConstants = new ScheduleFactory();

    private static boolean areClose(Float actual, Float expected, double withPercentage) {
        double lowerBound = expected * (1.0 - withPercentage / 100f);
        double upperBound = expected * (1.0 + withPercentage / 100f);
        return actual >= lowerBound && actual <= upperBound;
    }

    @Test
    public void firstScheduleValueIsWrittenAtScheduleStart() throws InvalidScheduleException, InterruptedException {
        TestDut dut = new TestDut();
        final Instant scheduleStart = now().truncatedTo(SECONDS).plusSeconds(3);

        int scheduleValue = 10;
        dut.writeAndEnableSchedule(
                scheduleConstants.prepareSchedule(Arrays.asList(scheduleValue), 1, ofSeconds(1), scheduleStart, 255));
        log.info("Expecting schedule to change value of reserve schedule value to schedule value {} shortly after {}",
                scheduleValue, scheduleStart);

        List<Pair<Instant, Float>> valueChanges = dut.watchValueChanges(now(), scheduleStart.plus(ofSeconds(4)));

        log.info("Got " + valueChanges);
        Assertions.assertEquals(3, valueChanges.size());

        // index0: value at beginning, index1: value at start of schedule
        Pair<Instant, Float> scheduleValueChange = valueChanges.get(1);

        // check value is as expected
        assertThat(scheduleValueChange.snd).isEqualTo(scheduleValue);

        // check timing
        Instant scheduleValueChangeTime = scheduleValueChange.fst;
        long millisBetweenScheduleStartUntilValueChange = between(scheduleStart, scheduleValueChangeTime).toMillis();
        log.debug("Got change {}ms after schedule start", millisBetweenScheduleStartUntilValueChange);
        assertThat(millisBetweenScheduleStartUntilValueChange).isNotNegative(); // positive or zero is allowed
        assertThat(millisBetweenScheduleStartUntilValueChange).isLessThan(500);
        dut.scheduler.shutdown();
    }

    @Test
    public void reserveScheduleIsRunningAfterSchedule() throws InterruptedException, InvalidScheduleException {
        TestDut dut = new TestDut();
        final Instant scheduleStart = now().truncatedTo(SECONDS).plusSeconds(2);

        Duration scheduleInterval = ofMillis(1000);
        float scheduleValue1 = 1;
        float scheduleValue2 = 42;
        float scheduleValue3 = 1337;
        List<Float> scheduleValues = Arrays.asList(scheduleValue1, scheduleValue2, scheduleValue3);
        dut.writeAndEnableSchedule(
                scheduleConstants.prepareSchedule(scheduleValues, 1, scheduleInterval, scheduleStart, 255));

        List<Pair<Instant, Float>> valueChanges = dut.watchValueChanges(now(),
                scheduleStart.plus(scheduleInterval.multipliedBy(scheduleValues.size() + 1)));

        log.info("Observed value changes: {}", valueChanges);
        assertThat(valueChanges).hasSize(5);
        // we expect 5 value changes
        // index0: value at beginning: schedule has not started yet -> reserve schedule
        assertThat(valueChanges.get(0).snd).isEqualTo(RESERVE_SCHEDULE_VALUE);
        // index1: value at start of schedule
        assertThat(valueChanges.get(1).snd).isEqualTo(scheduleValue1);
        // index2: 2n value at end of schedule
        assertThat(valueChanges.get(2).snd).isEqualTo(scheduleValue2);
        // index3: 3rd value of schedule
        assertThat(valueChanges.get(3).snd).isEqualTo(scheduleValue3);
        // index4: reserve schedule value of schedule
        assertThat(valueChanges.get(4).snd).isEqualTo(RESERVE_SCHEDULE_VALUE);

        // assert timing matches: reserve schedule should be enabled after
        Instant switchBackToReserveScheduleTime = valueChanges.get(4).fst;
        Duration delay = between(scheduleStart.plus(scheduleInterval.multipliedBy(3)), switchBackToReserveScheduleTime);
        assertThat(delay).isPositive();
        assertThat(delay).isLessThan(Duration.ofMillis(500));
        dut.scheduler.shutdown();
    }

    @Test
    public void test_prioritiesPowerSchedules() throws InterruptedException, InvalidScheduleException {
        TestDut dut = new TestDut();

        // do not change the interval, this is demanded by a requirement!
        final Duration interval = ofSeconds(1);

        final Instant testStart = now().truncatedTo(SECONDS).plusSeconds(2);
        final Instant schedule1Start = testStart.plus(interval);

        // schedule 1:
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(10, 30, 70, 100, 100, 100), 1,
                interval, schedule1Start, 25));

        // schedule 2:
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(11, 31, 71, 99, 99, 99), 2, interval,
                schedule1Start.plus(interval.multipliedBy(4)), 40));

        // schedule 3:
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(12, 32, 72, 98, 98), 3, interval,
                schedule1Start.plus(interval.multipliedBy(8)), 60));

        // schedule 4, ends after 44s:
        dut.writeAndEnableSchedule(
                scheduleConstants.prepareSchedule(Arrays.asList(13, 33, 73, 97, 97, 97, 97, 97, 97, 97), 4, interval,
                        schedule1Start.plus(interval.multipliedBy(12)), 70));

        // schedule 5, ends after 42s
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(70, 70, 70, 70, 70), 5, interval,
                schedule1Start.plus(interval.multipliedBy(16)), 100));

        // schedule 6,
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(90), 6, interval,
                schedule1Start.plus(interval.multipliedBy(17)), 120));

        // schedule 7,

        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(90), 7, interval,
                schedule1Start.plus(interval.multipliedBy(19)), 120));

        // schedule 8:
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(10), 8, interval,
                schedule1Start.plus(interval.multipliedBy(21)), 80));

        // schedule 9
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(80), 9, interval,
                schedule1Start.plus(interval.multipliedBy(22)), 20));

        // schedule 10
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(100), 10, interval,
                schedule1Start.plus(interval.multipliedBy(23)), 11));

        float sysResValue = dut.readFloatFromSysResSchedule();

        List<Float> expectedValues = Arrays.asList(sysResValue, 10f, 30f, 70f, 100f, 11f, 31f, 71f, 99f, 12f, 32f, 72f,
                98f, 13f, 33f, 73f, 97f, 70f, 90f, 70f, 90f, 70f, 10f, 80f, 100f, sysResValue);

        Instant monitoringStart = schedule1Start.minus(interval.dividedBy(2));
        log.info("Will start monitoring @ {}, schedule expected to start @ {}", monitoringStart, schedule1Start);
        List<Float> actualValues = dut.monitor(monitoringStart, interval, 26);

        log.info("expected values {}", expectedValues);
        log.info("observed values {}", actualValues);

        assertValuesMatch(expectedValues, actualValues, 0.01f);
        dut.scheduler.shutdown();
    }

    @Test
    public void test_DisablingSchedule() throws InterruptedException, InvalidScheduleException {
        TestDut dut = new TestDut();

        // do not change the interval, this is demanded by a requirement!
        final Duration interval = ofSeconds(1);

        final Instant testExecutionStart = now();
        final Instant schedulesStart = testExecutionStart.truncatedTo(SECONDS).plusSeconds(interval.getSeconds() + 1);

        Schedule schedule = scheduleConstants.prepareSchedule(Arrays.asList(1, 2, 3, 4, 5), 1, interval, schedulesStart,
                25);
        // schedule 1:
        dut.writeAndEnableSchedule(schedule);

        log.debug("Test setup took {}", Duration.between(testExecutionStart, now()));

        float sysResValue = dut.readFloatFromSysResSchedule();

        List<Float> expectedValues = Arrays.asList(sysResValue, 1f, 2f, sysResValue, sysResValue);

        List<Float> actualValues = dut.monitor(schedulesStart.minus(interval).plusMillis(200), interval, 3);

        dut.disableSchedule(schedule.getScheduleName());
        List<Float> actualValues2 = dut.monitor(schedulesStart.plus(of(10, SECONDS)).plusMillis(200), interval, 2);
        log.info("observed values {}", actualValues);
        log.info("observed values 2 {}", actualValues2);
        actualValues.addAll(actualValues2);
        log.info("expected values {}", expectedValues);
        log.info("observed values {}", actualValues);
        assertValuesMatch(expectedValues, actualValues, 0.01f);
        dut.scheduler.shutdown();
    }

    private ServerModelAccess mockServer(Instant start) {
        return new ServerModelAccess() {
            @Override
            public String readModelNode(String objectReference, Fc fc) {
                if (objectReference.contains("SchdPrio")) {
                    return "42";
                }
                if (objectReference.contains("SchdIntv")) {
                    return "1337";
                }
                if (objectReference.contains("StrTm")) {
                    return Long.toString(start.getEpochSecond());
                }
                if (objectReference.contains("EnaReq")) {
                    return "true";
                }
                if (objectReference.contains("NumEntr")) {
                    return "1";
                }
                if (objectReference.contains("Val")) {
                    return "123.456";
                }
                throw new RuntimeException("Mock not set up properly");
            }

            @Override
            public boolean checkNodeExists(String serverSchedule) {
                // fine for now, the config checks are in
                // org.openmuc.framework.server.iec61850.server.IEC61850ScheduleNodeSnapshotTest and should cover
                // everything there
                return true;
            }
        };
    }

    @Test
    public void test_schedulesThatStartAndEndInThePastAreConsideredInvalid()
            throws IEC61850ScheduleNodeSnapshot.IEC61850ServerConfigException {
        ServerModelAccess access = mockServer(Instant.ofEpochSecond(42));
        IEC61850ScheduleNodeSnapshot scheduleInput = new IEC61850ScheduleNodeSnapshot("utini", "java", access);

        Assertions.assertEquals("utini", scheduleInput.getScheduleName());
        Assertions.assertEquals("java", scheduleInput.getServerName());
        Assertions.assertEquals(Instant.ofEpochSecond(42), scheduleInput.getStart());
        Assertions.assertEquals(Duration.ofSeconds(1337), scheduleInput.getInterval());
        long secondsSinceEnable = between(scheduleInput.getEnabledAt(), now()).getSeconds();
        Assertions.assertTrue(secondsSinceEnable < 5); // could test with less but who cares...
        Assertions.assertEquals(1, scheduleInput.getNumEntr());
        Assertions.assertEquals(42, scheduleInput.getPrio());
        Assertions.assertIterableEquals(Arrays.asList(123.456f), scheduleInput.getValues());

        // start time is before now and ends vefore now --> schedule considered invalid for now
        Assertions.assertThrows(InvalidScheduleException.class,
                () -> IEC61850ScheduleController.validate(scheduleInput));

        // start time is after now -> schedule is considered valid!
        Assertions.assertDoesNotThrow(() -> IEC61850ScheduleController
                .validate(new IEC61850ScheduleNodeSnapshot("asd", "asd", mockServer(Instant.now().plusSeconds(5)))));
    }

    @Test
    public void test_DisablingScheduleBeforeStarting() throws InterruptedException, InvalidScheduleException {
        TestDut dut = new TestDut();

        // do not change the interval, this is demanded by a requirement!
        final Duration interval = ofSeconds(1);

        final Instant testExecutionStart = now();
        final Instant schedulesStart = testExecutionStart.truncatedTo(SECONDS).plusSeconds(2);

        Schedule schedule = scheduleConstants.prepareSchedule(Arrays.asList(1, 2, 3, 4, 5), 1, interval, schedulesStart,
                25);
        // schedule 1:
        dut.writeAndEnableSchedule(schedule);

        dut.disableSchedule(schedule.getScheduleName());

        log.debug("Test setup took {}", Duration.between(testExecutionStart, now()));

        float sysResValue = dut.readFloatFromSysResSchedule();

        List<Float> expectedValues = Arrays.asList(sysResValue, sysResValue, sysResValue, sysResValue, sysResValue);

        List<Float> actualValues = dut.monitor(schedulesStart.minus(interval).plusMillis(200), interval, 5);
        log.info("expected values {}", expectedValues);
        log.info("observed values {}", actualValues);

        assertValuesMatch(expectedValues, actualValues, 0.01f);
        dut.scheduler.shutdown();
    }

    @Test
    public void test_DisablingScheduleAndEnablingNewSchedule() throws InterruptedException, InvalidScheduleException {
        TestDut dut = new TestDut();

        // do not change the interval, this is demanded by a requirement!
        final Duration interval = ofSeconds(2);

        final Instant testExecutionStart = now();
        final Instant schedulesStart = testExecutionStart.truncatedTo(SECONDS).plusSeconds(2);

        Schedule schedule = scheduleConstants.prepareSchedule(Arrays.asList(1, 2, 3, 4, 5), 1, interval, schedulesStart,
                25);
        // schedule 1:
        dut.writeAndEnableSchedule(schedule);

        log.debug("Test setup took {}", Duration.between(testExecutionStart, now()));

        float sysResValue = dut.readFloatFromSysResSchedule();

        List<Float> expectedValues = Arrays.asList(sysResValue, 1f, 2f, sysResValue, 11f, 12f, 13f);

        List<Float> actualValues = dut.monitor(schedulesStart.minus(ofSeconds(1)).plusMillis(200), interval, 3);

        dut.disableSchedule(schedule.getScheduleName());

        schedule = scheduleConstants.prepareSchedule(Arrays.asList(11, 12, 13, 14, 15), 1, interval,
                now().truncatedTo(SECONDS).plusSeconds(2), 25);

        log.info("start time of new schedule {}", now().truncatedTo(SECONDS).plusSeconds(2));
        // schedule 1:
        dut.writeAndEnableSchedule(schedule);

        List<Float> actualValues2 = dut.monitor(now().truncatedTo(SECONDS).plusMillis(1000).plusMillis(200), interval,
                4);
        log.info("observed values {}", actualValues);
        log.info("observed values 2 {}", actualValues2);
        actualValues.addAll(actualValues2);
        log.info("expected values {}", expectedValues);
        log.info("observed values {}", actualValues);

        assertValuesMatch(expectedValues, actualValues, 0.01f);
        dut.scheduler.shutdown();
    }

    @Test
    public void test_disablingOfSchedules() throws InterruptedException, InvalidScheduleException {
        TestDut dut = new TestDut();

        // do not change the interval, this is demanded by a requirement!
        final Duration interval = ofSeconds(1);

        final Instant testExecutionStart = now();
        final Instant schedulesStart = testExecutionStart.truncatedTo(SECONDS).plusSeconds(2);

        Schedule schedule = scheduleConstants.prepareSchedule(Arrays.asList(1, 2, 3, 4, 5), 1, interval, schedulesStart,
                25);
        // schedule 1:
        dut.writeAndEnableSchedule(schedule);

        Thread.sleep(1000);

        dut.disableSchedule(schedule.getScheduleName());

        float sysResValue = dut.readFloatFromSysResSchedule();

        // the schedule has been disabled and should not be executed!
        List<Float> expectedValues = Arrays.asList(sysResValue, sysResValue, sysResValue);

        List<Float> actualValues = dut.monitor(Instant.now().plusMillis(200), interval, 3);

        log.info("expected values {}", expectedValues);
        log.info("observed values {}", actualValues);

        assertValuesMatch(expectedValues, actualValues, 0.01f);
        dut.scheduler.shutdown();
    }

    /**
     * concerning IEC61850 a schedule with the same prio but later start time rules out the one with this prio but
     * earlier start time, test for float schedules
     * <p>
     * see IEC61850-90-10 ed 2017 Schedule Controller Definitions, section 5.5.3
     */
    @Test
    public void testSamePriosDifferentStartPowerSchedules() throws InterruptedException, InvalidScheduleException {
        TestDut dut = new TestDut();
        final Instant testExecutionStart = now();
        Duration interval = ofSeconds(1);
        final Instant schedulesStart = testExecutionStart.truncatedTo(SECONDS).plusSeconds(2);

        // schedule 1, start after 2s, duration 12s, Prio 40
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(10, 10, 10, 10, 10, 10), 1, interval,
                schedulesStart, 40));
        // schedule 2, start after 4s, duration 4s, Prio 40
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(70, 100), 2, interval,
                schedulesStart.plus(interval), 40));
        // schedule 3, start after 8s, duration 4s, Prio 40
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(70, 70), 3, interval,
                schedulesStart.plus(interval.multipliedBy(3)), 40));
        // schedule 4, start after 14s, duration 2s, Prio 60
        dut.writeAndEnableSchedule(scheduleConstants.prepareSchedule(Arrays.asList(100), 4, interval,
                schedulesStart.plus(interval.multipliedBy(6)), 60));

        float sysResValue = dut.readFloatFromSysResSchedule();
        List<Float> expectedValues = Arrays.asList(sysResValue, 10f, 70f, 100f, 70f, 70f, 10f, 100f, sysResValue);

        List<Float> actualValues = dut.monitor(schedulesStart.minus(interval).plusMillis(200), interval,
                expectedValues.size());
        log.info("expected values {}", expectedValues);
        log.info("observed values {}", actualValues);
        assertValuesMatch(expectedValues, actualValues, 0.01);
        dut.scheduler.shutdown();
    }

    /**
     * two schedules with the same prio and start: the one with the lower Number in its name rules out the other one,
     * e.g. OnOffPow_FSCH04 rules put OnOffPow_FSCH10 IEC61850 does not determine a certain behavior in this case, this
     * is just a detail that was fixed for implementation
     */
    @Test
    public void test_samePrioAndStartFloatSchedule() throws InterruptedException, InvalidScheduleException {
        TestDut dut = new TestDut();
        float sysResValue = dut.readFloatFromSysResSchedule();

        Instant start = Instant.now().truncatedTo(SECONDS);
        Schedule schedule1 = scheduleConstants.prepareSchedule(Arrays.asList(70f, 70f), 1, ofSeconds(3),
                start.plus(ofSeconds(3)), 40);
        Schedule schedule2 = scheduleConstants.prepareSchedule(Arrays.asList(100f, 100f, 100f), 2, ofSeconds(3),
                start.plus(ofSeconds(3)), 40);

        dut.writeAndEnableSchedule(schedule1);
        dut.writeAndEnableSchedule(schedule2);

        List<Float> expectedValues = Arrays.asList(sysResValue, 70f, 70f, 100f, sysResValue);
        List<Float> actualValues = dut.monitor(start.plus(ofMillis(1000)), ofSeconds(3), expectedValues.size());
        log.info("expected values {}", expectedValues);
        log.info("observed values {}", actualValues);
        assertValuesMatch(expectedValues, actualValues, 0.01);
        dut.scheduler.shutdown();
    }

    @Test
    void reserveScheduleIsActivatedExactlyOneIntervalAfterLastValueOfActtiveSchedule()
            throws InvalidScheduleException, InterruptedException {
        TestDut dut = new TestDut();

        float sysResValue = dut.readFloatFromSysResSchedule();

        Instant start = Instant.now().truncatedTo(SECONDS);
        Instant scheduleStart = start.plus(ofSeconds(2));
        Instant monitoringStart = scheduleStart.minus(ofMillis(500));
        log.trace("Setting schedule start to {}, monitoring will start {}", scheduleStart, monitoringStart);
        Duration scheduleInterval = ofSeconds(3);
        Schedule schedule = scheduleConstants.prepareSchedule(Arrays.asList(70f), 1, scheduleInterval, scheduleStart,
                40);

        dut.writeAndEnableSchedule(schedule);

        List<Float> expectedValues = Arrays.asList(sysResValue, 70f, 70f, 70f, sysResValue);
        List<Float> actualValues = dut.monitor(monitoringStart, ofSeconds(1), 5);
        log.info("expected values {}", expectedValues);
        log.info("observed values {}", actualValues);
        assertValuesMatch(expectedValues, actualValues, 0.01);
        dut.scheduler.shutdown();
    }

    @Test
    void schedulesThatStartAndEndBeforeNowWillNotBeExecuted() {
        TestDut dut = new TestDut();
        // ... but instead they are considered invalid
        List<Float> scheduleValues = Arrays.asList(11f, 12f, 13f, 14f);
        Duration scheduleInterval = Duration.ofSeconds(1);
        Instant scheduleStartToHaveEndedJustBeforeNow = now().truncatedTo(SECONDS)
                .minus(scheduleInterval.multipliedBy(scheduleValues.size() + 1));
        Schedule scheduleEndsBeforeNow = scheduleConstants.prepareSchedule(scheduleValues, 1, scheduleInterval,
                scheduleStartToHaveEndedJustBeforeNow, 255);
        InvalidScheduleException ex = Assertions.assertThrows(InvalidScheduleException.class,
                () -> dut.writeAndEnableSchedule(scheduleEndsBeforeNow));
        assertThat(ex).hasMessageContaining("Schedule is late");
        dut.scheduler.shutdown();
    }

    @Test
    void schedulesThatStartBeforeNowButEndAfterNowArePartiallyExecuted()
            throws InvalidScheduleException, InterruptedException {
        TestDut dut = new TestDut();
        float sysResValue = dut.readFloatFromSysResSchedule();

        Duration scheduleInterval = Duration.ofSeconds(1);

        Instant now = Instant.now().truncatedTo(SECONDS);

        List<Float> scheduleValues = Arrays.asList(11f, 12f, 13f, 14f);
        Instant scheduleStartInPast = now.minus(scheduleInterval.multipliedBy(2)); // values 11 & 12 are not seen
                                                                                   // anymore
        Instant monitoringStart = now.plus(scheduleInterval.multipliedBy(3).dividedBy(2)); // value 13 is skipped

        dut.writeAndEnableSchedule(
                scheduleConstants.prepareSchedule(scheduleValues, 1, scheduleInterval, scheduleStartInPast, 40));

        log.debug("Schedule   starting @{}: BEFORE now and BEFORE monitoring start", scheduleStartInPast);
        log.debug("Monitoring starting @" + monitoringStart);
        List<Float> expectedValues = Arrays.asList(14f, sysResValue);
        // TODO: migrate to dut.watchValueChanges(..)?
        List<Float> actualValues = dut.monitor(monitoringStart, scheduleInterval, expectedValues.size());
        log.info("expected values {}", expectedValues);
        log.info("observed values {}", actualValues);
        assertValuesMatch(expectedValues, actualValues, 0.01);
        dut.scheduler.shutdown();
    }

    private void assertValuesMatch(List<Boolean> expectedValues, List<Boolean> actualValues) {
        throw new RuntimeException("Boolean schedules are not supported yet");
    }

    private void assertValuesMatch(List<Float> expectedValues, List<Float> actualValues, double withPercentage) {
        Assertions.assertEquals(actualValues.size(), expectedValues.size(),
                "Expected values size does not match actual values size");
        for (int i = 0; i < expectedValues.size(); i++) {
            float expected = expectedValues.get(i);
            float actual = actualValues.get(i);
            Assertions.assertTrue(areClose(actual, expected, withPercentage));
        }
    }

    static class ScheduleFactory {

        public Schedule prepareSchedule(List<?> values, int scheduleNumber, Duration interval, Instant scheduleStart,
                int schedulePiority) {
            return new Schedule() {
                ScheduleState state = ScheduleState.ready;
                Instant enabledAt = Instant.now();

                @Override
                public List<? extends Number> getValues() {
                    return values.stream().map(o -> {
                        try {
                            return (Number) o;
                        } catch (Exception e) {
                            throw new RuntimeException(
                                    "Only numbers supported for now, " + o.getClass() + " is not supported.");
                        }
                    }).collect(Collectors.toList());
                }

                @Override
                public Duration getInterval() {
                    return interval;
                }

                @Override
                public String getScheduleName() {
                    return "schedule" + scheduleNumber;
                }

                @Override
                public Instant getStart() {
                    return scheduleStart;
                }

                @Override
                public Instant getEnabledAt() {
                    return enabledAt;
                }

                @Override
                public int getPrio() {
                    return schedulePiority;
                }

                @Override
                public ScheduleState getState() {
                    return this.state;
                }

                @Override
                public void setState(ScheduleState scheduleState) {
                    this.state = scheduleState;
                }

                @Override
                public int getNumEntr() {
                    return values.size();
                }
            };
        }
    }
    // FIXME: this class (probably all) needs cleanup: reformat, reorder,...
}
