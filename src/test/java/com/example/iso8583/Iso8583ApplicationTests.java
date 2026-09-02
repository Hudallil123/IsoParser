package com.example.iso8583;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class Iso8583ApplicationTests {

	private Iso8583Parser parser;

	@BeforeEach
	void setUp() {

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
								false
						),
						48, new IsoFieldDefinition(
								48,
								FieldType.LLLVAR,
								25,
								FieldDataType.ALPHA,
								true
						)
				);

		parser = new Iso8583Parser(definitions);
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
}
