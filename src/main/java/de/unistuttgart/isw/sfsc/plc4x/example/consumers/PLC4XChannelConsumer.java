package de.unistuttgart.isw.sfsc.plc4x.example.consumers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import de.unistuttgart.isw.sfsc.adapter.BootstrapConfiguration;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XMonitorUpdate;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XMonitoringRequest;
import servicepatterns.api.SfscClient;
import servicepatterns.api.SfscServiceApi;
import servicepatterns.api.SfscServiceApiFactory;
import servicepatterns.api.SfscSubscriber;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class PLC4XChannelConsumer {
    static BootstrapConfiguration bootstrapConfiguration1 = new BootstrapConfiguration("127.0.0.1", 1251);
    static ByteString uuid = ByteString.copyFromUtf8(UUID.randomUUID().toString());
    static String ServiceName = "de.universitystuttgart.isw.sfsc.plc4x.monitoring.scapper";
    public static void main(String[] args) {
        SfscServiceApi serverSfscServiceApi = null;
        try {
            serverSfscServiceApi = SfscServiceApiFactory.getSfscServiceApi(bootstrapConfiguration1);
            Map<String, ByteString> genServerTags = null;

            while (genServerTags == null){
                genServerTags = serverSfscServiceApi.getServices(ServiceName).stream()
                        .findAny().orElse(null);
                Thread.sleep(1000);
                System.out.println("Waiting for service");
            }

            PLC4XMonitoringRequest testRequest = PLC4XMonitoringRequest.newBuilder()
                    .setSamplingTime(1000)
                    .setConnectionString("opcua:tcp://127.0.0.1:12686/milo?discovery=false")
                    .setType("opc")
                    .putVariables("TestVar", "ns=2;s=HelloWorld/ScalarTypes/String")
                    .build();

            SfscClient client2 = serverSfscServiceApi.client();
            SfscSubscriber sfscSubscriber = client2.requestChannel(
                    genServerTags,
                    testRequest.toByteString(),
                    10000,
                    message -> {
                        try {
                            PLC4XMonitorUpdate plc4XMonitorUpdate = PLC4XMonitorUpdate.parseFrom(message);
                            System.out.println("Received Update: " + plc4XMonitorUpdate .getValuesMap() + " \n at " + plc4XMonitorUpdate.getTime().getSeconds());
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
