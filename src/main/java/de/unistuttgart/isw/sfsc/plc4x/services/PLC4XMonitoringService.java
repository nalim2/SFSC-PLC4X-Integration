package de.unistuttgart.isw.sfsc.plc4x.services;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.unistuttgart.isw.sfsc.config.Constants;
import de.unistuttgart.isw.sfsc.adapter.BootstrapConfiguration;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XMonitorUpdate;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XMonitoringRequest;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XMonitorUpdate.Timestamp;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.scraper.config.JobConfigurationImpl;
import org.apache.plc4x.java.scraper.config.ScraperConfiguration;
import org.apache.plc4x.java.scraper.config.triggeredscraper.ScraperConfigurationTriggeredImpl;
import org.apache.plc4x.java.scraper.exception.ScraperException;
import org.apache.plc4x.java.scraper.triggeredscraper.TriggeredScraperImpl;
import org.apache.plc4x.java.scraper.triggeredscraper.triggerhandler.collector.TriggerCollector;
import org.apache.plc4x.java.scraper.triggeredscraper.triggerhandler.collector.TriggerCollectorImpl;
import org.apache.plc4x.java.utils.connectionpool.PooledPlcDriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import servicepatterns.api.SfscPublisher;
import servicepatterns.api.SfscServer;
import servicepatterns.api.SfscServiceApi;
import servicepatterns.api.SfscServiceApiFactory;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;


public class PLC4XMonitoringService {
    static BootstrapConfiguration bootstrapConfiguration1;
    static ByteString uuid = ByteString.copyFromUtf8(UUID.randomUUID().toString());
    static String ServiceName = "de.universitystuttgart.isw.sfsc.plc4x.monitoring.scapper";
    static String ChannelName = UUID.randomUUID().toString();
    static Logger log = LoggerFactory.getLogger(PLC4XMonitoringService.class);


    static PlcDriverManager driverManager = new PooledPlcDriverManager(pooledPlcConnectionFactory -> {
        GenericKeyedObjectPoolConfig<PlcConnection> config = new GenericKeyedObjectPoolConfig<>();
        config.setJmxEnabled(true);
        config.setMaxWaitMillis(-1);
        config.setMaxTotal(3);
        config.setMinIdlePerKey(0);
        config.setBlockWhenExhausted(true);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        return new GenericKeyedObjectPool<>(pooledPlcConnectionFactory, config);
    });

    public static void main(String[] args) {
        bootstrapConfiguration1 = new BootstrapConfiguration(Constants.CORE_ADDRESS, Constants.CORE_PORT);


        registerSFSCService();

    }

    static void registerSFSCService(){
        try {
            SfscServiceApi serverSfscServiceApi = SfscServiceApiFactory.getSfscServiceApi(bootstrapConfiguration1);
            ChannelGenerator channelGenerator = new ChannelGenerator(serverSfscServiceApi);
            SfscServer channelGeneratorServer = serverSfscServiceApi.channelGenerator(
                    ServiceName,
                    Map.of("id", uuid), // custom Tags
                    ByteString.copyFromUtf8(UUID.randomUUID().toString()), // Server Topic --> will be encapsulated
                    ByteString.copyFromUtf8("ignored"), // Description of the input message type
                    channelGenerator); // channel factory
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            log.error("Timeout Exception because there is no SFSC-Core available");
            log.info("Try to restart the service");
            registerSFSCService();
        }
    }

    static class ChannelGenerator implements Function<ByteString, SfscPublisher> {

        private final SfscServiceApi sfscServiceApi;

        ChannelGenerator(SfscServiceApi sfscServiceApi) {
            this.sfscServiceApi = sfscServiceApi;
        }

        @Override
        public SfscPublisher apply(ByteString sfscMessage) {


            try {
                String jobName = "ScheduleJob";
                String sourceDeviceName = "DeviceSource";

                PLC4XMonitoringRequest request = PLC4XMonitoringRequest.parseFrom(sfscMessage);
                String triggerConfig = "(SCHEDULED,100)";
                JobConfigurationImpl jobConfiguration = new JobConfigurationImpl(
                        jobName,
                        triggerConfig,
                        request.getSamplingTime(),
                        List.of(sourceDeviceName),
                        request.getVariablesMap());

                ScraperConfiguration configuration = new ScraperConfigurationTriggeredImpl(Collections.singletonMap(sourceDeviceName, request.getConnectionString()), List.of(jobConfiguration));
                TriggerCollector triggerCollector = new TriggerCollectorImpl(driverManager);

                SfscPublisher publisher = sfscServiceApi.unregisteredPublisher(
                        ChannelName,
                        ByteString.copyFromUtf8(UUID.randomUUID().toString()), // Topic
                        ByteString.copyFromUtf8(PLC4XMonitoringRequest.class.getName()), // Message Type
                        Collections.emptyMap()); // custom Tags



                    TriggeredScraperImpl scraper = new TriggeredScraperImpl(
                            (j, a, m) ->
                            {
                                try {
                                    long seconds = System.currentTimeMillis() / 1000;
                                    int nanos = (int)(System.nanoTime() % 1_000_000);
                                    Timestamp timestamp = Timestamp.newBuilder().setSeconds(seconds)
                                            .setNanos(nanos).build();

                                    PLC4XMonitorUpdate.Builder builder = PLC4XMonitorUpdate.newBuilder()
                                            .setItemStatus("GOOD")
                                            .setTime(timestamp);
                                    for (Map.Entry<String, Object> entry :
                                            m.entrySet()) {

                                        builder.putValues(entry.getKey(), getValue(entry.getValue()));

                                    }
                                    publisher.publish(builder.build());

                                } catch (ConnectException e) {
                                    e.printStackTrace();
                                }
                            }
                            ,
                            driverManager,
                            configuration.getJobs(),
                            triggerCollector,
                            1000);

                publisher.onSubscription(() -> {

                    scraper.start();
                        }
                );
                // TODO include a function to automatic delete a handle after unsubscribe
                return publisher;
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                return null;
            } catch (ScraperException e) {
                e.printStackTrace();
                return null;
            }


        }
    }


    private static String getValue(Object value) throws ConnectException {
        Objects.requireNonNull(value);
        if (value instanceof BigDecimal) {
            return "" + (BigDecimal)value;
        }
        if (value instanceof Boolean) {
            return "" + (Boolean)value;
        }
        if (value instanceof byte[]) {
            return "" + (byte[])value;
        }
        if (value instanceof Byte) {
            return "" + (Byte)value;
        }
        if (value instanceof Double) {
            return "" + (Double)value;
        }
        if (value instanceof Float) {
            return "" + (Float)value;
        }
        if (value instanceof Integer) {
            return "" + (Integer)value;
        }
        if (value instanceof LocalDate) {
            return "" + (LocalDate)value;
        }
        if (value instanceof LocalDateTime) {
            return "" + (LocalDateTime)value;
        }
        if (value instanceof LocalTime) {
            return "" + (LocalTime)value;
        }
        if (value instanceof Long) {
            return "" + (Long)value;
        }
        if (value instanceof Short) {
            return "" + (Short)value;
        }
        if (value instanceof String) {
            return "" + (String)value;
        }
        // TODO: add support for collective and complex types
        throw new ConnectException(String.format("Unsupported data type %s", value.getClass().getName()));
    }

}
