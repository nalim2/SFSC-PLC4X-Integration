package de.unistuttgart.isw.sfsc.plc4x.services;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.unistuttgart.isw.sfsc.adapter.configuration.AdapterConfiguration;
import de.unistuttgart.isw.sfsc.config.Constants;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XMonitorUpdate;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XMonitorUpdate.Timestamp;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XMonitoringRequest;
import de.unistuttgart.isw.sfsc.framework.api.*;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.utils.connectionpool.PooledPlcDriverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;


public class PLC4XMonitoringService {
    static AdapterConfiguration bootstrapConfiguration1;
    static ByteString uuid = ByteString.copyFromUtf8(UUID.randomUUID().toString());
    static String ServiceName = "de.universitystuttgart.isw.sfsc.plc4x.monitoring.scrapper";
    static String ChannelName = UUID.randomUUID().toString();
    static Logger log = LoggerFactory.getLogger(PLC4XMonitoringService.class);
    static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

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
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "WARN");
        bootstrapConfiguration1 = new AdapterConfiguration().setCoreHost(Constants.CORE_ADDRESS).setCorePubTcpPort(Constants.CORE_PORT);
        registerSFSCService();

    }

    static void registerSFSCService() {
        try {
            SfscServiceApi serverSfscServiceApi = SfscServiceApiFactory.getSfscServiceApi(bootstrapConfiguration1);
            ChannelGenerator channelGenerator = new ChannelGenerator(serverSfscServiceApi);
            SfscServer channelGeneratorServer = serverSfscServiceApi.channelFactory(new SfscChannelFactoryParameter().setServiceName(ServiceName).setInputMessageType(ByteString.copyFromUtf8(PLC4XMonitoringRequest.class.getName())),
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
            SfscPublisherParameter params = new SfscPublisherParameter()
                    .setServiceName(ChannelName)
                    .setOutputMessageType(ByteString.copyFromUtf8(PLC4XMonitoringRequest.class.getName())).setUnregistered(true);
            SfscPublisher publisher = sfscServiceApi.publisher(params); // custom Tags
            try {
                PLC4XMonitoringRequest request = PLC4XMonitoringRequest.parseFrom(sfscMessage);
                executorService.scheduleAtFixedRate(() -> {

                    PlcConnection connection = null;
                    try {
                        connection = driverManager.getConnection(request.getConnectionString());
                        PlcReadRequest.Builder builderRequest = connection.readRequestBuilder();
                        for (Map.Entry<String, String> entry :
                                request.getVariablesMap().entrySet()) {
                            builderRequest.addItem(entry.getKey(), entry.getValue());
                        }
                        CompletableFuture<? extends PlcReadResponse> future = builderRequest.build().execute();

                        PlcReadResponse response = future.get(1, TimeUnit.SECONDS);
                        long seconds = System.currentTimeMillis() / 1000;
                        int nanos = (int) (System.nanoTime() % 1_000_000);
                        Timestamp timestamp = Timestamp.newBuilder().setSeconds(seconds)
                                .setNanos(nanos).build();

                        PLC4XMonitorUpdate.Builder builder = PLC4XMonitorUpdate.newBuilder()
                                .setItemStatus("GOOD")
                                .setTime(timestamp);
                        for (String keys :
                                response.getFieldNames()) {
                            builder.putValues(keys, response.getString(keys));

                        }
                        publisher.publish(builder.build());
                    } catch (PlcConnectionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }finally {
                        try {
                            if(connection != null) connection.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }, 0, request.getSamplingTime(), TimeUnit.MILLISECONDS);

                // TODO include a function to automatic delete a handle after unsubscribe

            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
            return publisher;

        }


        private static String getValue(Object value) throws ConnectException {
            Objects.requireNonNull(value);
            if (value instanceof BigDecimal) {
                return "" + (BigDecimal) value;
            }
            if (value instanceof Boolean) {
                return "" + (Boolean) value;
            }
            if (value instanceof byte[]) {
                return "" + (byte[]) value;
            }
            if (value instanceof Byte) {
                return "" + (Byte) value;
            }
            if (value instanceof Double) {
                return "" + (Double) value;
            }
            if (value instanceof Float) {
                return "" + (Float) value;
            }
            if (value instanceof Integer) {
                return "" + (Integer) value;
            }
            if (value instanceof LocalDate) {
                return "" + (LocalDate) value;
            }
            if (value instanceof LocalDateTime) {
                return "" + (LocalDateTime) value;
            }
            if (value instanceof LocalTime) {
                return "" + (LocalTime) value;
            }
            if (value instanceof Long) {
                return "" + (Long) value;
            }
            if (value instanceof Short) {
                return "" + (Short) value;
            }
            if (value instanceof String) {
                return "" + (String) value;
            }
            // TODO: add support for collective and complex types
            throw new ConnectException(String.format("Unsupported data type %s", value.getClass().getName()));
        }

    }
}
