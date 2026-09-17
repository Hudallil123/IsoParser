package com.example.iso8583;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Iso8583TcpServer {

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 100;
    private static final long KEEP_ALIVE_SECONDS = 60;
    private static final int SOCKET_READ_TIMEOUT_MS = 30000;

    private final int port;
    private final Iso8583Parser parser;
    private final Iso8583Builder builder;
    private final ExecutorService executorService;
    private final int socketReadTimeoutMs;

    private volatile boolean running;
    private ServerSocket serverSocket;

    public Iso8583TcpServer(int port, Iso8583Parser parser, Iso8583Builder builder) {
        this(port, parser, builder, SOCKET_READ_TIMEOUT_MS);
    }

    public Iso8583TcpServer(int port, Iso8583Parser parser, Iso8583Builder builder, int socketReadTimeoutMs) {
        this.port = port;
        this.parser = parser;
        this.builder = builder;
        this.socketReadTimeoutMs = socketReadTimeoutMs;

        this.executorService = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public void start() throws IOException {

        if (running) {
            throw new IllegalStateException("Server sudah berjalan");
        }

        serverSocket = new ServerSocket(port);
        running = true;

        System.out.println("================================");
        System.out.println("ISO8583 TCP SERVER");
        System.out.println("================================");
        System.out.println("Server started");
        System.out.println("Listening on port: " + port);

        try {

            while (running) {

                Socket socket;

                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {

                    if (!running) {
                        break;
                    }

                    throw e;
                }

                try {

//                    socket.setSoTimeout(SOCKET_READ_TIMEOUT_MS);
                    socket.setSoTimeout(socketReadTimeoutMs);

                    System.out.println("Client connected: " + socket.getRemoteSocketAddress());

                    executorService.submit(() -> handleClient(socket));

                } catch (RejectedExecutionException e) {

                    System.out.println("Client rejected because server is overloaded");

                    try {
                        socket.close();
                    } catch (IOException closeException) {
                        System.out.println("Failed to close rejected client: " + closeException.getMessage());
                    }
                }
            }

        } finally {

            shutdown();
        }
    }

    private void handleClient(Socket socket) {

        try (socket) {

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            System.out.println("Client assigned to worker: " + Thread.currentThread().getName());

            while (running && !socket.isClosed()) {

                byte[] isoMessageBytes;

                try {

                    isoMessageBytes = readMessage(inputStream);

                } catch (SocketTimeoutException e) {

                    System.out.println("Client read timeout: " + socket.getRemoteSocketAddress());
                    break;

                } catch (IOException e) {

                    System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
                    break;
                }

                String isoMessage = new String(isoMessageBytes, StandardCharsets.US_ASCII);

                System.out.println("================================");
                System.out.println("ISO8583 MESSAGE RECEIVED");
                System.out.println("================================");
                System.out.println("Thread         : " + Thread.currentThread().getName());
                System.out.println("Client         : " + socket.getRemoteSocketAddress());
                System.out.println("Message Length : " + isoMessageBytes.length);
                System.out.println("Message        : " + isoMessage);

                try {

                    IsoMessage request = parser.parse(isoMessage);

                    printRequest(request);

                    Iso8583Mti mti = Iso8583Mti.parse(request.getMti());
                    Iso8583MtiValidator.validateRequest(mti);

                    IsoMessage response = createResponse(request);

                    String responseIso = builder.build(response);

                    byte[] responseBytes = Iso8583Encoder.encodeAscii(responseIso);

                    byte[] framedResponse = Iso8583Framer.addLengthHeader(responseBytes);

                    outputStream.write(framedResponse);
                    outputStream.flush();

                    System.out.println("Response sent");
                    System.out.println("Response: " + responseIso);

                } catch (Iso8583ParseException e) {

                    printError(e);
                }
            }

        } catch (IOException e) {

            System.out.println("TCP error: " + e.getMessage());
        }
    }

    private byte[] readMessage(InputStream inputStream) throws IOException {

        byte[] header = readFully(inputStream, 4);

        int messageLength = Iso8583Framer.parseLengthHeader(header);

        System.out.println("Length header received: " + new String(header, StandardCharsets.US_ASCII));
        System.out.println("Expected message length: " + messageLength);

        return readFully(inputStream, messageLength);
    }

    private byte[] readFully(InputStream inputStream, int length) throws IOException {

        byte[] data = new byte[length];

        int totalRead = 0;

        while (totalRead < length) {

            int bytesRead = inputStream.read(data, totalRead, length - totalRead);

            if (bytesRead == -1) {
                throw new IOException("Connection closed before message completed");
            }

            totalRead += bytesRead;
        }

        return data;
    }

    private IsoMessage createResponse(IsoMessage request) {

        IsoMessage response = new IsoMessage();

        response.setMti("0210");
        response.setField(2, request.getField(2));
        response.setField(3, request.getField(3));
        response.setField(4, request.getField(4));
        response.setField(11, request.getField(11));
        response.setField(41, request.getField(41));

        return response;
    }

    private void printRequest(IsoMessage request) {

        System.out.println("================================");
        System.out.println("ISO8583 REQUEST");
        System.out.println("================================");
        System.out.println("MTI   : " + request.getMti());
        System.out.println("DE 2  : " + request.getField(2));
        System.out.println("DE 3  : " + request.getField(3));
        System.out.println("DE 4  : " + request.getField(4));
        System.out.println("DE 11 : " + request.getField(11));
        System.out.println("DE 41 : " + request.getField(41));
        System.out.println("================================");
    }

    private void printError(Iso8583ParseException e) {

        System.out.println("================================");
        System.out.println("ISO8583 ERROR");
        System.out.println("================================");
        System.out.println("Error Code : " + e.getErrorCode());
        System.out.println("Message    : " + e.getMessage());
        System.out.println("Field      : " + e.getFieldNumber());
        System.out.println("Position   : " + e.getPosition());
        System.out.println("Value      : " + e.getValue());
        System.out.println("================================");
    }

    public void shutdown() {

        if (!running) {
            return;
        }

        System.out.println("================================");
        System.out.println("SHUTTING DOWN ISO8583 SERVER");
        System.out.println("================================");

        running = false;

        if (serverSocket != null && !serverSocket.isClosed()) {

            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Failed to close server socket: " + e.getMessage());
            }
        }

        executorService.shutdown();

        try {

            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {

                System.out.println("Forcing executor shutdown");

                executorService.shutdownNow();
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }

        System.out.println("ISO8583 TCP Server stopped");
        System.out.println("================================");
    }
}