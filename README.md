# Welcome to the PLC4X integration service for SFSC

The service implemented here will include the READ, WRITE and CONTINIUS-READ/SUBSCRIBE functions of machine controllers via their automation interface using the Apache project PLC4X. These services are implemented as SFSC services that require an SFSC core module to run as a local or remote broker. 

>Caution, the services presented here are under development and will change in structure and behaviour.

# Configuration
The services are configured via local environment variables that are displayed in the docker file. In the following the possible configuration values with their data type and default value are presented.
|Variable          |Type                 |Default  |Description                            |
|------------------|---------------------|---------|---------------------------------------|
|EXTERNEL_CORE_IP  |Hostname / IP-Address|127.0.0.1|The TCP IP or hostname of the SFSC core|
|EXTERNEL_CORE_PORT|TCP-Port: 1 - 65535  |1251     |THE TCP port of the SFSC core          |





# Services
Here we briefly introduce the three different services READ, WRITE and CONTINIUS-READ/SUBSCRIBE, which are to be implemented in this repository. Each of these services will be included in the Docker image. By default, the write service will be disabled.
## Monitoring over PLC4X scrapper api
Name of the Service: `de.universitystuttgart.isw.sfsc.plc4x.monitoring.scrapper`
### Example
```java
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
                    .putVariables("TestVar", "ns=2;s=Dynamic/RandomInt64")
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

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}

```
## Read plc values over PLC4X
Name of the Service: `de.universitystuttgart.isw.sfsc.plc4x.read`
### Example
```java
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
                        .setConnectionString("opcua:tcp://milo.digitalpetri.com:62541/milo")
                        .setVariableAdress("ns=2;s=Dynamic/RandomInt64")
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
```
## Write plc values over PLC4X
Not implemented yet
