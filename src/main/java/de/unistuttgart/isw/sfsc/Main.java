package de.unistuttgart.isw.sfsc;

import de.unistuttgart.isw.sfsc.config.Constants;
import de.unistuttgart.isw.sfsc.config.EnvironmentConstants;
import de.unistuttgart.isw.sfsc.plc4x.services.PLC4XMonitoringService;
import de.unistuttgart.isw.sfsc.plc4x.services.PLC4XProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Main {

    static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Map<String, String> envVar = System.getenv();
        if(envVar.containsKey(EnvironmentConstants.EXTERNEL_CORE_IP)) Constants.CORE_ADDRESS = envVar.get(EnvironmentConstants.EXTERNEL_CORE_IP);
        if(envVar.containsKey(EnvironmentConstants.EXTERNEL_CORE_PORT)) Constants.CORE_PORT = Integer.parseInt(envVar.get(EnvironmentConstants.EXTERNEL_CORE_PORT));

        log.info(
                String.format("Ramp up of application with the Port: %d and the ip %s ", Constants.CORE_PORT, Constants.CORE_ADDRESS));

        PLC4XMonitoringService.main(args);
        PLC4XProvider.main(args);
    }

}
