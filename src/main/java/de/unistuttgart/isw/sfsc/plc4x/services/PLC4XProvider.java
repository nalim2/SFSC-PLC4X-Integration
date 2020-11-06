package de.unistuttgart.isw.sfsc.plc4x.services;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import de.unistuttgart.isw.sfsc.adapter.configuration.AdapterConfiguration;
import de.unistuttgart.isw.sfsc.config.Constants;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XReadReply;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XReadRequest;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApi;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApiFactory;
import de.unistuttgart.isw.sfsc.framework.api.services.clientserver.SfscServer;
import de.unistuttgart.isw.sfsc.framework.api.services.clientserver.SfscServerParameter;
import de.unistuttgart.isw.sfsc.framework.descriptor.SfscServiceDescriptor.ServerTags.RegexDefinition;
import de.unistuttgart.isw.sfsc.framework.patterns.ackreqrep.AckServerResult;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class PLC4XProvider {
    private ConcurrentMap<String, PlcConnection> availableConnections = new ConcurrentHashMap<>();
    private  PlcDriverManager driverManager = new PlcDriverManager();
    static Logger log = LoggerFactory.getLogger(PLC4XProvider.class);
    public static void main(String[] args) {

        PLC4XProvider service = new PLC4XProvider();
        service.start();
    }

    public void start(){
        AdapterConfiguration adapterConfiguration1 = new AdapterConfiguration().setCoreHost(Constants.CORE_ADDRESS).setCorePubTcpPort(Constants.CORE_PORT);

        adapterConfiguration1.setCoreHost("prj-sfsc03.isw.uni-stuttgart.de");
        try {
            SfscServiceApi serverSfscServiceApi = SfscServiceApiFactory.getSfscServiceApi(adapterConfiguration1);
            SfscServer server = serverSfscServiceApi.server(new SfscServerParameter().setServiceName("de.universitystuttgart.isw.sfsc.plc4x.read")
                            .setInputMessageType(ByteString.copyFromUtf8("de.universitystuttgart.isw.sfsc.PLC4XReadRequest"))
                            .setOutputMessageType(ByteString.copyFromUtf8("de.universitystuttgart.isw.sfsc.PLC4XReadReply"))
                            .setRegexDefinition(
                                    RegexDefinition.newBuilder().build())
                    ,
                    replyFunction() // Hier wird die Reply Funktion hineingegeben
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private PlcConnection getConnection(String connectionString) throws PlcConnectionException {
        PlcConnection result = availableConnections.getOrDefault(connectionString, null);
        if(result == null){
            result = initateNewConnection(connectionString);
        }
        return result;
    }

    private synchronized PlcConnection initateNewConnection(String connectionString) throws PlcConnectionException {
        PlcConnection result = availableConnections.getOrDefault(connectionString, null);
        if(result == null){
            PlcConnection connection = driverManager.getConnection(connectionString);
            PlcConnection previousValue = availableConnections.putIfAbsent(connectionString, connection);
            // TODO: Include handling of already existing connections inside the map if they are possible?
            result = connection;
        }
        return result;
    }

    private synchronized void closeConnection(String connectionString)
    {
        PlcConnection result = availableConnections.remove(connectionString);
        if(result != null){
            try {
                result.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    Function<ByteString, AckServerResult> replyFunction() {
        return requestByteString -> {
            System.out.println("Got request");
            String valueResult = "";
            String statusResult = "";
            String errorMessage = "";
            try {
                PLC4XReadRequest request = PLC4XReadRequest.parseFrom(requestByteString);
                // Do cool Stuff with the request

                PlcConnection opcuaConnection = getConnection(request.getConnectionString());



                PlcReadRequest.Builder builder = opcuaConnection.readRequestBuilder();
                builder.addItem("LonlyVar", request.getVariableAdress());
                PlcReadRequest requestRead = builder.build();


                PlcReadResponse response = requestRead.execute().get();
                statusResult = response.getResponseCode("LonlyVar").toString();
                if(response.getResponseCode("LonlyVar").equals(PlcResponseCode.OK)){
                    valueResult = response.getObject("LonlyVar").toString();
                }
            } catch (InterruptedException e) {
                statusResult = "BAD" + e.getMessage();

            } catch (ExecutionException e) {
                statusResult = "BAD"+ e.getMessage();
            } catch (InvalidProtocolBufferException e) {
                statusResult = "BAD"+ e.getMessage();
            } catch (PlcConnectionException e) {
                statusResult = "BAD"+ e.getMessage();
            }

            // reply Answer
            PLC4XReadReply reply = PLC4XReadReply.newBuilder()
                    .setStatus(statusResult)
                    .setValue(valueResult).build();
            return serverResult(reply);

        };
    }

    AckServerResult serverResult(Message response) {
        return new AckServerResult(
                response,
                () -> System.out.println("plc4x server acknowledge succeeded"),
                () -> System.out.println("plc4x server acknowledge didnt succeed")
        );

    }



}
