package com.example.iso8583;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@SpringBootApplication
public class Iso8583Application {

	public static void main(String[] args) {
		SpringApplication.run(Iso8583Application.class, args);
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

					39, new IsoFieldDefinition(
							39,
							FieldType.FIXED,
							2,
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
									"DE " + fieldNumber +
											" : Type=" + definition.getFieldType() +
											", Max Length=" + definition.getMaxLength() +
											", Data Type=" + definition.getDataType() +
											", Encoding=" + definition.getEncoding() +
											", Length Unit=" + definition.getLengthUnit() +
											", Required=" + definition.isRequired()
							)
			);

			// =========================
			// Create Request 0200
			// =========================

			IsoMessage request = new IsoMessage();

			request.setMti("0200");
			request.setField(2, "6212345678901234");
			request.setField(3, "123000");
			request.setField(4, "000000000010");
			request.setField(11, "000123");
			request.setField(41, "ATMABC");

			Iso8583Mti mti = Iso8583Mti.parse(request.getMti());
			Iso8583MtiValidator.validateRequest(mti);

			// =========================
			// Build Request
			// =========================

			Iso8583Builder builder = new Iso8583Builder(definitions);

			String requestMessage = builder.build(request);

			System.out.println();
			System.out.println("================================");
			System.out.println("       REQUEST 0200");
			System.out.println("================================");
			System.out.println("ISO Message : " + requestMessage);

			// =========================
			// Create Response 0210
			// =========================

			IsoMessage response = createResponse(
					request,
					Iso8583ResponseCode.APPROVED
			);

			// =========================
			// Build Response
			// =========================

			String responseMessage = builder.build(response);

			System.out.println();
			System.out.println("================================");
			System.out.println("       RESPONSE 0210");
			System.out.println("================================");
			System.out.println("ISO Message : " + responseMessage);

			System.out.println();
			System.out.println("Response MTI  : " + response.getMti());
			System.out.println("Response DE11 : " + response.getField(11));
			System.out.println("Response DE39 : " + response.getField(39));
			System.out.println("Response DE41 : " + response.getField(41));
		};
	}

	private IsoMessage createResponse(
			IsoMessage request,
			String responseCode
	) {
		IsoMessage response = new IsoMessage();

		response.setMti("0210");

		response.setField(11, request.getField(11));
		response.setField(39, responseCode);
		response.setField(41, request.getField(41));

		return response;
	}
}