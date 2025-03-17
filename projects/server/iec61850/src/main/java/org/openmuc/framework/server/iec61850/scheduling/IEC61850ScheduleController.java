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

import static java.lang.Math.ceil;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * ScheduleController which implements an IEC61850ScheduleController. Holds all enabled schedules and executes one
 * according to the priority and starting time. New control actions can be added and may change the active schedule. May
 * also be disabled whenever the corresponding DsaReq value is set to false
 */
public class IEC61850ScheduleController {
    private static final ObjectMapper mapper;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IEC61850ScheduleController.class);

    static {
        mapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        mapper.registerModule(simpleModule);
    }

    private final Timer timer;
    private final BiConsumer<Float, Integer> scheduleOutput;
    private double outputValue;

    public static final String RESERVE_SCHEDULE_NAME = "reserve-schedule";
    private static final ScheduleEntityImpl DEFAULT_SCHEDULE = new ScheduleEntityImpl(0, RESERVE_SCHEDULE_NAME, 0,
            Instant.now(), false, true, Instant.MAX);
    private String activeSchedule = RESERVE_SCHEDULE_NAME;
    private int activePrio = 0;
    private Instant allowedStartingTimeOfReserveSchedule = Instant.now();

    private final Collection<RunningSchedule> potentialScheduleEntities = new HashSet<>();
    private final Collection<Schedule> potentialSchedules = new HashSet<>();

    public IEC61850ScheduleController(BiConsumer<Float, Integer> target, RunningSchedule reserveSchedule) {
        this.timer = new Timer();
        this.scheduleOutput = target;
        startExecutingReserveSchedule(reserveSchedule);
    }

    /**
     * Initiates the {@link IEC61850ScheduleController} and starts executing the reserve schedule.
     */
    public IEC61850ScheduleController(BiConsumer<Float, Integer> target) {
        this(target, DEFAULT_SCHEDULE);
        log.info("No reserve schedule found in server configuration. Using default {}", DEFAULT_SCHEDULE);
    }

    /**
     * runs a reserve schedule with prio and value ==0 runs every second
     */
    synchronized private void startExecutingReserveSchedule(RunningSchedule singleValueReserveSchedule) {
        long reserveScheduleInterval = ofMillis(1000).toMillis();
        Instant reserveScheduleStartAtNextFullSecond = Instant.now().truncatedTo(SECONDS).plus(ofSeconds(1));

        long millisUntilNextFullSecond = Instant.now().until(reserveScheduleStartAtNextFullSecond, MILLIS);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runScheduleController(singleValueReserveSchedule);
            }
        }, millisUntilNextFullSecond, reserveScheduleInterval);
    }

    /**
     * called by schedule checks if @param entityToExecute should be executed based on priority and start
     */
    synchronized private void runScheduleController(RunningSchedule entityToExecute) {

        log.trace("Active schedule={}", activeSchedule);
        List<Schedule> availableSchedules = potentialSchedules.stream()
                // get all schedules whose starting times are in the past or now, additionally add some nanoseconds to
                // avoid problems caused by task execution order
                .filter(e -> e.getStart().compareTo(Instant.now()) <= 0)
                .collect(Collectors.toList());

        boolean hasPrio = true;
        for (Schedule entityToCompare : availableSchedules) {
            if (!entityToCompare.getScheduleName().equals(entityToExecute.getScheduleName())) {
                if (entityToCompare.getPrio() > entityToExecute.getPrio()) {
                    // if there is a schedule available with a higher priority we break the loop and set the execution
                    // prio to false
                    hasPrio = false;
                    break;
                }
                else if (entityToCompare.getPrio() == entityToExecute.getPrio()) {
                    // if the scheduled entity has the same prio
                    int startComparison = entityToCompare.getStart().compareTo(entityToExecute.getStart());
                    if (startComparison > 0) {
                        // and an earlier start time it its execution priority is set to false and the while loop breaks
                        hasPrio = false;
                        break;
                    }
                    else {
                        if (startComparison == 0) {
                            // start and prio are the same -> the schedule that was enabled later has prio!
                            if (entityToCompare.getEnabledAt().compareTo(entityToExecute.getEnabledAt()) < 0) {
                                hasPrio = false;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (hasPrio) { // if the entity sent to the controller has the right to execute (according to IEC 61850-90-10)
                       // we send it
            log.debug("Schedule {} selected to be executed", entityToExecute.getScheduleName());
            Instant nowTruncatedToFullSeconds = Instant.now().truncatedTo(SECONDS);

            if (!entityToExecute.isReserveSchedule()) {
                Duration interval = potentialSchedules.stream()
                        .filter(e -> e.getScheduleName().equals(entityToExecute.getScheduleName()))
                        .collect(Collectors.toList())
                        .get(0)
                        .getInterval();
                allowedStartingTimeOfReserveSchedule = nowTruncatedToFullSeconds.plus(interval);
            }
            if (entityToExecute.isReserveSchedule()
                    && nowTruncatedToFullSeconds.isBefore(allowedStartingTimeOfReserveSchedule)) {
                // do nothing since the reserve schedule has to wait the interval of the last executed schedule to start
                // writing again
            }
            else {
                outputValue = entityToExecute.getCurrentOutputValue();
                activeSchedule = entityToExecute.getScheduleName();
                activePrio = entityToExecute.getPrio();
                this.scheduleOutput.accept((float) outputValue, activePrio);
            }
        }
        removeFinishedSchedules();
    }

    /**
     * removes schedules which sent their last entity from the potential schedule list
     */
    private void removeFinishedSchedules() {
        Collection<String> removableScheduleNames = potentialScheduleEntities.stream()
                .filter(e -> e.isInLastExecutionInterval())
                .map(RunningSchedule::getScheduleName)
                .collect(Collectors.toSet());

        final int msDelayBeforeRemoving = 10;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (potentialScheduleEntities.stream()
                        .filter(e -> e.isInLastExecutionInterval())
                        .anyMatch(e -> e.getScheduleName().equals(activeSchedule))) {
                    log.debug("Schedule {} in last execution interval", activeSchedule);
                }
                potentialScheduleEntities.removeIf(e -> e.isInLastExecutionInterval());
                for (String name : removableScheduleNames) {
                    potentialSchedules.removeIf(e -> e.getScheduleName().equals(name));
                    log.debug("removed schedule {}", name);
                }
            }
        }, msDelayBeforeRemoving);

    }

    /**
     * runs a schedule according to the control action received from IEC 61850 client every interval and call for
     * execution with its entity if the last interval is reached we set the var lastInterval it, so the controller will
     * remove it from the potential schedule list after execution
     */
    synchronized private void execute(Schedule scheduleData) {
        Instant start = scheduleData.getStart();

        long periodMilllis = scheduleData.getInterval().toMillis();
        log.warn("New schedule '{}' with start @ {}, prio {}, interval of {}ms and {} values",
                scheduleData.getScheduleName(), start, scheduleData.getPrio(), periodMilllis,
                scheduleData.getNumEntr());

        TimerTask task = new TimerTask() {
            int index = 0;
            int intervalCounter = 0;

            @Override
            public void run() {

                int priority = scheduleData.getPrio();
                intervalCounter++;
                int size = scheduleData.getNumEntr();
                String scheduleName = scheduleData.getScheduleName();
                // if the schedule was disabled while running it the Timer.Task removes the schedule from the potential
                // schedule List and cancels itself
                if (potentialSchedules.stream()
                        .filter(e -> e.getScheduleName().equals(scheduleName))
                        .anyMatch(e -> e.getState().equals(ScheduleState.notReady))) {
                    potentialScheduleEntities.removeIf(e -> e.getScheduleName().equals(scheduleName));
                    potentialSchedules.removeIf(e -> e.getScheduleName().equals(scheduleName));
                    cancel();
                    return;
                }
                // if the schedule is not in the potential schedule List, since it was disabled and a new schedule with
                // different parameters was enabled on this node, the task cancels
                if (potentialSchedules.stream()
                        .filter(e -> e.getScheduleName().equals(scheduleName))
                        .noneMatch(e -> e.getStart().equals(start))) {
                    cancel();
                    return;
                }
                // set state in potential schedule List to running on the first time starting the Timer.task
                if (potentialSchedules.stream()
                        .filter(e -> e.getScheduleName().equals(scheduleName))
                        .anyMatch(e -> e.getState().equals(ScheduleState.ready))) {
                    potentialSchedules.stream()
                            .filter(c -> c.getScheduleName().equals(scheduleName))
                            .forEach(ent -> ent.setState(ScheduleState.running));
                }

                if (intervalCounter > size) {
                    cancel();
                    return;
                }
                if (potentialSchedules.stream()
                        .filter(e -> e.getScheduleName().equals(scheduleName))
                        .anyMatch(e -> e.getState().equals(ScheduleState.running))) {
                    RunningSchedule scheduleEntity;
                    // create scheduleEntity with the current value and send to controlling function
                    if (intervalCounter == size) {
                        // if we have reached the last interval of the schedule set the last interval var in the
                        // potential schedule list and cancels the timer task
                        potentialScheduleEntities.stream()
                                .filter(c -> c.getScheduleName().equals(scheduleName))
                                .forEach(ent -> ent.setLastInterval());
                        scheduleEntity = new ScheduleEntityImpl(scheduleData.getValues().get(index).floatValue(),
                                scheduleName, priority, start, true, false, scheduleData.getEnabledAt());
                    }
                    else {
                        scheduleEntity = new ScheduleEntityImpl(scheduleData.getValues().get(index).floatValue(),
                                scheduleName, priority, start, false, false, scheduleData.getEnabledAt());
                    }
                    runScheduleController(scheduleEntity);
                }
                index++;
            }
        };
        this.timer.scheduleAtFixedRate(task, Date.from(start), periodMilllis);
    }

    /**
     * adds the new @param action to potential schedules calls runSchedule to generate a timer task for that schedule
     */
    public synchronized void addNewControlAction(Schedule schedule) throws InvalidScheduleException {
        validate(schedule);
        Schedule action = filterPassedValuesAdjustStart(schedule);

        if (potentialSchedules.stream()
                .anyMatch(e -> e.getScheduleName().equals(action.getScheduleName())
                        && e.getStart().equals(action.getStart()))) {
            // re-enabling schedule if it has not started but was disabled before
            potentialSchedules.stream()
                    .filter(e -> e.getScheduleName().equals(action.getScheduleName()))
                    .forEach(e -> e.setState(ScheduleState.ready));
        }
        else if (potentialSchedules.stream()
                .anyMatch(e -> e.getScheduleName().equals(action.getScheduleName())
                        && !e.getStart().equals(action.getStart()))) {
            potentialSchedules.removeIf(e -> e.getScheduleName().equals(action.getScheduleName()));
            potentialSchedules.add(action);
            RunningSchedule scheduleEntity = new ScheduleEntityImpl(action.getValues().get(0).floatValue(),
                    action.getScheduleName(), action.getPrio(), action.getStart(), false, false, action.getEnabledAt());
            potentialScheduleEntities.add(scheduleEntity);
            execute(action);
        }
        else if (potentialSchedules.stream().noneMatch(e -> e.getScheduleName().equals(action.getScheduleName()))) {
            potentialSchedules.add(action);
            RunningSchedule scheduleEntity = new ScheduleEntityImpl(action.getValues().get(0).floatValue(),
                    action.getScheduleName(), action.getPrio(), action.getStart(), false, false, action.getEnabledAt());
            potentialScheduleEntities.add(scheduleEntity);
            execute(action);
        }
    }

    /**
     * Adjusts schedules that started in the past in such a way that only the values that still need to be executed
     * remain and start is set to now or shortly after now
     */
    private static Schedule filterPassedValuesAdjustStart(Schedule schedule) {
        // check schedule has start time before now
        if (schedule.getStart().compareTo(Instant.now()) < 0) {
            // if the schedule would have ended before now, the validate method would have thrown, so we do not need to
            // check this here.
            log.debug(
                    "Schedule starts in the past but is still going on. Manipulating start and values to be processed");
            long diff = Instant.now().minus(schedule.getStart().toEpochMilli(), MILLIS).toEpochMilli();
            int intervalsPassed = (int) ceil(diff / schedule.getInterval().toMillis());
            Instant newStart = schedule.getStart()
                    .plus(1 + intervalsPassed * schedule.getInterval().toMillis(), MILLIS);

            Schedule manipulatedSchedule = new Schedule() {
                @Override
                public List<? extends Number> getValues() {
                    return schedule.getValues().stream().skip(intervalsPassed).collect(Collectors.toList());
                }

                @Override
                public Duration getInterval() {
                    return schedule.getInterval();
                }

                @Override
                public String getScheduleName() {
                    return schedule.getScheduleName();
                }

                @Override
                public Instant getStart() {
                    return newStart;
                }

                @Override
                public Instant getEnabledAt() {
                    return schedule.getEnabledAt();
                }

                @Override
                public int getPrio() {
                    return schedule.getPrio();
                }

                @Override
                public ScheduleState getState() {
                    return schedule.getState();
                }

                @Override
                public void setState(ScheduleState scheduleState) {
                    schedule.setState(scheduleState);
                }

                @Override
                public int getNumEntr() {
                    return schedule.getNumEntr() - intervalsPassed;
                }
            };
            log.debug("Only {} of {} values remaining", schedule.getValues().size(),
                    manipulatedSchedule.getValues().size());
            log.debug("Shifted start from {} to {}", schedule.getStart(), manipulatedSchedule.getStart());
            return manipulatedSchedule;
        }
        else {
            log.trace("Schedule does not start in the past. Leaving unchanged.");
            return schedule;
        }
    }

    /**
     * This method makes sure all requirements are met such that the schedule can be processed by the
     * {@link IEC61850ScheduleController}.
     *
     * @param schedule
     * @throws InvalidScheduleException
     *             if any of the requirements for processing is violated
     */
    public static void validate(Schedule schedule) throws InvalidScheduleException {
        if (schedule.getValues().isEmpty()) {
            throw new InvalidScheduleException("too few control values");
        }
        if (!schedule.getStart().truncatedTo(SECONDS).equals(schedule.getStart())) {
            throw new InvalidScheduleException("resolution of action start too high");
        }
        long intervalCheck = schedule.getInterval().toMillis() % 1000;
        if (intervalCheck != 0) {
            throw new InvalidScheduleException("resolution of interval should be a multiple of SECOND");
        }
        if (schedule.getInterval().getSeconds() < 1) {
            throw new InvalidScheduleException("interval should be at least 1 second");
        }
        Instant scheduleEnd = schedule.getStart()
                .plus(schedule.getInterval().multipliedBy(schedule.getValues().size()));
        if (scheduleEnd.compareTo(Instant.now()) < 0) {
            throw new InvalidScheduleException("Schedule is late: would have started at " + schedule.getStart()
                    + " and ended before now, at " + scheduleEnd + ".");
        }
    }

    public void disableSchedule(String scheduleName) {
        potentialSchedules.stream()
                .filter(e -> e.getScheduleName().equals(scheduleName))
                .forEach(c -> c.setState(ScheduleState.notReady));
    }

    public void shutdown() {
        this.timer.cancel();
        this.timer.purge();
    }

    public static class InvalidScheduleException extends Exception {
        public InvalidScheduleException(String message) {
            super(message);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IEC61850ScheduleController that = (IEC61850ScheduleController) o;
        return Double.compare(outputValue, that.outputValue) == 0 && activePrio == that.activePrio
                && Objects.equals(timer, that.timer) && Objects.equals(scheduleOutput, that.scheduleOutput)
                && Objects.equals(activeSchedule, that.activeSchedule)
                && Objects.equals(allowedStartingTimeOfReserveSchedule, that.allowedStartingTimeOfReserveSchedule)
                && Objects.equals(potentialScheduleEntities, that.potentialScheduleEntities)
                && Objects.equals(potentialSchedules, that.potentialSchedules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timer, scheduleOutput, outputValue, activeSchedule, activePrio,
                allowedStartingTimeOfReserveSchedule, potentialScheduleEntities, potentialSchedules);
    }
}
