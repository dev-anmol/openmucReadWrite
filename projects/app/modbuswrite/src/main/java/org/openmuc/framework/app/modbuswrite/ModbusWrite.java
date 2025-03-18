//     // package org.openmuc.framework.app.modbuswrite;

//     // import org.openmuc.framework.driver.modbus.ModbusConnection;
//     // import org.openmuc.framework.driver.modbus.tcp.ModbusTCPConnection;
//     // import org.openmuc.framework.driver.modbus.ModbusChannel;
//     // import org.openmuc.framework.data.ShortValue;
//     // import org.openmuc.framework.dataaccess.DataAccessService;
//     // import com.ghgande.j2mod.modbus.ModbusException;
//     // import org.openmuc.framework.driver.spi.ConnectionException;
//     // import org.osgi.framework.BundleActivator;
//     // import org.osgi.framework.BundleContext;
//     // import org.osgi.framework.ServiceReference;
//     // import org.osgi.service.component.annotations.Component;
//     // // import org.osgi.service.component.annotations.Activate;
//     // // import org.osgi.service.component.annotations.Component;
//     // // import org.osgi.service.component.annotations.Deactivate;
//     // // import org.osgi.service.component.annotations.Reference;

//     // @Component(service = {})
//     // public class ModbusWrite implements BundleActivator {

//     //     private DataAccessService dataAccess;

//     //     @Override
//     //     public void start(BundleContext context) throws Exception {
//     //         System.out.println("Starting ModbusWrite application...");

//     //         ServiceReference<?> serviceReference = context.getServiceReference(DataAccessService.class.getName());
//     //         if(serviceReference != null){
//     //             dataAccess = (DataAccessService) context.getService(serviceReference);
//     //         }
//     //         if (dataAccess == null) {
//     //             throw new Exception("DataAccessService not available.");
//     //         }
//     //         try {
//     //             // Create connection to diagslave on 127.0.0.1:502
//     //             ModbusConnection connection = new ModbusTCPConnection("127.0.0.1:502", 3000, true);
//     //             connection.connect();

//     //             // Define the channel (unit ID 1, holding register 1000, INT16, write access)
//     //             ModbusChannel channel = new ModbusChannel("1:HOLDING_REGISTERS:1000:INT16", ModbusChannel.EAccess.WRITE);

//     //             // Write a value to the channel
//     //             connection.writeChannel(channel, new ShortValue((short) 1234));

//     //             System.out.println("Value 1234 written successfully to holding register 1000!");

//     //             // Disconnect
//     //             connection.disconnect();

//     //         } catch (ConnectionException | ModbusException e) {
//     //             System.err.println("Error writing to Modbus: " + e.getMessage());
//     //             throw e;
//     //         }
//     //     }

//     //     @Override
//     //     public void stop(BundleContext context) throws Exception {
//     //         System.out.println("Stopping ModbusWrite application...");
//     //     }
//     // }

//     // package org.openmuc.framework.app.modbuswrite;

//     // import org.openmuc.framework.data.ShortValue;
//     // import org.openmuc.framework.dataaccess.Channel;
//     // import org.openmuc.framework.dataaccess.DataAccessService;
//     // import org.osgi.service.component.annotations.Activate;
//     // import org.osgi.service.component.annotations.Component;
//     // import org.osgi.service.component.annotations.Deactivate;
//     // import org.osgi.service.component.annotations.Reference;
//     // import org.slf4j.Logger;
//     // import org.slf4j.LoggerFactory;
    
//     // @Component(service = {})
//     // public final class ModbusWrite {
    
//     //     private static final Logger logger = LoggerFactory.getLogger(ModbusWrite.class);
//     //     private static final String APP_NAME = "OpenMUC ModbusWrite App";
    
//     //     private Channel modbusChannel;
    
//     //     @Reference
//     //     private DataAccessService dataAccessService;
    
//     //     @Activate
//     //     private void activate() {
//     //         logger.info("Activating {}", APP_NAME);
    
//     //         // Get the pre-defined channel "register1" from the configuration
//     //         modbusChannel = dataAccessService.getChannel("register1");
    
//     //         if (modbusChannel != null) {
//     //             try {
//     //                 // Example short value to write (within SHORT range: -32768 to 32767)
//     //                 short valueToWrite = 1234;
//     //                 modbusChannel.write(new ShortValue(valueToWrite));
//     //                 logger.info("Successfully wrote short value {} to Modbus channel 'register1'", valueToWrite);
//     //             } catch (Exception e) {
//     //                 logger.error("Failed to write to Modbus channel 'register1': {}", e.getMessage(), e);
//     //             }
//     //         } else {
//     //             logger.error("Failed to get Modbus channel 'register1'. Check configuration.");
//     //         }
//     //     }
    
//     //     @Deactivate
//     //     private void deactivate() {
//     //         logger.info("Deactivating {}", APP_NAME);
//     //         modbusChannel = null; // Clean up reference
//     //     }
//     // }


//     package org.openmuc.framework.app.modbuswrite;

// import org.openmuc.framework.data.ShortValue;
// import org.openmuc.framework.dataaccess.Channel;
// import org.openmuc.framework.dataaccess.DataAccessService;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Deactivate;
// import org.osgi.service.component.annotations.Reference;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import java.util.Timer;
// import java.util.TimerTask;

// @Component(service = {})
// public final class ModbusWrite {
//     private static final Logger logger = LoggerFactory.getLogger(ModbusWrite.class);
//     private static final String APP_NAME = "OpenMUC ModbusWrite App";
//     private static final long UPDATE_INTERVAL_MS = 5000; // 5 seconds

//     private Channel modbusChannel;
//     private Timer updateTimer;

//     @Reference
//     private DataAccessService dataAccessService;

//     @Activate
//     private void activate() {
//         logger.info("Activating {}", APP_NAME);

//         modbusChannel = dataAccessService.getChannel("register1"); // Use register1 from your config
//         if (modbusChannel == null) {
//             logger.error("Failed to get Modbus channel 'register1'. Check configuration.");
//             return;
//         }

//         initUpdateTimer();
//         logger.info("Successfully initialized periodic write to 'register1'");
//     }

//     @Deactivate
//     private void deactivate() {
//         logger.info("Deactivating {}", APP_NAME);
//         if (updateTimer != null) {
//             updateTimer.cancel();
//             updateTimer.purge();
//         }
//         modbusChannel = null;
//     }

//     private void initUpdateTimer() {
//         updateTimer = new Timer("ModbusWrite Update");
//         TimerTask task = new TimerTask() {
//             private short valueToWrite = 1234; // Example short value (-32768 to 32767)
//             @Override
//             public void run() {
//                 try {
//                     modbusChannel.write(new ShortValue(valueToWrite));
//                     logger.info("Wrote short value {} to 'register1'", valueToWrite);
//                     valueToWrite += 1; // Increment for testing (optional)
//                     if (valueToWrite > 32767) valueToWrite = 1234; // Reset if exceeding SHORT max
//                 } catch (Exception e) {
//                     logger.error("Failed to write to 'register1': {}", e.getMessage(), e);
//                 }
//             }
//         };
//         updateTimer.scheduleAtFixedRate(task, 0, UPDATE_INTERVAL_MS);
//     }
// }

package org.openmuc.framework.app.modbuswrite;

import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.ChannelChangeListener;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.RecordListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.ConnectionPoolDataSource;
import java.util.Timer;
import java.util.TimerTask;
@Component(service = {})
public final class ModbusWrite {
    private static final Logger logger = LoggerFactory.getLogger(ModbusWrite.class);
    private static final String APP_NAME = "OpenMUC ModbusWrite App";
    private static final long UPDATE_INTERVAL_MS = 5000; // 5 seconds
    private Channel modbusChannel;
    private Timer updateTimer;
    private boolean isConnected = false;
    private DataAccessService dataAccessService;
    @Reference
    public void setDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }
    @Activate
    private void activate() {
        logger.info("Activating {}", APP_NAME);
        modbusChannel = dataAccessService.getChannel("register1"); // Use register1 from your config
        if (modbusChannel == null) {
            logger.error("Failed to get Modbus channel 'register1'. Check configuration.");
            return;
        }
        initUpdateTimer();
        logger.info("Successfully initialized periodic write to 'register1'");
    }
    @Deactivate
    private void deactivate() {
        logger.info("Deactivating {}", APP_NAME);
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer.purge();
        }
        modbusChannel = null;
    }

    private void initUpdateTimer() {
        updateTimer = new Timer("ModbusWrite Update");
        TimerTask task = new TimerTask() {
            private short valueToWrite = 1234;
            @Override
            public void run() {
                try {
                    // Check whether the device is connected or not.
                    if (!isDeviceConnected()) {
                        logger.warn("Modbus device not connected, skipping write operation");
                        return;
                    }
                    modbusChannel.write(new ShortValue(valueToWrite));
                    logger.info("Wrote short value {} to 'register1'", valueToWrite);
                    valueToWrite += 1;
                    if (valueToWrite > 32767) valueToWrite = 1234;
                }
                catch (Exception e) {
                    logger.error("Failed to write to 'register1': {}", e.getMessage(), e);
                    isConnected = false;
                }
            }
        };
        updateTimer.scheduleAtFixedRate(task, 0, UPDATE_INTERVAL_MS);
    }

    // This is added to check whether the device is connected or not.
    private boolean isDeviceConnected(){
        try{
            isConnected = modbusChannel.isConnected();
            return isConnected;
        }
        catch(Exception e ){
            isConnected = false;
            return false;
        }
    }
}