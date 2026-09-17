package com.example.iso8583;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Iso8583TcpClient {

    private final String host;
    private final int port;
    private final Iso8583Builder builder;
    private final Iso8583Parser parser;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public Iso8583TcpClient(
            String host,
            int port,
            Iso8583Builder builder,
            Iso8583Parser parser
    ) {
        this.host = host;
        this.port = port;
        this.builder = builder;
        this.parser = parser;
    }

    public void connect() throws IOException {

        socket = new Socket(host, port);

        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();

        System.out.println("================================");
        System.out.println("ISO8583 TCP CLIENT");
        System.out.println("================================");
        System.out.println("Connected to: " + host + ":" + port);
    }

    public IsoMessage send(IsoMessage request) throws IOException {

        if (socket == null || socket.isClosed()) {
            throw new IOException("Client belum terhubung ke server");
        }

        String isoMessage = builder.build(request);

        byte[] encodedMessage = Iso8583Encoder.encodeAscii(isoMessage);

        byte[] framedMessage = Iso8583Framer.addLengthHeader(encodedMessage);

        System.out.println("================================");
        System.out.println("SEND ISO8583 REQUEST");
        System.out.println("================================");
        System.out.println("Request ISO8583 : " + isoMessage);
        System.out.println("Message Length  : " + encodedMessage.length);
        System.out.println("Framed Length   : " + framedMessage.length);

        outputStream.write(framedMessage);
        outputStream.flush();

        System.out.println("Request sent");

        byte[] responseBytes = readMessage();

        String responseIso = new String(
                responseBytes,
                StandardCharsets.US_ASCII
        );

        System.out.println("Response received");
        System.out.println("Response ISO8583 : " + responseIso);

        return parser.parse(responseIso);
    }

    public void close() throws IOException {

        if (socket != null && !socket.isClosed()) {

            socket.close();

            System.out.println("Client connection closed");
        }
    }

    private byte[] readMessage() throws IOException {

        byte[] header = readFully(4);

        int messageLength = Iso8583Framer.parseLengthHeader(header);

        System.out.println("Response length: " + messageLength);

        return readFully(messageLength);
    }

    private byte[] readFully(int length) throws IOException {

        byte[] data = new byte[length];

        int totalRead = 0;

        while (totalRead < length) {

            int bytesRead = inputStream.read(
                    data,
                    totalRead,
                    length - totalRead
            );

            if (bytesRead == -1) {
                throw new IOException(
                        "Connection closed before response completed"
                );
            }

            totalRead += bytesRead;
        }

        return data;
    }
}