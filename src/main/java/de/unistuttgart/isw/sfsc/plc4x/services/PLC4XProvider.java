package de.unistuttgart.isw.sfsc.plc4x.services;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import de.unistuttgart.isw.sfsc.adapter.configuration.AdapterConfiguration;
import de.unistuttgart.isw.sfsc.config.Constants;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XReadReply;
import de.unistuttgart.isw.sfsc.example.services.messages.PLC4XReadRequest;
import de.unistuttgart.isw.sfsc.framework.api.SfscServer;
import de.unistuttgart.isw.sfsc.framework.api.SfscServerParameter;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApi;
import de.unistuttgart.isw.sfsc.framework.api.SfscServiceApiFactory;
import de.unistuttgart.isw.sfsc.framework.descriptor.SfscServiceDescriptor.ServerTags.RegexDefinition;
import de.unistuttgart.isw.sfsc.framework.patterns.ackreqrep.AckServerResult;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.opcua.connection.OpcuaTcpPlcConnection;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class PLC4XProvider {


    public static void main(String[] args) {
        AdapterConfiguration adapterConfiguration1 = new AdapterConfiguration().setCoreHost(Constants.CORE_ADDRESS).setCorePubTcpPort(Constants.CORE_PORT);
        try {
            SfscServiceApi serverSfscServiceApi = SfscServiceApiFactory.getSfscServiceApi(adapterConfiguration1);
            SfscServer server = serverSfscServiceApi.server(new SfscServerParameter().setServiceName("de.universitystuttgart.isw.sfsc.plc4x.read")
                .setInputMessageType(ByteString.copyFromUtf8("de.universitystuttgart.isw.sfsc.PLC4XReadRequest"))
                 .setOutputMessageType(ByteString.copyFromUtf8("de.universitystuttgart.isw.sfsc.PLC4XReadReply"))
                    .setRegexDefinition(
                    RegexDefinition.newBuilder().build())
                            .setCustomTags(Map.of("plc4x-service-type", ByteString.copyFromUtf8("opc")))
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


    static Function<ByteString, AckServerResult> replyFunction() {
        return requestByteString -> {
            String valueResult = "";
            String statusResult = "";
                try {
                    PLC4XReadRequest request = PLC4XReadRequest.parseFrom(requestByteString);
                    // Do cool Stuff with the request

                    OpcuaTcpPlcConnection opcuaConnection = null;
                    opcuaConnection = (OpcuaTcpPlcConnection)
                            new PlcDriverManager().getConnection(request.getConnectionString());


                PlcReadRequest.Builder builder = opcuaConnection.readRequestBuilder();
                builder.addItem("LonlyVar", request.getVariableAdress());
                PlcReadRequest requestRead = builder.build();


                PlcReadResponse response = opcuaConnection.read(requestRead).get();
                statusResult = response.getResponseCode("LonlyVar").toString();
                if(response.getResponseCode("LonlyVar").equals(PlcResponseCode.OK)){
                    valueResult = response.getString("LonlyVar");
                }
            } catch (InterruptedException e) {
                statusResult = "BAD";
            } catch (ExecutionException e) {
                statusResult = "BAD";
            } catch (InvalidProtocolBufferException e) {
                    statusResult = "BAD";
                } catch (PlcConnectionException e) {
                    statusResult = "BAD";
                }

            // reply Answer
                PLC4XReadReply reply = PLC4XReadReply.newBuilder()
                        .setStatus(statusResult)
                        .setValue(valueResult).build();
                return serverResult(reply);

        };
    }

    static AckServerResult serverResult(Message response) {
        return new AckServerResult(
                response,
                () -> System.out.println("plc4x server acknowledge succeeded"),
                () -> System.out.println("plc4x server acknowledge didnt succeed")
        );

    }




}
