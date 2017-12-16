package ch.bfh.ti.gapa.test;

import ch.bfh.ti.gapa.integration.model.GapaMessage;
import ch.bfh.ti.gapa.integration.server.converter.GapaMessageToJsonConverter;
import ch.bfh.ti.gapa.integration.server.converter.JsonSender;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

/**
 * This server mock is used for application tests. It mocks a server capable of websocket connections.
 * The server redirects received requests to the connected websocket client.
 */
public class ServerMock {
    private static final Logger LOGGER = Logger.getLogger(ServerMock.class.getName());

    private HttpServer httpServer;
    private AtomicReference<ServerWebSocket> webSocketClient = new AtomicReference<>();
    private GapaMessageToJsonConverter gapaMessageToJsonConverter;

    ServerMock() {
        //Create Vertx instance
        Vertx vertx = Vertx.vertx();

        //create http server with vertx
        httpServer = vertx.createHttpServer();

        //Add websocket handler.
        //We only support one websocket client for now
        httpServer.websocketHandler(event -> {
            LOGGER.info("A WebsocketClient has connected.");
            webSocketClient.set(event);
        });

        //The classes JsonSender, GapaMessageToJsonConverter and GapaMessage come
        //from the gapa-integration module which this server depends on.

        //The jsonSender takes a json object and sends it over the websocket connection
        JsonSender jsonSender = jsonObject -> webSocketClient.get().writeFinalTextFrame(jsonObject.toString());

        //the converter takes a gapa message, converts it to JSON and passes it to the jsonSender
        gapaMessageToJsonConverter = new GapaMessageToJsonConverter(jsonSender);

        //start http server at a short lived port (vertx chooses one itself)
        httpServer.listen(0);
        waitForPort();
    }

    /**
     * Sends given gapa messages to the websocket client.
     * This messages would be generated by the server on incoming requests.
     * For the tests, we just prepare gapa messages and send them to the websocket client
     * which is a gapa instance.
     * @param gapaMessages gapa message samples
     */
    public void sendGapaMessages(List<GapaMessage> gapaMessages) {
        for(GapaMessage gapaMessage: gapaMessages) {
            //converts the message to json which is sent over the wire
            gapaMessageToJsonConverter.sendGapaMessage(gapaMessage);
        }

        //TODO Output indicator of received messages to stderr so it can be checked in test
        //Wait until gapa messages are received by client
        //Testing in an asynchronous environment is difficult.
        //40ms is an approximation. Gapa has no capabilities to tell
        //how many messages were already received. We can't output
        //this information in StdOut because this channel is reserved for
        //PlantUml output.
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return a random chosen port
     */
    public int getPort() {
        return httpServer.actualPort();
    }

    private void waitFor(BooleanSupplier booleanSupplier) throws TimeoutException {
        int maxWait = 3000;
        int count = 0;
        while(true) {
            if(count > maxWait) {
                throw new TimeoutException("Timeout reached.");
            } else {
                try {
                    if(booleanSupplier.getAsBoolean()) {
                        break;
                    } else {
                        //Wait a millisecond between checks
                        Thread.sleep(1);
                        count++;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void waitForPort() {
        try {
            waitFor(()-> getPort() != 0);
            //Random port number was set. We break out of the loop.
            LOGGER.info("Server port: " + getPort());
        } catch (TimeoutException e) {
            throw new RuntimeException("Could not get random port number");
        }
    }

    public void waitForConnection() {
        try {
            waitFor(()-> webSocketClient.get() != null);
        } catch (TimeoutException e) {
            throw new RuntimeException("No websocket client connected in time.");
        }
    }

    /**
     * Convenience method to start the server mock separately.
     */
    public static void main(String[] args) {
        new ServerMock();
    }
}
