package de.unistuttgart.isw.sfsc.plc4x.example.consumers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.unistuttgart.isw.sfsc.adapter.BootstrapConfiguration;
import de.unistuttgart.isw.sfsc.commonjava.util.StoreEvent;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XReadReply;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XReadRequest;
import servicepatterns.api.SfscClient;
import servicepatterns.api.SfscServiceApi;
import servicepatterns.api.SfscServiceApiFactory;
import servicepatterns.api.filtering.Filters;
import servicepatterns.api.tagging.Tagger;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class PLC4XConsumer {
    static BootstrapConfiguration bootstrapConfiguration1 = new BootstrapConfiguration("127.0.0.1", 1251);
    static ByteString uuid = ByteString.copyFromUtf8(UUID.randomUUID().toString());

    public static void main(String[] args) {
        try {
            SfscServiceApi clientSfscServiceApi = SfscServiceApiFactory.getSfscServiceApi(bootstrapConfiguration1);

            Set<Map<String, ByteString>> exampleServiceTags = clientSfscServiceApi.getServices("isw.sfsc.PLC4XRead");

            try {
                CountDownLatch cdl = new CountDownLatch(1);
                SfscServiceApi serverSfscServiceApi = SfscServiceApiFactory.getSfscServiceApi(bootstrapConfiguration1);
                serverSfscServiceApi.addRegistryStoreEventListener(
                        event -> {
                            if (event.getStoreEventType() == StoreEvent.StoreEventType.CREATE
                                    && Tagger.getName(event.getData()).equals("Bool")
                                    && Filters.byteStringEqualsFilter("id", ByteString.copyFromUtf8("Some sample ID")).test(event.getData())) { //todo not sure yet how is best
                                System.out.println("matching service found");
                                cdl.countDown();
                            }
                        }
                );
                cdl.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }

            SfscClient client = clientSfscServiceApi.client();
            for ( Map<String, ByteString> tags: exampleServiceTags) {
                PLC4XReadRequest readRequest = PLC4XReadRequest.newBuilder()
                        .setConnectionString("opcua:tcp://127.0.0.1:12686/milo?discovery=false")
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



