package de.unistuttgart.isw.sfsc.mvp;

import com.google.protobuf.ByteString;
import com.google.protobuf.StringValue;
import de.unistuttgart.isw.sfsc.adapter.configuration.AdapterConfiguration;
import de.unistuttgart.isw.sfsc.framework.api.SfscChannelFactoryParameter;
import de.unistuttgart.isw.sfsc.framework.api.SfscPublisher;
import de.unistuttgart.isw.sfsc.framework.api.SfscPublisherParameter;
import de.unistuttgart.isw.sfsc.framework.api.SfscServer;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApi;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApiFactory;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class ChannelFactory {

  public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
    AdapterConfiguration config = new AdapterConfiguration().setCoreHost("127.0.0.1").setCorePubTcpPort(1251);
    new ChannelFactory(config).start();
  }

  AdapterConfiguration config;

  public ChannelFactory(AdapterConfiguration config) {
    this.config = config;
  }

  public void start() throws InterruptedException, ExecutionException, TimeoutException {
    SfscServiceApi serverApi = SfscServiceApiFactory.getSfscServiceApi(config);
    FactoryFunction factoryFunction = new FactoryFunction(serverApi);
    SfscServer channelFactory = serverApi
        .channelFactory(new SfscChannelFactoryParameter().setServiceName("channelfactory"),
            factoryFunction);
    System.out.println("registered service with input topic " + channelFactory.getDescriptor().getChannelFactoryTags().getInputTopic()
        .toStringUtf8());
  }

  static class FactoryFunction implements Function<ByteString, SfscPublisher> {

    private final SfscServiceApi sfscServiceApi;

    FactoryFunction(SfscServiceApi sfscServiceApi) {
      this.sfscServiceApi = sfscServiceApi;
    }

    @Override
    public SfscPublisher apply(ByteString sfscMessage) {
      System.out.println("received request");
      SfscPublisherParameter params = new SfscPublisherParameter()
          .setUnregistered(true);
      SfscPublisher publisher = sfscServiceApi.publisher(params);
      publisher.onSubscription(() -> {
        publisher.publish(StringValue.of("data"));
        System.out.println("published message");
      });
      return publisher;
    }
  }
}
