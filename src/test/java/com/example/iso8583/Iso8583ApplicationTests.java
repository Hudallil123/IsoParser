package com.example.iso8583;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class Iso8583ApplicationTests {

	private static final int TEST_PORT = 19000;
	private static final int TEST_SOCKET_TIMEOUT_MS = 2000;

	private Iso8583Parser parser;
	private Iso8583Builder builder;

	private Iso8583TcpServer server;
	private Thread serverThread;

//	@BeforeEach
//	void setUp() {
//
//		Map<Integer, IsoFieldDefinition> definitions =
//				Map.of(
//						2, new IsoFieldDefinition(
//								2,
//								FieldType.LLVAR,
//								19,
//								FieldDataType.NUMERIC,
//								true
//						),
//						3, new IsoFieldDefinition(
//								3,
//								FieldType.FIXED,
//								6,
//								FieldDataType.NUMERIC,
//								true
//						),
//						4, new IsoFieldDefinition(
//								4,
//								FieldType.FIXED,
//								12,
//								FieldDataType.NUMERIC,
//								true
//						),
//						11, new IsoFieldDefinition(
//								11,
//								FieldType.FIXED,
//								6,
//								FieldDataType.NUMERIC,
//								true
//						),
//						41, new IsoFieldDefinition(
//								41,
//								FieldType.FIXED,
//								6,
//								FieldDataType.ALPHA,
//								false
//						),
//						48, new IsoFieldDefinition(
//								48,
//								FieldType.LLLVAR,
//								25,
//								FieldDataType.ALPHA,
//								true
//						)
//				);
//
//		parser = new Iso8583Parser(definitions);
//	}

	@BeforeEach
	void setUp() throws Exception {

		Map<Integer, IsoFieldDefinition> definitions = Map.of(
				2,
				new IsoFieldDefinition(
						2,
						FieldType.LLVAR,
						19,
						FieldDataType.NUMERIC,
						true
				),
				3,
				new IsoFieldDefinition(
						3,
						FieldType.FIXED,
						6,
						FieldDataType.NUMERIC,
						true
				),
				4,
				new IsoFieldDefinition(
						4,
						FieldType.FIXED,
						12,
						FieldDataType.NUMERIC,
						true
				),
				11,
				new IsoFieldDefinition(
						11,
						FieldType.FIXED,
						6,
						FieldDataType.NUMERIC,
						true
				),
				41,
				new IsoFieldDefinition(
						41,
						FieldType.FIXED,
						6,
						FieldDataType.ALPHA,
						false
				)
		);

		parser = new Iso8583Parser(definitions);
		builder = new Iso8583Builder(definitions);

		server = new Iso8583TcpServer(
				TEST_PORT,
				parser,
				builder,
				TEST_SOCKET_TIMEOUT_MS
		);

		serverThread = new Thread(() -> {
			try {
				server.start();
			} catch (IOException e) {
				if (serverThread != null && serverThread.isAlive()) {
					System.out.println("Test server error: " + e.getMessage());
				}
			}
		}, "iso8583-test-server");

		serverThread.start();

		waitForServer();
	}

	@AfterEach
	void tearDown() throws Exception {

		if (server != null) {
			server.shutdown();
		}

		if (serverThread != null) {
			serverThread.join(5000);
		}
	}

	// =====================================================
	// 1. SERVER START
	// =====================================================

	@Test
	void shouldStartTcpServer() {

		assertTrue(serverThread.isAlive());
	}

	// =====================================================
	// 2. TCP CONNECTION
	// =====================================================

	@Test
	void shouldAcceptTcpConnection() throws Exception {

		try (Socket socket = new Socket("localhost", TEST_PORT)) {

			assertTrue(socket.isConnected());
			assertFalse(socket.isClosed());
		}
	}

	// =====================================================
	// 3. SINGLE REQUEST RESPONSE
	// =====================================================

	@Test
	void shouldSendAndReceiveIso8583Message() throws Exception {

		try (Socket socket = new Socket("localhost", TEST_PORT)) {

			IsoMessage request = createRequest("000001");

			String requestIso = builder.build(request);

			sendMessage(socket, requestIso);

			String responseIso = readMessage(socket);

			IsoMessage response = parser.parse(responseIso);

			assertEquals("0210", response.getMti());
			assertEquals("6212345678901234", response.getField(2));
			assertEquals("123000", response.getField(3));
			assertEquals("000000000010", response.getField(4));
			assertEquals("000001", response.getField(11));
			assertEquals("ATMABC", response.getField(41));
		}
	}

	// =====================================================
	// 4. LENGTH HEADER
	// =====================================================

	@Test
	void shouldUseCorrectLengthHeader() throws Exception {

		try (Socket socket = new Socket("localhost", TEST_PORT)) {

			IsoMessage request = createRequest("000002");

			String requestIso = builder.build(request);

			byte[] messageBytes = Iso8583Encoder.encodeAscii(requestIso);
			byte[] framedMessage = Iso8583Framer.addLengthHeader(messageBytes);

			OutputStream outputStream = socket.getOutputStream();

			outputStream.write(framedMessage);
			outputStream.flush();

			InputStream inputStream = socket.getInputStream();

			byte[] header = readFully(inputStream, 4);

			assertEquals(
					"0068",
					new String(header, StandardCharsets.US_ASCII)
			);

			int responseLength = Iso8583Framer.parseLengthHeader(header);

			assertEquals(68, responseLength);

			byte[] responseBytes = readFully(inputStream, responseLength);

			assertEquals(68, responseBytes.length);
		}
	}

	// =====================================================
	// 5. PERSISTENT CONNECTION
	// =====================================================

	@Test
	void shouldHandleMultipleMessagesOnSameConnection() throws Exception {

		try (Socket socket = new Socket("localhost", TEST_PORT)) {

			for (int i = 1; i <= 3; i++) {

				String stan = String.format("%06d", i);

				IsoMessage request = createRequest(stan);

				String requestIso = builder.build(request);

				sendMessage(socket, requestIso);

				String responseIso = readMessage(socket);

				IsoMessage response = parser.parse(responseIso);

				assertEquals("0210", response.getMti());
				assertEquals(stan, response.getField(11));
			}
		}
	}

	// =====================================================
	// 6. REQUEST / RESPONSE SEQUENCE
	// =====================================================

	@Test
	void shouldProcessRequestAndResponseSequentially() throws Exception {

		try (Socket socket = new Socket("localhost", TEST_PORT)) {

			IsoMessage request1 = createRequest("000101");

			sendMessage(
					socket,
					builder.build(request1)
			);

			IsoMessage response1 = parser.parse(
					readMessage(socket)
			);

			assertEquals("0210", response1.getMti());
			assertEquals("000101", response1.getField(11));

			IsoMessage request2 = createRequest("000102");

			sendMessage(
					socket,
					builder.build(request2)
			);

			IsoMessage response2 = parser.parse(
					readMessage(socket)
			);

			assertEquals("0210", response2.getMti());
			assertEquals("000102", response2.getField(11));
		}
	}

	// =====================================================
	// 7. CLIENT DISCONNECT
	// =====================================================

	@Test
	void shouldHandleClientDisconnect() throws Exception {

		Socket socket = new Socket("localhost", TEST_PORT);

		assertTrue(socket.isConnected());

		socket.close();

		TimeUnit.MILLISECONDS.sleep(200);

		assertTrue(serverThread.isAlive());
	}

	// =====================================================
	// 8. SOCKET READ TIMEOUT
	// =====================================================

	@Test
	void shouldCloseIdleClientAfterReadTimeout() throws Exception {

		try (Socket socket = new Socket("localhost", TEST_PORT)) {

			socket.setSoTimeout(5000);

			InputStream inputStream = socket.getInputStream();

			TimeUnit.MILLISECONDS.sleep(
					TEST_SOCKET_TIMEOUT_MS + 500
			);

			int result = inputStream.read();

			assertEquals(-1, result);
		}
	}

	// =====================================================
	// 9. SERVER SHUTDOWN
	// =====================================================

	@Test
	void shouldShutdownServerGracefully() throws Exception {

		assertTrue(serverThread.isAlive());

		server.shutdown();

		serverThread.join(5000);

		assertFalse(serverThread.isAlive());
	}

	// =====================================================
	// 10. SHUTDOWN SHOULD NOT THROW
	// =====================================================

	@Test
	void shouldAllowShutdownToBeCalledTwice() {

		assertDoesNotThrow(() -> {
			server.shutdown();
			server.shutdown();
		});
	}

	// =====================================================
	// 11. MESSAGE FRAMING ROUND TRIP
	// =====================================================

	@Test
	void shouldPreserveMessageAfterFraming() {

		IsoMessage request = createRequest("000201");

		String isoMessage = builder.build(request);

		byte[] messageBytes = Iso8583Encoder.encodeAscii(isoMessage);

		byte[] framedMessage = Iso8583Framer.addLengthHeader(messageBytes);

		byte[] extractedMessage = Iso8583Framer.extractMessage(framedMessage);

		String extractedIso = Iso8583Encoder.decodeAscii(extractedMessage);

		assertEquals(isoMessage, extractedIso);
		assertEquals(messageBytes.length, extractedMessage.length);
	}

	// =====================================================
	// 12. CONCURRENT CLIENT CONNECTION
	// =====================================================

	@Test
	void shouldHandleMultipleClientsConcurrently() throws Exception {

		int clientCount = 5;

		Thread[] clients = new Thread[clientCount];

		for (int i = 0; i < clientCount; i++) {

			final int clientNumber = i;

			clients[i] = new Thread(() -> {

				try (Socket socket = new Socket("localhost", TEST_PORT)) {

					String stan = String.format(
							"%06d",
							clientNumber + 100
					);

					IsoMessage request = createRequest(stan);

					sendMessage(
							socket,
							builder.build(request)
					);

					String responseIso = readMessage(socket);

					IsoMessage response = parser.parse(responseIso);

					assertEquals("0210", response.getMti());
					assertEquals(stan, response.getField(11));

				} catch (Exception e) {

					throw new RuntimeException(e);
				}

			}, "test-client-" + i);

			clients[i].start();
		}

		for (Thread client : clients) {
			client.join(5000);
		}

		for (Thread client : clients) {
			assertFalse(client.isAlive());
		}
	}

	// =====================================================
	// HELPER
	// =====================================================

	private IsoMessage createRequest(String stan) {

		IsoMessage request = new IsoMessage();

		request.setMti("0200");
		request.setField(2, "6212345678901234");
		request.setField(3, "123000");
		request.setField(4, "000000000010");
		request.setField(11, stan);
		request.setField(41, "ATMABC");

		return request;
	}

	private void sendMessage(
			Socket socket,
			String isoMessage
	) throws IOException {

		byte[] messageBytes =
				Iso8583Encoder.encodeAscii(isoMessage);

		byte[] framedMessage =
				Iso8583Framer.addLengthHeader(messageBytes);

		OutputStream outputStream =
				socket.getOutputStream();

		outputStream.write(framedMessage);
		outputStream.flush();
	}

	private String readMessage(
			Socket socket
	) throws IOException {

		InputStream inputStream =
				socket.getInputStream();

		byte[] header =
				readFully(inputStream, 4);

		int messageLength =
				Iso8583Framer.parseLengthHeader(header);

		byte[] message =
				readFully(inputStream, messageLength);

		return Iso8583Encoder.decodeAscii(message);
	}

	private byte[] readFully(
			InputStream inputStream,
			int length
	) throws IOException {

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
						"Connection closed before data completed"
				);
			}

			totalRead += bytesRead;
		}

		return data;
	}

	private void waitForServer() throws Exception {

		long timeout = System.currentTimeMillis() + 5000;

		while (System.currentTimeMillis() < timeout) {

			try (Socket socket = new Socket("localhost", TEST_PORT)) {
				return;
			} catch (IOException e) {
				TimeUnit.MILLISECONDS.sleep(100);
			}
		}

		fail("TCP server tidak berhasil start dalam 5 detik");
	}

	// =====================================================
	// 1. MESSAGE NULL
	// =====================================================

	@Test
	void shouldRejectNullMessage() {

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(null)
		);
	}


	// =====================================================
	// 2. MESSAGE KOSONG
	// =====================================================

	@Test
	void shouldRejectEmptyMessage() {

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse("")
		);
	}


	// =====================================================
	// 3. MTI INVALID
	// =====================================================

	@Test
	void shouldRejectInvalidMti() {

		String message =
				"XXXX" +
						"7020000000010000" +
						"16" +
						"6212345678901234" +
						"123000" +
						"000000000010" +
						"000123" +
						"025" +
						"ABCDEFGHIJRXCYVUYIBDASJKL";

		assertThrows(
				Exception.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// 4. BITMAP INVALID
	// =====================================================

	@Test
	void shouldRejectInvalidBitmap() {

		String message =
				"0200" +
						"ZZZZZZZZZZZZZZZZ" +
						"16" +
						"6212345678901234" +
						"123000" +
						"000000000010" +
						"000123" +
						"025" +
						"ABCDEFGHIJRXCYVUYIBDASJKL";

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// 5. SECONDARY BITMAP TIDAK LENGKAP
	// =====================================================

	@Test
	void shouldRejectIncompleteSecondaryBitmap() {

		String message =
				"0200" +
						"F020000000010000" +
						"1234";

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// 6. LLVAR LENGTH INDICATOR INVALID
	// =====================================================

	@Test
	void shouldRejectInvalidLlvarLengthIndicator() {

		String message =
				"0200" +
						"7020000000010000" +
						"XX" +
						"6212345678901234" +
						"123000" +
						"000000000010" +
						"000123" +
						"025" +
						"ABCDEFGHIJRXCYVUYIBDASJKL";

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// 7. LLVAR DATA KURANG
	// =====================================================

	@Test
	void shouldRejectInsufficientLlvarData() {

		String message =
				"0200" +
						"7020000000010000" +
						"19" +
						"6212345678901234" +
						"123000" +
						"000000000010" +
						"000123" +
						"025" +
						"ABCDEFGHIJRXCYVUYIBDASJKL";

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// 8. LLLVAR DATA KURANG
	// =====================================================

	@Test
	void shouldRejectInsufficientLllvarData() {

		String message =
				"0200" +
						"7020000000010000" +
						"16" +
						"6212345678901234" +
						"123000" +
						"000000000010" +
						"000123" +
						"025" +
						"ABCDEFGHIJ";

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// 9. DATA MELEBIHI MAX LENGTH
	// =====================================================

	@Test
	void shouldRejectDataExceedingMaxLength() {

		String message =
				"0200" +
						"7020000000010000" +
						"20" +
						"62123456789012345678" +
						"123000" +
						"000000000010" +
						"000123" +
						"025" +
						"ABCDEFGHIJRXCYVUYIBDASJKL";

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// 10. FIELD ALPHA BERISI ANGKA
	// =====================================================

	@Test
	void shouldRejectAlphaContainingNumber() {

		Map<Integer, IsoFieldDefinition> definitions =
				Map.of(
						2, new IsoFieldDefinition(
								2,
								FieldType.LLVAR,
								19,
								FieldDataType.NUMERIC,
								true
						),
						3, new IsoFieldDefinition(
								3,
								FieldType.FIXED,
								6,
								FieldDataType.NUMERIC,
								true
						),
						4, new IsoFieldDefinition(
								4,
								FieldType.FIXED,
								12,
								FieldDataType.NUMERIC,
								true
						),
						11, new IsoFieldDefinition(
								11,
								FieldType.FIXED,
								6,
								FieldDataType.NUMERIC,
								true
						),
						41, new IsoFieldDefinition(
								41,
								FieldType.FIXED,
								6,
								FieldDataType.ALPHA,
								true
						)
				);

		Iso8583Parser alphaParser =
				new Iso8583Parser(definitions);

		String message =
				"0200" +
						"7020000000800000" +
						"16" +
						"6212345678901234" +
						"123000" +
						"000000000010" +
						"000123" +
						"456ATM";

		assertThrows(
				Iso8583ParseException.class,
				() -> alphaParser.parse(message)
		);
	}


	// =====================================================
	// 11. FIELD NUMERIC BERISI HURUF
	// =====================================================

	@Test
	void shouldRejectNumericContainingLetter() {

		String message =
				"0200" +
						"7020000000010000" +
						"16" +
						"6212345678901234" +
						"ABC123" +
						"000000000010" +
						"000123" +
						"025" +
						"ABCDEFGHIJRXCYVUYIBDASJKL";

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// 12. FIELD WAJIB TIDAK ADA
	// =====================================================

	@Test
	void shouldRejectMissingRequiredField() {

		/*
		 * DE 11 wajib,
		 * tetapi bitmap tidak mengaktifkan DE 11.
		 */

		String message =
				"0200" +
						"7020000000000000" +
						"16" +
						"6212345678901234" +
						"123000" +
						"000000000010" +
						"025" +
						"ABCDEFGHIJRXCYVUYIBDASJKL";

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// 13. BITMAP FIELD ADA,
	//     DEFINITION TIDAK ADA
	// =====================================================

	@Test
	void shouldRejectMissingFieldDefinition() {

		Map<Integer, IsoFieldDefinition> definitions =
				Map.of(
						2, new IsoFieldDefinition(
								2,
								FieldType.LLVAR,
								19,
								FieldDataType.NUMERIC,
								true
						),
						3, new IsoFieldDefinition(
								3,
								FieldType.FIXED,
								6,
								FieldDataType.NUMERIC,
								true
						),
						4, new IsoFieldDefinition(
								4,
								FieldType.FIXED,
								12,
								FieldDataType.NUMERIC,
								true
						),
						11, new IsoFieldDefinition(
								11,
								FieldType.FIXED,
								6,
								FieldDataType.NUMERIC,
								true
						)
				);

		Iso8583Parser no48Parser =
				new Iso8583Parser(definitions);

		String message = validMessage();

		assertThrows(
				Iso8583ParseException.class,
				() -> no48Parser.parse(message)
		);
	}


	// =====================================================
	// 14. DATA TERSISA
	// =====================================================

	@Test
	void shouldRejectRemainingData() {

		String message =
				validMessage() +
						"EXTRA";

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// 15. POSITION MELEWATI MESSAGE
	// =====================================================

	@Test
	void shouldRejectPositionExceedingMessageLength() {

		String message =
				"0200" +
						"7020000000010000" +
						"16" +
						"6212345678901234" +
						"123000";

		assertThrows(
				Iso8583ParseException.class,
				() -> parser.parse(message)
		);
	}


	// =====================================================
	// VALID MESSAGE
	// =====================================================

	private String validMessage() {

		return
				"0200" +
						"7020000000010000" +
						"16" +
						"6212345678901234" +
						"123000" +
						"000000000010" +
						"000123" +
						"025" +
						"ABCDEFGHIJRXCYVUYIBDASJKL";
	}

	// =========================================================
	// HEX TO BINARY
	// =========================================================

	@Test
	void hexToBinary_shouldReturn64Bits() {

		String bitmap =
				"7020000000800000";

		String binary =
				BitmapUtil.hexToBinary(bitmap);

		assertEquals(
				64,
				binary.length()
		);
	}

	@Test
	void hexToBinary_shouldConvertCorrectly() {

		String bitmap =
				"7020000000800000";

		String binary =
				BitmapUtil.hexToBinary(bitmap);

		assertEquals(
				"0111000000100000000000000000000000000000100000000000000000000000",
				binary
		);
	}

	// =========================================================
	// FIELD PRESENT
	// =========================================================

	@Test
	void isFieldPresent_shouldReturnTrue_whenFieldExists() {

		String bitmap =
				"7020000000800000";

		String binary =
				BitmapUtil.hexToBinary(bitmap);

		assertTrue(
				BitmapUtil.isFieldPresent(
						binary,
						2
				)
		);

		assertTrue(
				BitmapUtil.isFieldPresent(
						binary,
						3
				)
		);

		assertTrue(
				BitmapUtil.isFieldPresent(
						binary,
						4
				)
		);

		assertTrue(
				BitmapUtil.isFieldPresent(
						binary,
						11
				)
		);

		assertTrue(
				BitmapUtil.isFieldPresent(
						binary,
						41
				)
		);
	}

	@Test
	void isFieldPresent_shouldReturnFalse_whenFieldDoesNotExist() {

		String bitmap =
				"7020000000800000";

		String binary =
				BitmapUtil.hexToBinary(bitmap);

		assertFalse(
				BitmapUtil.isFieldPresent(
						binary,
						5
				)
		);

		assertFalse(
				BitmapUtil.isFieldPresent(
						binary,
						6
				)
		);

		assertFalse(
				BitmapUtil.isFieldPresent(
						binary,
						42
				)
		);
	}

	// =========================================================
	// INVALID BITMAP
	// =========================================================

	@Test
	void hexToBinary_shouldThrowException_whenBitmapIsNull() {

		Iso8583ParseException exception =
				assertThrows(
						Iso8583ParseException.class,
						() -> BitmapUtil.hexToBinary(null)
				);

		assertEquals(
				Iso8583ErrorCode.INVALID_BITMAP,
				exception.getErrorCode()
		);
	}

	@Test
	void hexToBinary_shouldThrowException_whenBitmapIsEmpty() {

		Iso8583ParseException exception =
				assertThrows(
						Iso8583ParseException.class,
						() -> BitmapUtil.hexToBinary("")
				);

		assertEquals(
				Iso8583ErrorCode.INVALID_BITMAP,
				exception.getErrorCode()
		);
	}

	@Test
	void hexToBinary_shouldThrowException_whenBitmapLengthIsInvalid() {

		String bitmap =
				"702000000080000";

		Iso8583ParseException exception =
				assertThrows(
						Iso8583ParseException.class,
						() -> BitmapUtil.hexToBinary(bitmap)
				);

		assertEquals(
				Iso8583ErrorCode.INVALID_BITMAP,
				exception.getErrorCode()
		);
	}

	@Test
	void hexToBinary_shouldThrowException_whenBitmapContainsInvalidHex() {

		String bitmap =
				"70200000008Z0000";

		Iso8583ParseException exception =
				assertThrows(
						Iso8583ParseException.class,
						() -> BitmapUtil.hexToBinary(bitmap)
				);

		assertEquals(
				Iso8583ErrorCode.INVALID_BITMAP,
				exception.getErrorCode()
		);
	}

	// =========================================================
	// INVALID BINARY BITMAP
	// =========================================================

	@Test
	void isFieldPresent_shouldThrowException_whenBinaryBitmapIsNull() {

		Iso8583ParseException exception =
				assertThrows(
						Iso8583ParseException.class,
						() -> BitmapUtil.isFieldPresent(
								null,
								2
						)
				);

		assertEquals(
				Iso8583ErrorCode.INVALID_BITMAP,
				exception.getErrorCode()
		);
	}

	@Test
	void isFieldPresent_shouldThrowException_whenBinaryBitmapLengthIsInvalid() {

		String binaryBitmap =
				"101010";

		Iso8583ParseException exception =
				assertThrows(
						Iso8583ParseException.class,
						() -> BitmapUtil.isFieldPresent(
								binaryBitmap,
								2
						)
				);

		assertEquals(
				Iso8583ErrorCode.INVALID_BITMAP,
				exception.getErrorCode()
		);
	}

	@Test
	void isFieldPresent_shouldThrowException_whenFieldIsLessThanOne() {

		String bitmap =
				"7020000000800000";

		String binary =
				BitmapUtil.hexToBinary(bitmap);

		Iso8583ParseException exception =
				assertThrows(
						Iso8583ParseException.class,
						() -> BitmapUtil.isFieldPresent(
								binary,
								0
						)
				);

		assertEquals(
				Iso8583ErrorCode.INVALID_BITMAP,
				exception.getErrorCode()
		);
	}

	@Test
	void isFieldPresent_shouldThrowException_whenFieldIsGreaterThan64() {

		String bitmap =
				"7020000000800000";

		String binary =
				BitmapUtil.hexToBinary(bitmap);

		Iso8583ParseException exception =
				assertThrows(
						Iso8583ParseException.class,
						() -> BitmapUtil.isFieldPresent(
								binary,
								65
						)
				);

		assertEquals(
				Iso8583ErrorCode.INVALID_BITMAP,
				exception.getErrorCode()
		);
	}

	// =========================================================
	// BINARY CONTENT
	// =========================================================

	@Test
	void hexToBinary_shouldContainOnlyZeroAndOne() {

		String bitmap =
				"7020000000800000";

		String binary =
				BitmapUtil.hexToBinary(bitmap);

		assertTrue(
				binary.matches("[01]+")
		);
	}

	@Test
	void encodeField_shouldUseAsciiEncoding() {

		IsoFieldDefinition definition =
				new IsoFieldDefinition(
						41,
						FieldType.FIXED,
						6,
						FieldDataType.ALPHA,
						FieldEncoding.ASCII,
						false
				);

		byte[] result =
				Iso8583Encoder.encodeField(
						"ATMABC",
						definition
				);

		assertEquals(
				"41 54 4D 41 42 43",
				Iso8583Encoder.bytesToHex(result)
		);
	}

	@Test
	void encodeField_shouldUseBcdEncoding() {

		IsoFieldDefinition definition =
				new IsoFieldDefinition(
						3,
						FieldType.FIXED,
						6,
						FieldDataType.NUMERIC,
						FieldEncoding.BCD,
						true
				);

		byte[] result =
				Iso8583Encoder.encodeField(
						"123000",
						definition
				);

		assertEquals(
				"12 30 00",
				Iso8583Encoder.bytesToHex(result)
		);
	}

	@Test
	void encodeField_shouldUseBinaryEncoding() {

		IsoFieldDefinition definition =
				new IsoFieldDefinition(
						55,
						FieldType.LLLVAR,
						999,
						FieldDataType.BINARY,
						FieldEncoding.BINARY,
						false
				);

		byte[] result =
				Iso8583Encoder.encodeField(
						"9F0206000000010000",
						definition
				);

		assertEquals(
				"9F 02 06 00 00 00 01 00 00",
				Iso8583Encoder.bytesToHex(result)
		);
	}

	@Test
	void bcd_shouldEncodeAndDecodeCorrectly() {

		String original = "123456";

		byte[] encoded =
				Iso8583Encoder.encodeBcd(original);

		String decoded =
				Iso8583Encoder.decodeBcd(encoded);

		assertEquals(
				original,
				decoded
		);
	}

	@Test
	void binary_shouldEncodeAndDecodeCorrectly() {

		String original = "9F0206000000010000";

		byte[] encoded =
				Iso8583Encoder.encodeBinary(original);

		String decoded =
				Iso8583Encoder.decodeBinary(encoded);

		assertEquals(
				original.toUpperCase(),
				decoded
		);
	}

	@Test
	void bcd_shouldRejectNonNumericValue() {

		IsoFieldDefinition definition =
				new IsoFieldDefinition(
						3,
						FieldType.FIXED,
						6,
						FieldDataType.NUMERIC,
						FieldEncoding.BCD,
						true
				);

		assertThrows(
				Iso8583ParseException.class,
				() -> Iso8583Encoder.encodeField(
						"12AB34",
						definition
				)
		);
	}

	@Test
	void binary_shouldRejectInvalidHexValue() {

		IsoFieldDefinition definition =
				new IsoFieldDefinition(
						55,
						FieldType.LLLVAR,
						999,
						FieldDataType.BINARY,
						FieldEncoding.BINARY,
						false
				);

		assertThrows(
				Iso8583ParseException.class,
				() -> Iso8583Encoder.encodeField(
						"9F02ZZ",
						definition
				)
		);
	}

	@Test
	void binary_shouldRejectOddHexLength() {

		IsoFieldDefinition definition =
				new IsoFieldDefinition(
						55,
						FieldType.LLLVAR,
						999,
						FieldDataType.BINARY,
						FieldEncoding.BINARY,
						false
				);

		assertThrows(
				Iso8583ParseException.class,
				() -> Iso8583Encoder.encodeField(
						"9F0",
						definition
				)
		);
	}

	@Test
	void buildBytes_shouldMatchAsciiBuild() {

		IsoMessage message = new IsoMessage();

		message.setMti("0200");
		message.setField(2, "6212345678901234");
		message.setField(3, "123000");
		message.setField(4, "000000000010");
		message.setField(11, "000001");
		message.setField(41, "ATMABC");

		String builtMessage =
				builder.build(message);

		byte[] asciiBytes =
				Iso8583Encoder.encodeAscii(
						builtMessage
				);

		byte[] encodedBytes =
				builder.buildBytes(message);

		assertArrayEquals(
				asciiBytes,
				encodedBytes
		);
	}
}
