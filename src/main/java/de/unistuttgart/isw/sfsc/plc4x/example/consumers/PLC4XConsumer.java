package de.unistuttgart.isw.sfsc.plc4x.example.consumers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.unistuttgart.isw.sfsc.adapter.configuration.AdapterConfiguration;
import de.unistuttgart.isw.sfsc.commonjava.util.StoreEvent;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XReadReply;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XReadRequest;
import de.unistuttgart.isw.sfsc.framework.api.SfscClient;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApi;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApiFactory;
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
            SfscServiceApi clientSfscServiceApi = SfscServiceApiFactory.getSfscServiceApi(adapterConfiguration);




                clientSfscServiceApi.addOneShotRegistryStoreEventListener(
                        event -> event.getStoreEventType() == StoreEvent.StoreEventType.CREATE
                                 && Objects.equals(event.getData().getServiceName(), "de.universitystuttgart.isw.sfsc.plc4x.read")
                ).await();
                Set<SfscServiceDescriptor> exampleServiceTags = clientSfscServiceApi.getServices("de.universitystuttgart.isw.sfsc.plc4x.read");


            SfscClient client = clientSfscServiceApi.client();
            for ( SfscServiceDescriptor tags: exampleServiceTags) {
                PLC4XReadRequest readRequest = PLC4XReadRequest.newBuilder()
                        .setConnectionString("opcua:tcp://127.0.0.1:12686/milo")
                        .setVariableAdress("ns=2;s=HelloWorld/ScalarTypes/String")
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



