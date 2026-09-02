package com.example.iso8583;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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

			System.out.println();
			System.out.println("================================");
			System.out.println("       FIELD DEFINITIONS");
			System.out.println("================================");

			definitions.forEach(
					(fieldNumber, definition) ->
							System.out.println(
									"DE " + fieldNumber
											+ " : Type=" + definition.type()
											+ ", Max Length=" + definition.maxLength()
											+ ", Data Type=" + definition.dataType()
											+ ", Required=" + definition.required()
							)
			);

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

			// =========================
			// Print Built Message
			// =========================

			System.out.println();
			System.out.println("================================");
			System.out.println("       ISO BUILD SUCCESS");
			System.out.println("================================");

			System.out.println("ISO Message : " + isoMessage);
			System.out.println("Message Length : " + isoMessage.length());

			// =========================
			// Print Bitmap
			// =========================

			String primaryBitmap = isoMessage.substring(4, 20);

			System.out.println();
			System.out.println("Primary Bitmap : " + primaryBitmap);

			// Karena DE 65 ada,
			// secondary bitmap harus ada.

			String secondaryBitmap = isoMessage.substring(20, 36);

			System.out.println("Secondary Bitmap : " + secondaryBitmap);

			// =========================
			// PARSE
			// =========================

			Iso8583Parser parser = new Iso8583Parser(definitions);

			try {

				IsoMessage result = parser.parse(isoMessage);

				// =========================
				// Parse Success
				// =========================

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

			catch (Iso8583ParseException e) {
				printError(e);
			}
		};
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
}