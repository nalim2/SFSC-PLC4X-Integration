package de.unistuttgart.isw.sfsc;

import de.unistuttgart.isw.sfsc.config.Constants;
import de.unistuttgart.isw.sfsc.config.EnvironmentConstants;
import de.unistuttgart.isw.sfsc.plc4x.example.endpoints.StartMiloServer;
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
        if(envVar.containsKey(EnvironmentConstants.RUN_MONITORING_SERVICE)) Constants.RUN_MONITOR_SERVICE = Boolean.parseBoolean(envVar.get(EnvironmentConstants.RUN_MONITORING_SERVICE).toLowerCase());
        if(envVar.containsKey(EnvironmentConstants.RUN_READ_SERVICE)) Constants.RUN_READ_SERVICE = Boolean.parseBoolean(envVar.get(EnvironmentConstants.RUN_READ_SERVICE).toLowerCase());
        if(envVar.containsKey(EnvironmentConstants.RUN_WRITE_SERVICE)) Constants.RUN_WRITE_SERVICE = Boolean.parseBoolean(envVar.get(EnvironmentConstants.RUN_WRITE_SERVICE).toLowerCase());
        if(envVar.containsKey(EnvironmentConstants.RUN_OPC_EXAMPLE_SERVER)) Constants.RUN_OPC_EXAMPLE_SERVER = Boolean.parseBoolean(envVar.get(EnvironmentConstants.RUN_OPC_EXAMPLE_SERVER).toLowerCase());
        log.info(
                String.format("Ramp up of application with the Port: %d and the ip %s ", Constants.CORE_PORT, Constants.CORE_ADDRESS));

        if(Constants.RUN_OPC_EXAMPLE_SERVER) StartMiloServer.main(args);
        if(Constants.RUN_MONITOR_SERVICE)PLC4XMonitoringService.main(args);
        if(Constants.RUN_READ_SERVICE)PLC4XProvider.main(args);
        if(Constants.RUN_WRITE_SERVICE) log.info("The not implemented write service got triggered to start");
    }

}
