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
package org.openmuc.framework.driver.channelmath;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.soft.javaluator.DoubleEvaluator;

/**
 * This class contains methods to parse the mathematical formula contained in a channel's address, replace channels by
 * their values and finally return the result
 */
class MathEvaluator {

    private final static String CHANNEL_DELIMITER = "§";
    private static final Logger logger = LoggerFactory.getLogger(MathEvaluator.class);

    // Private constructor to prevent instantiation
    private MathEvaluator() {
    }

    /**
     * The main method. Will parse settings and address of the mathChannel, read the latest records from other channels
     * and return a Record containing the result.
     */
    public static RecordWithInputChannelCache evaluate(Channel mathChannel, ChannelFinder channelRefresher,
            Object cache) throws MathChannelParseException, MathChannelReadExcpetion {
        TimestampMergeStrategy tsStrategy = TimestampMergeStrategy.parseDeviceSettings(mathChannel);

        String calculationFormulaWithChannelReferences = mathChannel.getChannelAddress();
        IntermediateResult intermediateResult = parseContainerReplaceChannelsByValues(
                calculationFormulaWithChannelReferences, mathChannel.getId(), channelRefresher, cache);

        return intermediateResult.getResult(tsStrategy, mathChannel.getSamplingInterval());
    }

    @SuppressWarnings("unchecked")
    static IntermediateResult parseContainerReplaceChannelsByValues(
            final String calculationFormulaWithChannelReferences, String channelId, ChannelFinder channelRefresher,
            Object cache) throws MathChannelParseException, MathChannelReadExcpetion {

        LinkedList<Record> usedRecords = new LinkedList<>();

        final Set<Channel> inputChannels;
        final boolean firstOrRefreshRun;
        if (cache == null) {
            // first run --> definitely parse the formula and look for channel references!
            inputChannels = channelRefresher
                    .findChannelsRequiredToEvaluateFormula(calculationFormulaWithChannelReferences, channelId);
            firstOrRefreshRun = true;
        }
        else {
            Set<Channel> mayBeInputChannelsToBeCachedForNextRun;
            try {
                mayBeInputChannelsToBeCachedForNextRun = (Set<Channel>) cache;
                logger.debug("Using cached channels");
            } catch (Exception e) {
                logger.warn(
                        "Got invalid cache: cache={0} with type {1}. This should never happen. Refreshing channels required to evaluate formula.",
                        new Object[] { cache, cache.getClass(), e });
                mayBeInputChannelsToBeCachedForNextRun = channelRefresher
                        .findChannelsRequiredToEvaluateFormula(calculationFormulaWithChannelReferences, channelId);
                logger.info(
                        "Refreshed channels required to evaluate formula. Found {0} required input channels. Will store these channels in cache.");
            }
            inputChannels = mayBeInputChannelsToBeCachedForNextRun;
            firstOrRefreshRun = false;
        }

        String plainFormulaWithConstants = calculationFormulaWithChannelReferences;
        for (Channel channel : inputChannels) {
            Record latestRecord = channel.getLatestRecord();
            if (latestRecord == null) {
                throw new MathChannelReadExcpetion("Unable to read from channel " + channel.getId());
            }
            if (latestRecord.getFlag() != Flag.VALID) {
                throw new MathChannelReadExcpetion("Read bad latest record " + latestRecord
                        + " (flag is not valid) from channel " + channel.getId() + ". Aborting processing.");
            }

            usedRecords.add(latestRecord);
            String valueAsString = String.valueOf(latestRecord.getValue().asDouble());
            plainFormulaWithConstants = plainFormulaWithConstants
                    .replaceAll(CHANNEL_DELIMITER + channel.getId() + CHANNEL_DELIMITER, valueAsString);

        }
        if (plainFormulaWithConstants.contains(CHANNEL_DELIMITER)) {

            if (!firstOrRefreshRun) {
                logger.warn(
                        "Formula of channel with id={0} has channel placeholders left. This should only happen upon changes of channels.xml. Re-parsing formula '{1}'",
                        new Object[] { channelId, calculationFormulaWithChannelReferences });
                // call again with null cache
                return parseContainerReplaceChannelsByValues(calculationFormulaWithChannelReferences, channelId,
                        channelRefresher, null);
            }
            else {
                throw new MathChannelParseException(
                        "Replacing channel references by values failed. Refusing to parse again to avoid infinite recursion (and thus stack overflow).");
            }
        }
        else {
            logger.debug(
                    "Successfully replaced all channel references by their values. Input formula={0}, only values formula={1}, used channels={2}.",
                    new Object[] { calculationFormulaWithChannelReferences, plainFormulaWithConstants, inputChannels });
            return new IntermediateResult(plainFormulaWithConstants, usedRecords, inputChannels);
        }
    }

    public static Set<String> getChannelIdentifiers(String calculationFormulaWithChannelReferences)
            throws MathChannelParseException {

        String[] split = calculationFormulaWithChannelReferences.split(CHANNEL_DELIMITER);

        if (count(CHANNEL_DELIMITER, calculationFormulaWithChannelReferences) % 2 == 1) {
            throw new MathChannelParseException("Missing end delimiter '" + CHANNEL_DELIMITER
                    + "' for channel definition found in '" + calculationFormulaWithChannelReferences + "'.");
        }

        Iterator<String> iterator = Arrays.stream(split).iterator();
        iterator.next(); // throw away everything until the first §

        HashSet<String> channelIds = new HashSet<>();
        while (iterator.hasNext()) {
            String channelId = iterator.next();

            if (channelId == null || channelId.isEmpty() || channelId.trim().isEmpty()) {
                throw new MathChannelParseException(
                        "Empty Channel definition found in '" + calculationFormulaWithChannelReferences + "'");
            }

            channelIds.add(channelId);

            if (iterator.hasNext()) {
                iterator.next();// throw away the next string
            }
        }

        return channelIds;
    }

    private static int count(String characterToCount, String searchString) {
        int lengthWithout = searchString.replaceAll(characterToCount, "").length();
        int lengthWith = searchString.length();
        return lengthWith - lengthWithout;
    }

    private static class IntermediateResult {
        /**
         * A math expression that contains only values and no more channel variables
         */
        private final String mathExpression;

        private final Collection<Record> usedRecords;

        private final Set<Channel> inputChannelCache;

        public IntermediateResult(String mathExpression, Collection<Record> usedRecords,
                Set<Channel> inputChannelCache) {
            this.mathExpression = mathExpression;
            this.usedRecords = usedRecords;
            this.inputChannelCache = inputChannelCache;
        }

        public RecordWithInputChannelCache getResult(TimestampMergeStrategy tsStrategy, int samplingIntervalMillis) {
            Double value = new DoubleEvaluator().evaluate(mathExpression);

            List<Long> usedTimestampsMillis = usedRecords.stream()
                    .map(Record::getTimestamp)
                    .sorted()
                    .collect(Collectors.toList());
            Long timestamp = tsStrategy.merge(usedTimestampsMillis);

            final Flag flag;
            long maxTsMillis = usedTimestampsMillis.get(usedTimestampsMillis.size() - 1);
            long minTsMillis = usedTimestampsMillis.get(0);
            long timestampSpanMilliseconds = maxTsMillis - minTsMillis;
            if (timestampSpanMilliseconds > samplingIntervalMillis) {
                flag = Flag.DRIVER_ERROR_READ_FAILURE;
            }
            else {
                flag = Flag.VALID;
            }
            Record record = new Record(new DoubleValue(value), timestamp, flag);
            return new RecordWithInputChannelCache(record, inputChannelCache);
        }
    }

    public static class RecordWithInputChannelCache {
        private final Record record;
        private final Set<Channel> inputChannelCache;

        public RecordWithInputChannelCache(Record record, Set<Channel> inputChannelCache) {
            this.record = record;
            this.inputChannelCache = inputChannelCache;
        }

        public Record getRecord() {
            return record;
        }

        public Set<Channel> getInputChannelCache() {
            return inputChannelCache;
        }
    }
}
