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
	CommandLineRunner iso8583ParserRunner() {

		return args -> {

			Map<Integer, IsoFieldDefinition> definitions =
					Map.of(
							2, new IsoFieldDefinition(2, FieldType.LLVAR, 19,FieldDataType.NUMERIC,true),
							3, new IsoFieldDefinition(3,FieldType.FIXED, 6,FieldDataType.NUMERIC,true),
							4, new IsoFieldDefinition(4, FieldType.FIXED, 12,FieldDataType.NUMERIC,true),
							11, new IsoFieldDefinition(11, FieldType.FIXED, 6,FieldDataType.NUMERIC,true),
							41, new IsoFieldDefinition(41, FieldType.FIXED, 6,FieldDataType.ALPHA,false),
							48, new IsoFieldDefinition(48, FieldType.LLLVAR, 25, FieldDataType.ALPHA,true)
					);

			System.out.println("Definisi Field ISO 8583:");
			definitions.forEach(
					(fieldNumber, definition) ->
							System.out.println(
									"Field " + fieldNumber +
											" : Max Length = " +
											definition.maxLength() +
											", Required = " +
											definition.required()
							)
			);

			Iso8583Parser parser = new Iso8583Parser(definitions);

			String message =
					"0200" +
							"7020000000010000" +
							"16" +
							"6212345678901234" +
							"123000" +
							"000000000010" +
							"000123" +
							"025" +
							"ABCDEFGHIJRXCYVUYIBDASJKL";

			printActiveBits(message.substring(4, 20));

			try {

				IsoMessage result = parser.parse(message);

				System.out.println();
				System.out.println("================================");
				System.out.println("       ISO PARSE SUCCESS");
				System.out.println("================================");
				System.out.printf("MTI		: %s%n", result.getMti());
				System.out.printf("DE 2	: %s%n", result.getField(2));
				System.out.printf("DE 3 	: %s%n", result.getField(3));
				System.out.printf("DE 4 	: %s%n", result.getField(4));
				System.out.printf("DE 11 	: %s%n", result.getField(11));
				System.out.printf("DE 41 	: %s%n", result.getField(41));
				System.out.printf("DE 48 	: %s%n", result.getField(48));
			}
			catch (Iso8583ParseException e) {

				System.out.println();
				System.out.println("================================");
				System.out.println("       ISO PARSE ERROR");
				System.out.println("================================");
				System.out.println("Error Code 	: " + e.getErrorCode());
				System.out.println("Message 	: " + e.getMessage());
				System.out.println("Field 		: " + e.getFieldNumber());
				System.out.println("Position 	: " + e.getPosition());
				System.out.println("Value       : " + e.getValue());
				System.out.println("================================");
			}
		};
	}

	private void printActiveBits(String bitmapHex) {

		String binary = new java.math.BigInteger(
				bitmapHex,
				16
		).toString(2);

		binary = String.format(
				"%64s",
				binary
		).replace(' ', '0');

		System.out.println();
		System.out.println("================================");
		System.out.println("          BITMAP ANALYSIS");
		System.out.println("================================");
		System.out.println("Bitmap Hex    : " + bitmapHex);
		System.out.println("Bitmap Binary : " + binary);
		System.out.println();

		System.out.println("Active Bits:");

		for (int i = 0; i < binary.length(); i++) {

			if (binary.charAt(i) == '1') {

				int de = i + 1;

				System.out.printf(
						"Bit %-2d -> DE %d%n",
						de,
						de
				);
			}
		}
	}


}