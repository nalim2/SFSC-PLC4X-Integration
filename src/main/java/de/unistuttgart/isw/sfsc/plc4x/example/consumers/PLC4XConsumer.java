package de.unistuttgart.isw.sfsc.plc4x.example.consumers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.unistuttgart.isw.sfsc.adapter.configuration.AdapterConfiguration;
import de.unistuttgart.isw.sfsc.commonjava.util.StoreEvent;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XReadReply;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XReadRequest;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApi;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApiFactory;
import de.unistuttgart.isw.sfsc.framework.api.services.clientserver.SfscClient;
import de.unistuttgart.isw.sfsc.framework.descriptor.SfscServiceDescriptor;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class PLC4XConsumer {
    static AdapterConfiguration adapterConfiguration = new AdapterConfiguration();

    public static void main(String[] args) {
        try {
            adapterConfiguration.setCoreHost("prj-sfsc03.isw.uni-stuttgart.de");
            SfscServiceApi clientSfscServiceApi = SfscServiceApiFactory.getSfscServiceApi(adapterConfiguration);




                clientSfscServiceApi.addOneShotRegistryStoreEventListener(
                        event -> event.getStoreEventType() == StoreEvent.StoreEventType.CREATE
                                 && Objects.equals(event.getData().getServiceName(), "de.universitystuttgart.isw.sfsc.plc4x.read")
                ).await();
                Set<SfscServiceDescriptor> exampleServiceTags = clientSfscServiceApi.getServices("de.universitystuttgart.isw.sfsc.plc4x.read2");
            clientSfscServiceApi.addRegistryStoreEventListener(sfscServiceDescriptorStoreEvent -> {
                        if(sfscServiceDescriptorStoreEvent.getData().getServiceName().equals("UNSER SERVICE NAME") &&
                                sfscServiceDescriptorStoreEvent.getStoreEventType() == StoreEvent.StoreEventType.CREATE){
                            // Subscribe auf den Service
                            sfscServiceDescriptorStoreEvent.getData().getCustomTagsMap();
                            clientSfscServiceApi.subscriber(sfscServiceDescriptorStoreEvent.getData(), message -> {
                                // Send to bridge
                            });
                        }
                    }
                    );

            SfscClient client = clientSfscServiceApi.client();
            for ( SfscServiceDescriptor tags: exampleServiceTags) {
                PLC4XReadRequest readRequest = PLC4XReadRequest.newBuilder()
                        //.setConnectionString("opcua:tcp://milo.digitalpetri.com:62541/milo")
                        .setConnectionString("ads:tcp://141.58.103.40/141.58.103.40.1.1:851/141.58.102.36.1.1:9000")
                        .setVariableAdress("bool1:BOOL")
                        .setName("Nutzlos")
                        .setType("opc")
                        .build();

                client.request(tags, readRequest,
                        replyConsumer(), 300000, () ->
                                System.out.println("timeout"));

            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    static Consumer<ByteString> replyConsumer() {
        return response -> {
            try {
                PLC4XReadReply plc4XReadReply = PLC4XReadReply.parseFrom(response);
                System.out.println("Read request got response: \n" + plc4XReadReply.getStatus() + "\n" + plc4XReadReply.getValue());
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        };
    }


}



