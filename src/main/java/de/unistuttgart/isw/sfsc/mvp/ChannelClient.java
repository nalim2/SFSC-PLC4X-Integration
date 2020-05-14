package de.unistuttgart.isw.sfsc.mvp;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import de.unistuttgart.isw.sfsc.adapter.configuration.AdapterConfiguration;
import de.unistuttgart.isw.sfsc.commonjava.util.StoreEvent;
import de.unistuttgart.isw.sfsc.framework.api.SfscClient;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApi;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApiFactory;
import de.unistuttgart.isw.sfsc.framework.api.SfscSubscriber;
import de.unistuttgart.isw.sfsc.framework.descriptor.SfscServiceDescriptor;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChannelClient {

  public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
    AdapterConfiguration config = new AdapterConfiguration().setCoreHost("127.0.0.1").setCorePubTcpPort(1251);
    new ChannelClient(config).start();
  }

  AdapterConfiguration config;

  public ChannelClient(AdapterConfiguration config) {
    this.config = config;
  }

  public void start() throws InterruptedException, ExecutionException, TimeoutException {
    AdapterConfiguration clientConfig = new AdapterConfiguration().setCoreHost("127.0.0.1").setCorePubTcpPort(1251);
    SfscServiceApi clientApi = SfscServiceApiFactory.getSfscServiceApi(clientConfig);
    clientApi.addOneShotRegistryStoreEventListener(
        event -> event.getStoreEventType() == StoreEvent.StoreEventType.CREATE
            && Objects.equals(event.getData().getServiceName(), "channelfactory")
    ).await(10, TimeUnit.SECONDS);
    SfscServiceDescriptor serverTags = clientApi.getServices("channelfactory").stream()
        .findAny().orElseThrow();

    SfscClient client2 = clientApi.client();
    System.out.println("sending request to topic " + serverTags.getChannelFactoryTags().getInputTopic().toStringUtf8());
    SfscSubscriber sfscSubscriber = client2.requestChannel(
        serverTags,
        ByteString.EMPTY,
        5000,
        message -> {
          try {
            String data = StringValue.parseFrom(message).getValue();
            System.out.println("Received message on channel factory subscriber with content: " + data);
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        }
    ).get();

  }
}
