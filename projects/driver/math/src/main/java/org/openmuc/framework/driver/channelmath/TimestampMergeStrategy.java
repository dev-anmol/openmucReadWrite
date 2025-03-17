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
import java.util.stream.Collectors;

import org.openmuc.framework.dataaccess.Channel;

/**
 * Defines how to merge timestamps of {@link org.openmuc.framework.data.Record}s that are calculated from other
 * {@link org.openmuc.framework.data.Record}s.
 */

public enum TimestampMergeStrategy {

    AVG("use_average_timestamp") {
        @Override
        public long merge(Long... timestamps) {
            long sum = 0;
            for (long ts : timestamps) {
                sum += ts;
            }
            return sum / timestamps.length;
        }
    },

    OLDEST("use_older_timestamp") {
        @Override
        public long merge(Long... timestamps) {
            long oldest = Long.MAX_VALUE;
            for (long ts : timestamps) {
                if (ts < oldest) {
                    oldest = ts;
                }
            }
            return oldest;
        }
    },

    NEWEST("use_newer_timestamp") {
        @Override
        public long merge(Long... timestamps) {
            long newest = Long.MIN_VALUE;
            for (long ts : timestamps) {
                if (ts > newest) {
                    newest = ts;
                }
            }
            return newest;
        }
    };

    public static final String SETTINGS_PREFIX = "math-ts-strategy=";
    private final static String KNOWN_KEYS = Arrays.stream(TimestampMergeStrategy.values())
            .map(TimestampMergeStrategy::getDeviceSettingsString)
            .collect(Collectors.joining(", "));

    private final String deviceSettingsString;

    TimestampMergeStrategy(String deviceSettingsString) {
        this.deviceSettingsString = deviceSettingsString;
    }

    public static TimestampMergeStrategy getDefaultMergeStrategy() {
        return OLDEST;
    }

    public static TimestampMergeStrategy parseDeviceSettings(Channel channel) throws MathChannelParseException {
        String deviceSettingsFromChannelXml = channel.getSettings();
        TimestampMergeStrategy strategy = getDefaultMergeStrategy();

        for (String singleSetting : deviceSettingsFromChannelXml.split(";")) {
            if (singleSetting.startsWith(SETTINGS_PREFIX)) {
                final String settingStringValue = singleSetting.substring(SETTINGS_PREFIX.length());

                strategy = Arrays.stream(TimestampMergeStrategy.values())//
                        .filter(knownStrategy -> knownStrategy.deviceSettingsString.equals(settingStringValue))//
                        .findFirst()
                        .orElseThrow(() -> raiseUnknownSettingsException(singleSetting));
            }
        }
        return strategy;
    }

    private static MathChannelParseException raiseUnknownSettingsException(String singleSetting) {
        return new MathChannelParseException("Unable to parse " + TimestampMergeStrategy.class.getSimpleName()
                + " from device string " + singleSetting + ". Known keys are: " + KNOWN_KEYS);
    }

    public String getDeviceSettingsString() {
        return deviceSettingsString;
    }

    /**
     * Calculates the timestamp of the result of the math operation
     */
    public abstract long merge(Long... timestamps);

    public final long merge(Collection<Long> timestamps) {
        Long[] timestampsArray = new Long[timestamps.size()];
        return merge(timestamps.toArray(timestampsArray));
    }
}
