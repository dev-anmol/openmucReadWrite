
// // package org.openmuc.framework.app.modbusread;

// // import org.openmuc.framework.dataaccess.DataAccessService;
// // import org.openmuc.framework.dataaccess.RecordListener;
// // import org.openmuc.framework.dataaccess.Channel;
// // // import org.openmuc.framework.data.Record;
// // import org.osgi.service.component.annotations.Activate;
// // import org.osgi.service.component.annotations.Component;
// // import org.osgi.service.component.annotations.Deactivate;
// // import org.osgi.service.component.annotations.Reference;
// // import org.slf4j.Logger;
// // import org.slf4j.LoggerFactory;

// // @Component(service = {})
// // public final class ModbusRead {
// //     private static final Logger logger = LoggerFactory.getLogger(ModbusRead.class);
// //     private static final String APP_NAME = "OpenMUC ModbusRead App";
    
// //     private Channel modbusChannel;
// //     private RecordListener modbusListener;

// //     @Reference
// //     private DataAccessService dataAccessService;

// //     @Activate
// //     private void activate() {
// //         logger.info("Activating {}", APP_NAME);
// //         modbusListener = new ModbusListener();
// //         modbusChannel = dataAccessService.getChannel("register1");
        
// //         if (modbusChannel != null) {
// //             modbusChannel.addListener(modbusListener);
// //             logger.info("Successfully added listener to Modbus channel");
// //         }
// //         else {
// //             logger.error("Failed to get Modbus channel 'register1'. Check channels.xml configuration.");
// //         }
// //     }

// //     @Deactivate
// //     private void deactivate() {
// //         logger.info("Deactivating {}", APP_NAME);
// //         if (modbusChannel != null && modbusListener != null) {
// //             modbusChannel.removeListener(modbusListener);
// //         }
// //     }
// // }


// package org.openmuc.framework.app.modbusread;

// import org.openmuc.framework.data.Record;
// import org.openmuc.framework.dataaccess.DataAccessService;
// import org.openmuc.framework.dataaccess.RecordListener;
// import org.openmuc.framework.dataaccess.Channel;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Deactivate;
// import org.osgi.service.component.annotations.Reference;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// @Component(service = {})
// public final class ModbusRead {
//     private static final Logger logger = LoggerFactory.getLogger(ModbusRead.class);
//     private static final String APP_NAME = "OpenMUC ModbusRead App";
    
//     private Channel modbusChannel;
//     private RecordListener modbusListener;

//     @Reference
//     private DataAccessService dataAccessService;

//     @Activate
//     private void activate() {
//         logger.info("Activating {}", APP_NAME);
        
//         modbusChannel = dataAccessService.getChannel("register1");
//         if (modbusChannel == null) {
//             logger.error("Failed to get Modbus channel 'register1'. Check channels.xml configuration.");
//             return; // Early exit if channel is missing
//         }

//         modbusListener = new ModbusListener();
//         modbusChannel.addListener(modbusListener);
//         logger.info("Successfully added listener to Modbus channel 'register1'");
//     }

//     @Deactivate
//     private void deactivate() {
//         logger.info("Deactivating {}", APP_NAME);
//         if (modbusChannel != null && modbusListener != null) {
//             modbusChannel.removeListener(modbusListener);
//         }
//     }

//     private class ModbusListener implements RecordListener {
//         @Override
//         public void newRecord(Record record) {
//             if (record != null && record.getValue() != null) {
//                 logger.info("New value from register1: {} (Timestamp: {})", 
//                     record.getValue().asShort(), record.getTimestamp());
//             } else {
//                 logger.warn("Received null record or value from register1");
//             }
//         }
//     }
// }

package org.openmuc.framework.app.modbusread;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.ChannelState;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.RecordListener;
import org.openmuc.framework.dataaccess.Channel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component(service = {})
public final class ModbusRead {
    private static final Logger logger = LoggerFactory.getLogger(ModbusRead.class);
    private static final String APP_NAME = "OpenMUC ModbusRead App";
    private Channel modbusChannel;

    private RecordListener modbusListener;

    private DataAccessService dataAccessService;

    @Reference
    public void setDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    @Activate
    private void activate() {
        logger.info("Activating {}", APP_NAME);
        modbusChannel = dataAccessService.getChannel("register1");
        if (modbusChannel == null) {
            logger.error("Failed to get Modbus channel 'register1'. Check channels.xml configuration.");
            //throw new IllegalStateException("Modbus channel 'register1' is not available.ACTIVATION ABORTED");
            return; // Early exit if channel is missing
        }


        logger.info("Successfully added listener to Modbus channel 'register1'");
    }

    @Deactivate
    private void deactivate() {
        logger.info("Deactivating {}", APP_NAME);
        if (modbusChannel != null && modbusListener != null) {
            modbusChannel.removeListener(modbusListener);
        }
    }

    private class ModbusListener implements RecordListener {
        @Override
        public void newRecord(Record record) {
            if (record != null && record.getValue() != null) {
                int value = record.getValue().asShort();
                logger.info("New value from register1: {} (Timestamp: {})",
                        record.getValue().asShort(), record.getTimestamp());
                try{
                   // dsR.writeData(value);
                    }catch (IllegalStateException e ){
                    logger.error("Failed to write data to DigiSlave. {} ",e.getMessage());
                }

                try{
                    //int readValue = dsR.readData();
                    //logger.info("Read value from DigiSlaveRegister : {}",readValue);
                }catch (IllegalStateException e ){
                    logger.error("Failed to read data from DigiSlave register: {}",e.getMessage());
                }
            }
            else{
                logger.warn("Recieved null Record or value from register1");
            }
            }
    }





//    public class DigiSlaveRegister{
//        private boolean isClosed = false;
//        private int data;
//        public void close(){
//            isClosed = true;
//        }
//        public void writeData(int data){
//            if(isClosed){
//                throw new IllegalStateException("Cannot write to a closed register");
//            }
//            this.data = data;
//        }
//        public int readData(){
//            if(isClosed){
//                throw new IllegalStateException("Cannot read from a closed register");
//            }
//            return data;
//        }
//        public boolean isClosed(){
//            return isClosed;
//        }
//
//    }

}