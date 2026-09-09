package com.example.iso8583;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@SpringBootApplication
public class Iso8583Application {

	public static void main(String[] args) {

		SpringApplication.run(
				Iso8583Application.class,
				args
		);
	}

	@Bean
	CommandLineRunner iso8583Runner() {

		return args -> {

			// =========================
			// Field Definitions
			// =========================

			Map<Integer, IsoFieldDefinition> definitions = Map.of(
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
							false
					),
					48, new IsoFieldDefinition(
							48,
							FieldType.LLLVAR,
							25,
							FieldDataType.ALPHA,
							true
					),
					65, new IsoFieldDefinition(
							65,
							FieldType.FIXED,
							6,
							FieldDataType.NUMERIC,
							false
					)
			);

			// =========================
			// Print Definitions
			// =========================
			printFieldDefinitions(definitions);

			// =========================
			// Create ISO Message
			// =========================

			IsoMessage message = new IsoMessage();

			message.setMti("0200");

			message.setField(2, "6212345678901234");
			message.setField(3, "123000");
			message.setField(4, "000000000010");
			message.setField(11, "000123");
			message.setField(41, "ATMABC");
			message.setField(48, "ABCDEFGHIJRXCYVUYIBDASJKL");

			// =========================
			// Add DE 65
			// =========================

			message.setField(65, "123456");

			// =========================
			// BUILD
			// =========================

			Iso8583Builder builder = new Iso8583Builder(definitions);

			String isoMessage = builder.build(message);

			printBuildResult(isoMessage);

			// =========================================================
			// 7. EXTRACT BITMAP
			// =========================================================

			String primaryBitmap =
					isoMessage.substring(
							4,
							20
					);

			boolean hasSecondaryBitmap =
					BitmapUtil.isFieldPresent(
							BitmapUtil.hexToBinary(
									primaryBitmap
							),
							1
					);

			String secondaryBitmap = null;

			if (hasSecondaryBitmap) {

				secondaryBitmap =
						isoMessage.substring(
								20,
								36
						);
			}

			printBitmapResult(
					primaryBitmap,
					secondaryBitmap
			);

			// =========================================================
			// 8. ASCII ENCODING
			// =========================================================
			byte[] encodedMessage = Iso8583Encoder.encodeAscii(isoMessage);
			printEncodingResult(
					isoMessage,
					encodedMessage
			);

			byte[] framedMessage =
					Iso8583Framer.addLengthHeader(
							encodedMessage
					);

			printMessageFramingResult(
					encodedMessage,
					framedMessage
			);

			int expectedLength =
					Iso8583Framer.readLengthHeader(
							framedMessage
					);

			byte[] extractedMessage =
					Iso8583Framer.extractMessage(
							framedMessage
					);

			String extractedIsoMessage =
					Iso8583Encoder.decodeAscii(
							extractedMessage
					);

			printMessageExtractResult(
					expectedLength,
					extractedMessage,
					isoMessage,
					extractedIsoMessage
			);

			// =========================================================
			// 9. ASCII DECODING
			// =========================================================
			String decodedMessage = Iso8583Encoder.decodeAscii(encodedMessage);
			printDecodeResult(
					decodedMessage,
					isoMessage
			);

			// =========================================================
			// 10. PARSE DECODED MESSAGE
			// =========================================================
			Iso8583Parser parser = new Iso8583Parser(definitions);
			try {
				IsoMessage result = parser.parse(decodedMessage);
				printParseResult(result);
			} catch (Iso8583ParseException e) {
				printError(e);
			}
		};
	}

	private void printMessageExtractResult(
			int expectedLength,
			byte[] extractedMessage,
			String isoMessage,
			String extractedIsoMessage
	) {

		System.out.println();
		System.out.println("================================");
		System.out.println("       MESSAGE EXTRACT");
		System.out.println("================================");

		System.out.println("Expected Length : " + expectedLength);
		System.out.println("Actual Length   : " + extractedMessage.length);
		System.out.println("Same Message    : " + isoMessage.equals(extractedIsoMessage));

		System.out.println("================================");

	}

	private void printError(Iso8583ParseException e) {
		System.out.println();
		System.out.println("================================");
		System.out.println("         ISO PARSE ERROR");
		System.out.println("================================");

		System.out.println("Error Code : " + e.getErrorCode());
		System.out.println("Message    : " + e.getMessage());
		System.out.println("Field      : " + e.getFieldNumber());
		System.out.println("Position   : " + e.getPosition());
		System.out.println("Value      : " + e.getValue());

		System.out.println("================================");
	}

	// =========================================================
	// PRINT FIELD DEFINITIONS
	// =========================================================

	private void printFieldDefinitions(
			Map<Integer, IsoFieldDefinition> definitions
	) {

		System.out.println();
		System.out.println("================================");
		System.out.println("       FIELD DEFINITIONS");
		System.out.println("================================");

		definitions.forEach(
				(fieldNumber, definition) ->
						System.out.println(
								"DE " + fieldNumber
										+ " : Type="
										+ definition.type()
										+ ", Max Length="
										+ definition.maxLength()
										+ ", Data Type="
										+ definition.dataType()
										+ ", Required="
										+ definition.required()
						)
		);

		System.out.println("================================");
	}

	// =========================================================
	// PRINT BUILD RESULT
	// =========================================================

	private void printBuildResult(String isoMessage) {

		System.out.println();
		System.out.println("================================");
		System.out.println("       ISO BUILD SUCCESS");
		System.out.println("================================");

		System.out.println("ISO Message   : " + isoMessage);
		System.out.println("Message Length: " + isoMessage.length());

		System.out.println("================================");
	}


	// =========================================================
	// PRINT BITMAP RESULT
	// =========================================================

	private void printBitmapResult(
			String primaryBitmap,
			String secondaryBitmap
	) {

		System.out.println();
		System.out.println("================================");
		System.out.println("          BITMAP RESULT");
		System.out.println("================================");

		System.out.println("Primary Bitmap   : " + primaryBitmap);
		System.out.println(
				"Primary Binary   : "
						+ BitmapUtil.hexToBinary(primaryBitmap)
		);

		if (secondaryBitmap != null) {

			System.out.println("Secondary Bitmap : " + secondaryBitmap);
			System.out.println(
					"Secondary Binary : "
							+ BitmapUtil.hexToBinary(secondaryBitmap)
			);

		} else {

			System.out.println("Secondary Bitmap : NONE");
		}

		System.out.println("================================");
	}


	// =========================================================
	// PRINT ENCODING RESULT
	// =========================================================

	private void printEncodingResult(
			String isoMessage,
			byte[] encodedMessage
	) {

		System.out.println();
		System.out.println("================================");
		System.out.println("       ASCII ENCODING RESULT");
		System.out.println("================================");
		System.out.println("String Length : " + isoMessage.length());
		System.out.println("Byte Length   : " + encodedMessage.length);
		System.out.println("HEX           : " + Iso8583Encoder.bytesToHex(encodedMessage));
		System.out.println("================================");
	}


	// =========================================================
	// PRINT PARSE RESULT
	// =========================================================

	private void printParseResult(IsoMessage result) {

		System.out.println();
		System.out.println("================================");
		System.out.println("       ISO PARSE SUCCESS");
		System.out.println("================================");

		System.out.printf("MTI    : %s%n", result.getMti());
		System.out.printf("DE 2   : %s%n", result.getField(2));
		System.out.printf("DE 3   : %s%n", result.getField(3));
		System.out.printf("DE 4   : %s%n", result.getField(4));
		System.out.printf("DE 11  : %s%n", result.getField(11));
		System.out.printf("DE 41  : %s%n", result.getField(41));
		System.out.printf("DE 48  : %s%n", result.getField(48));
		System.out.printf("DE 65  : %s%n", result.getField(65));

		System.out.println("================================");
	}

	private void printMessageFramingResult (
			byte[] encodedMessage,
			byte[] framedMessage
	) {

		System.out.println();
		System.out.println("================================");
		System.out.println("       MESSAGE FRAMING");
		System.out.println("================================");

		System.out.println("ISO Message Length : " + encodedMessage.length);
		System.out.println(
				"Length Header      : "
						+ new String(
						framedMessage,
						0,
						4,
						StandardCharsets.US_ASCII
				)
		);
		System.out.println("Framed Length      : " + framedMessage.length);
		System.out.println("Framed HEX         : " + Iso8583Encoder.bytesToHex(framedMessage));
		System.out.println("================================");

	}

	private void printDecodeResult (
			String decodedMessage,
			String isoMessage
	){
		System.out.println();
		System.out.println("================================");
		System.out.println("       ASCII DECODE RESULT");
		System.out.println("================================");
		System.out.println("Decoded Message : " + decodedMessage);
		System.out.println("Same as Original: " + isoMessage.equals(decodedMessage));
	}
}