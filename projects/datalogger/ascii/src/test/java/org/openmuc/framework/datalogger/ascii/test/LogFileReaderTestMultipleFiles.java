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
package org.openmuc.framework.datalogger.ascii.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.ascii.AsciiLogger;
import org.openmuc.framework.datalogger.ascii.LogFileReader;
import org.openmuc.framework.datalogger.ascii.LogFileWriter;
import org.openmuc.framework.datalogger.ascii.LogIntervalContainerGroup;
import org.openmuc.framework.datalogger.ascii.utils.LoggerUtils;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;

public class LogFileReaderTestMultipleFiles {

    // t1 = start timestamp of requestet interval
    // t2 = end timestamp of requestet interval

    private static final String Channel0Name = "power";
    private static final String EXT = ".dat";
    static int loggingTimeOffset = 0; // ms
    private static String fileDate0 = "20770707";
    private static String fileDate1 = "20770708";
    private static String fileDate2 = "20770709";
    private static int loggingInterval = 60000; // ms;
    // private static String[] channelIds = new String[] { Channel0Name };
    private static String dateFormat = "yyyyMMdd HH:mm:ss";
    // private static String ext = ".dat";

    LogChannelTestImpl channelTestImpl = new LogChannelTestImpl(Channel0Name, "", "Comment", "W", ValueType.DOUBLE, 0.0,
            0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);

    @BeforeAll
    public static void setup() {

        System.out.println("### Setup() LogFileReaderTestMultipleFiles");

        TestUtils.createTestFolder();
        // drei Dateien

        // 1 Kanal im Sekunden-Takt loggen ueber von 23 Uhr bis 1 Uhr des uebernaechsten Tages
        // --> Ergebnis muessten drei
        // Dateien sein die vom LogFileWriter erstellt wurden

        String filename0 = TestUtils.TESTFOLDERPATH + fileDate0 + "_" + loggingInterval + EXT;
        String filename1 = TestUtils.TESTFOLDERPATH + fileDate1 + "_" + loggingInterval + EXT;
        String filename2 = TestUtils.TESTFOLDERPATH + fileDate2 + "_" + loggingInterval + EXT;

        File file0 = new File(filename0);
        File file1 = new File(filename1);
        File file2 = new File(filename2);

        if (file0.exists()) {
            System.out.println("Delete File " + filename2);
            file0.delete();
        }
        if (file1.exists()) {
            System.out.println("Delete File " + filename1);
            file1.delete();
        }
        if (file2.exists()) {
            System.out.println("Delete File " + filename2);
            file2.delete();
        }

        HashMap<String, LogChannel> logChannelList = new HashMap<>();

        LogChannelTestImpl ch0 = new LogChannelTestImpl("power", "", "dummy description", "kW", ValueType.DOUBLE, 0.0,
                0.0, false, 1000, 0, "", loggingInterval, loggingTimeOffset, false, false);

        logChannelList.put(Channel0Name, ch0);

        Calendar calendar = TestUtils.stringToDate(dateFormat, fileDate0 + " 23:00:00");

        int hour = 3600;

        for (int i = 0; i < ((hour * 24 + hour * 2) * (1000d / loggingInterval)); i++) {
            LoggingRecord container1 = new LoggingRecord(Channel0Name,
                    new Record(new DoubleValue(1), calendar.getTimeInMillis()));

            LogIntervalContainerGroup group = new LogIntervalContainerGroup();
            group.add(container1);

            LogFileWriter lfw = new LogFileWriter(TestUtils.TESTFOLDERPATH, false);
            lfw.log(group, loggingInterval, 0, calendar, logChannelList);
            AsciiLogger.setLastLoggedLineTimeStamp(loggingInterval, 0, calendar.getTimeInMillis());

            calendar.add(Calendar.MILLISECOND, loggingInterval);
        }
        // }
    }

    @AfterAll
    public static void tearDown() {

        System.out.println("tearing down");
        TestUtils.deleteTestFolder();
    }

    @Test
    public void tc009_t1_t2_within_available_data_with_three_files() {

        System.out.println("### Begin test tc009_t1_t2_within_available_data_with_three_files");

        long t1 = TestUtils.stringToDate(dateFormat, fileDate0 + " 23:00:00").getTimeInMillis();
        long t2 = TestUtils.stringToDate(dateFormat, fileDate2 + " 00:59:" + (60 - loggingInterval / 1000))
                .getTimeInMillis();

        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        List<Record> records = fr.getValues(t1, t2).get(channelTestImpl.getId());

        int hour = 3600;
        long expectedRecords = (hour * 24 + hour * 2) / (loggingInterval / 1000);
        System.out.print(Thread.currentThread().getStackTrace()[1].getMethodName());

        boolean result;

        if (records.size() == expectedRecords) {
            result = true;
        }
        else {
            result = false;
        }
        System.out.println(" records = " + records.size() + " (" + expectedRecords + " expected); ");
        assertTrue(result);
    }

    @Test
    public void tc010_test_getValues() {
        LogFileReader fr = new LogFileReader(TestUtils.TESTFOLDERPATH, channelTestImpl);
        String filename1 = TestUtils.TESTFOLDERPATH + fileDate1 + "_" + loggingInterval + EXT;
        File file1 = new File(filename1);
        if (!file1.exists()) {
            fail("File does not exist at path " + file1.getAbsolutePath());
        }
        int expected = 1440;
        Map<String, List<Record>> values = fr.getValues(file1.getPath());
        for (Map.Entry<String, List<Record>> entry : values.entrySet()) {
            List<Record> records = entry.getValue();
            int actual = records.size();
            assertEquals(expected, actual);
        }
    }

    @Test
    public void tc_011_test_getAllDataFiles() {
        String dir = TestUtils.TESTFOLDERPATH;
        List<File> files = LoggerUtils.getAllDataFiles(dir);
        List<String> expected = new LinkedList<>();
        expected.add("20770709_60000.dat");
        expected.add("20770708_60000.dat");
        expected.add("20770707_60000.dat");
        List<String> actual = new LinkedList<>();
        for (File file : files) {
            actual.add(file.getName());
        }
        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));
    }

    @Test
    public void tc_012_test_getLatestFile() {
        String dir = TestUtils.TESTFOLDERPATH;
        List<File> files = LoggerUtils.getAllDataFiles(dir);
        String expected = "20770709_60000.dat";
        File file = LoggerUtils.getLatestFile(files);
        String actual = file.getName();
        assertEquals(expected, actual);
    }
}
