package de.unistuttgart.isw.sfsc.plc4x.example.consumers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.unistuttgart.isw.sfsc.adapter.configuration.AdapterConfiguration;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XMonitorUpdate;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XMonitoringRequest;
import de.unistuttgart.isw.sfsc.framework.api.SfscClient;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApi;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApiFactory;
import de.unistuttgart.isw.sfsc.framework.api.SfscSubscriber;
import de.unistuttgart.isw.sfsc.framework.descriptor.SfscServiceDescriptor;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class PLC4XChannelConsumer {
    static AdapterConfiguration adapterConfiguration = new AdapterConfiguration();
    static ByteString uuid = ByteString.copyFromUtf8(UUID.randomUUID().toString());
    static String ServiceName = "de.universitystuttgart.isw.sfsc.plc4x.monitoring.scrapper";
    public static void main(String[] args) {
        SfscServiceApi serverSfscServiceApi = null;
        try {
            serverSfscServiceApi = SfscServiceApiFactory.getSfscServiceApi(adapterConfiguration);
            SfscServiceDescriptor genServerTags = null;

            while (genServerTags == null){
                genServerTags = serverSfscServiceApi.getServices(ServiceName).stream()
                        .findAny().orElse(null);
                Thread.sleep(1000);
                System.out.println("Waiting for service");
            }

            PLC4XMonitoringRequest testRequest = PLC4XMonitoringRequest.newBuilder()
                    .setSamplingTime(1000)
                    .setConnectionString("opcua:tcp://milo.digitalpetri.com:62541/milo")
                    .setType("opc")
                    .putVariables("TestVar", "ns=2;s=HelloWorld/ScalarTypes/Int16")
                    .build();

            SfscClient client2 = serverSfscServiceApi.client();
            SfscSubscriber sfscSubscriber = client2.requestChannel(
                    genServerTags,
                    testRequest.toByteString(),
                    10000,
                    message -> {
                        try {
                            PLC4XMonitorUpdate plc4XMonitorUpdate = PLC4XMonitorUpdate.parseFrom(message);
                            System.out.println("Received Update: " + plc4XMonitorUpdate .getValuesOrDefault("TestVar", "NotSetValue") + " \n at " + plc4XMonitorUpdate.getTime().getSeconds());
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                    }
            ).get();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }



}
