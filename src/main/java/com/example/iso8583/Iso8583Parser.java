package com.example.iso8583;

import java.util.Map;

public class Iso8583Parser {

    private final Map<Integer, IsoFieldDefinition> definitions;

    public Iso8583Parser(Map<Integer, IsoFieldDefinition> definitions) {

        if (definitions == null) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Field definitions tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        this.definitions = definitions;
    }

    public IsoMessage parse(String message) {

        // =========================
        // 1. Validate Message
        // =========================

        validateMessage(message);

        int position = 0;

        // =========================
        // 2. MTI
        // =========================

        validateRemainingLength(
                message,
                position,
                4,
                "MTI",
                null
        );

        String mti = message.substring(position, position + 4);

        validateMti(mti, position);

        position += 4;

        // =========================
        // 3. Primary Bitmap
        // =========================

        validateRemainingLength(
                message,
                position,
                16,
                "Primary bitmap",
                null
        );

        String primaryBitmap = message.substring(position, position + 16);

        validateBitmap(
                primaryBitmap,
                "Primary bitmap",
                position
        );

        position += 16;

        String binaryPrimaryBitmap = BitmapUtil.hexToBinary(primaryBitmap);

        // =========================
        // 4. Secondary Bitmap
        // =========================

        boolean hasSecondaryBitmap =
                BitmapUtil.isFieldPresent(
                        binaryPrimaryBitmap,
                        1
                );

        String secondaryBitmap = null;
        String binarySecondaryBitmap = null;

        if (hasSecondaryBitmap) {

            validateRemainingLength(
                    message,
                    position,
                    16,
                    "Secondary bitmap",
                    null
            );

            secondaryBitmap = message.substring(position, position + 16);

            validateBitmap(
                    secondaryBitmap,
                    "Secondary bitmap",
                    position
            );

            position += 16;

            binarySecondaryBitmap = BitmapUtil.hexToBinary(secondaryBitmap);
        }

        // =========================
        // 5. Validate Required Fields
        // =========================

        validateRequiredFields(
                binaryPrimaryBitmap,
                binarySecondaryBitmap,
                hasSecondaryBitmap,
                position
        );

        // =========================
        // 6. Create ISO Message
        // =========================

        IsoMessage isoMessage = new IsoMessage();

        isoMessage.setMti(mti);

        isoMessage.setBitmap(primaryBitmap);

        printBitmapInformation(
                mti,
                primaryBitmap,
                binaryPrimaryBitmap,
                secondaryBitmap,
                binarySecondaryBitmap
        );

        // =========================
        // 7. Parse Primary Fields
        // =========================

        for (int field = 2; field <= 64; field++) {

            if (!BitmapUtil.isFieldPresent(
                    binaryPrimaryBitmap,
                    field
            )) {
                continue;
            }

            ParsedField parsedField =
                    parseField(
                            message,
                            position,
                            field
                    );

            isoMessage.setField(field, parsedField.value());

            position = parsedField.nextPosition();
        }

        // =========================
        // 8. Parse Secondary Fields
        // =========================

        if (hasSecondaryBitmap) {

            for (int field = 65; field <= 128; field++) {

                int secondaryIndex = field - 64;

                if (!BitmapUtil.isFieldPresent(
                        binarySecondaryBitmap,
                        secondaryIndex
                )) {
                    continue;
                }

                ParsedField parsedField =
                        parseField(
                                message,
                                position,
                                field
                        );

                isoMessage.setField(field, parsedField.value());

                position = parsedField.nextPosition();
            }
        }

        // =========================
        // 9. Validate End Of Message
        // =========================

        validateEndOfMessage(message, position);

        return isoMessage;
    }

    // =========================================================
    // Parse Field
    // =========================================================

    private ParsedField parseField(
            String message,
            int position,
            int fieldNumber
    ) {

        IsoFieldDefinition definition = definitions.get(fieldNumber);

        // =========================
        // Definition Validation
        // =========================

        if (definition == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DEFINITION_NOT_FOUND,
                    "Definition DE " + fieldNumber + " tidak ditemukan",
                    fieldNumber,
                    position,
                    null
            );
        }

        int startPosition = position;

        // =========================
        // Position Validation
        // =========================

        validatePosition(message, position, fieldNumber);

//        System.out.println();
//        System.out.println("Membaca DE " + fieldNumber);
//        System.out.println("Position awal : " + position);

        String value;

        // =========================
        // FIXED
        // =========================

        if (definition.getFieldType() == FieldType.FIXED) {

            int length = definition.getMaxLength();

            validateRemainingLength(
                    message,
                    position,
                    length,
                    "DE " + fieldNumber,
                    fieldNumber
            );

            value = message.substring(position, position + length);

            position += length;
        }

        // =========================
        // LLVAR
        // =========================

        else if (definition.getFieldType() == FieldType.LLVAR) {

            int prefixLength = 2;

            validateRemainingLength(
                    message,
                    position,
                    prefixLength,
                    "DE " + fieldNumber + " length indicator",
                    fieldNumber
            );

            String lengthIndicator =
                    message.substring(
                            position,
                            position + prefixLength
                    );

            validateLengthIndicator(
                    fieldNumber,
                    lengthIndicator,
                    position
            );

            int length = Integer.parseInt(lengthIndicator);

            validateMaximumLength(
                    fieldNumber,
                    length,
                    definition.getMaxLength(),
                    position
            );

            position += prefixLength;

            validateRemainingLength(
                    message,
                    position,
                    length,
                    "DE " + fieldNumber,
                    fieldNumber
            );

            value = message.substring(position, position + length);

            position += length;
        }

        // =========================
        // LLLVAR
        // =========================

        else if (definition.getFieldType() == FieldType.LLLVAR) {

            int prefixLength = 3;

            validateRemainingLength(
                    message,
                    position,
                    prefixLength,
                    "DE " + fieldNumber + " length indicator",
                    fieldNumber
            );

            String lengthIndicator =
                    message.substring(
                            position,
                            position + prefixLength
                    );

            validateLengthIndicator(
                    fieldNumber,
                    lengthIndicator,
                    position
            );

            int length = Integer.parseInt(lengthIndicator);

            validateMaximumLength(
                    fieldNumber,
                    length,
                    definition.getMaxLength(),
                    position
            );

            position += prefixLength;

            validateRemainingLength(
                    message,
                    position,
                    length,
                    "DE " + fieldNumber,
                    fieldNumber
            );

            value = message.substring(position, position + length);
            position += length;
        }

        else {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Field type tidak didukung: " + definition.getFieldType(),
                    fieldNumber,
                    position,
                    null
            );
        }

        // =========================
        // Data Validation
        // =========================

        FieldValidator.validate(
                fieldNumber,
                value,
                definition.getDataType(),
                startPosition
        );

        // =========================
        // Tracking Position
        // =========================

        System.out.printf(
                "DE %-3d : position %d -> %d | value = %s%n",
                fieldNumber,
                startPosition,
                position,
                value
        );

        return new ParsedField(
                value,
                startPosition,
                position
        );
    }

    // =========================================================
    // Validate Message
    // =========================================================

    private void validateMessage(String message) {

        if (message == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "ISO message tidak boleh null",
                    null,
                    0,
                    null
            );
        }

        if (message.isBlank()) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "ISO message tidak boleh kosong",
                    null,
                    0,
                    message
            );
        }

        if (message.length() < 20) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "ISO message terlalu pendek. "
                            + "Minimal MTI + primary bitmap "
                            + "= 20 karakter",
                    null,
                    message.length(),
                    message
            );
        }
    }

    // =========================================================
    // Validate MTI
    // =========================================================

    private void validateMti(
            String mti,
            int position
    ) {

        if (mti.length() != 4) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI harus memiliki 4 karakter",
                    null,
                    position,
                    mti
            );
        }

        if (!mti.chars().allMatch(Character::isDigit)) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI harus numeric: " + mti,
                    null,
                    position,
                    mti
            );
        }
    }

    // =========================================================
    // Validate Bitmap
    // =========================================================

    private void validateBitmap(
            String bitmap,
            String bitmapName,
            int position
    ) {
        if (bitmap == null || bitmap.length() != 16) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_BITMAP,
                    bitmapName
                            + " harus memiliki "
                            + "16 karakter hexadecimal",
                    null,
                    position,
                    bitmap
            );
        }

        if (!bitmap.matches("[0-9A-Fa-f]{16}")) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_BITMAP,
                    bitmapName
                            + " mengandung karakter "
                            + "hexadecimal tidak valid: "
                            + bitmap,
                    null,
                    position,
                    bitmap
            );
        }
    }

    // =========================================================
    // Validate Position
    // =========================================================

    private void validatePosition(
            String message,
            int position,
            int fieldNumber
    ) {

        if (position < 0 || position > message.length()) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.POSITION_OUT_OF_BOUND,
                    "Position DE "
                            + fieldNumber
                            + " tidak valid. "
                            + "Position=" + position
                            + ", MessageLength="
                            + message.length(),
                    fieldNumber,
                    position,
                    null
            );
        }
    }

    // =========================================================
    // Validate Remaining Length
    // =========================================================

    private void validateRemainingLength(
            String message,
            int position,
            int requiredLength,
            String fieldName,
            Integer fieldNumber
    ) {

        if (position < 0 || position > message.length()) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.POSITION_OUT_OF_BOUND,
                    "Position tidak valid. "
                            + "Position=" + position
                            + ", MessageLength="
                            + message.length(),
                    fieldNumber,
                    position,
                    null
            );
        }

        int available = message.length() - position;

        if (available < requiredLength) {

            Iso8583ErrorCode errorCode;

            if (fieldName.contains("length indicator")) {
                errorCode = Iso8583ErrorCode.FIELD_LENGTH_INVALID;
            } else if (fieldName.startsWith("DE ")) {
                errorCode = Iso8583ErrorCode.FIELD_DATA_INCOMPLETE;
            } else {
                errorCode = Iso8583ErrorCode.INVALID_MESSAGE;
            }

            throw new Iso8583ParseException(
                    errorCode,
                    fieldName
                            + " tidak lengkap. "
                            + "Position=" + position
                            + ", Required="
                            + requiredLength
                            + ", Available="
                            + available,
                    fieldNumber,
                    position,
                    null
            );
        }
    }

    // =========================================================
    // Validate Length Indicator
    // =========================================================

    private void validateLengthIndicator(
            int fieldNumber,
            String lengthIndicator,
            int position
    ) {

        if (!lengthIndicator.chars().allMatch(Character::isDigit)) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_LENGTH_INVALID,
                    "DE " + fieldNumber
                            + " memiliki length indicator "
                            + "tidak valid: "
                            + lengthIndicator,
                    fieldNumber,
                    position,
                    lengthIndicator
            );
        }
    }

    // =========================================================
    // Validate Maximum Length
    // =========================================================

    private void validateMaximumLength(
            int fieldNumber,
            int actualLength,
            int maximumLength,
            int position
    ) {

        if (actualLength > maximumLength) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_LENGTH_EXCEEDED,
                    "DE " + fieldNumber
                            + " melebihi maximum length. "
                            + "Actual=" + actualLength
                            + ", Max="
                            + maximumLength,
                    fieldNumber,
                    position,
                    null
            );
        }
    }

    // =========================================================
    // Validate Required Fields
    // =========================================================

    private void validateRequiredFields(
            String primaryBitmap,
            String secondaryBitmap,
            boolean hasSecondaryBitmap,
            int position
    ) {

        for (IsoFieldDefinition definition : definitions.values()) {

            if (!definition.isRequired()) {
                continue;
            }

            int field = definition.getFieldNumber();

            boolean present;

            if (field <= 64) {

                present = BitmapUtil.isFieldPresent(
                        primaryBitmap,
                        field
                );

            } else {

                if (!hasSecondaryBitmap) {
                    throw new Iso8583ParseException(
                            Iso8583ErrorCode.SECONDARY_BITMAP_MISSING,
                            "DE " + field
                                    + " wajib ada tetapi "
                                    + "secondary bitmap "
                                    + "tidak tersedia",
                            field,
                            position,
                            null
                    );
                }

                int secondaryIndex = field - 64;

                present = BitmapUtil.isFieldPresent(
                        secondaryBitmap,
                        secondaryIndex
                );
            }

            if (!present) {
                throw new Iso8583ParseException(
                        Iso8583ErrorCode.FIELD_REQUIRED_MISSING,
                        "DE " + field
                                + " wajib ada tetapi "
                                + "tidak terdapat pada bitmap",
                        field,
                        position,
                        null
                );
            }
        }
    }

    // =========================================================
    // Validate End Of Message
    // =========================================================

    private void validateEndOfMessage(
            String message,
            int position
    ) {

        if (position < message.length()) {

            String remaining = message.substring(position);

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.REMAINING_DATA,
                    "Data tersisa setelah parsing selesai. "
                            + "Position=" + position
                            + ", MessageLength="
                            + message.length()
                            + ", Remaining="
                            + remaining,
                    null,
                    position,
                    remaining
            );
        }

        if (position > message.length()) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.POSITION_OUT_OF_BOUND,
                    "Parsing melewati panjang message. "
                            + "Position=" + position
                            + ", MessageLength="
                            + message.length(),
                    null,
                    position,
                    null
            );
        }
    }

    // =========================================================
    // Print Bitmap Information
    // =========================================================

    private void printBitmapInformation(
            String mti,
            String primaryBitmap,
            String binaryPrimaryBitmap,
            String secondaryBitmap,
            String binarySecondaryBitmap
    ) {

        System.out.println();
        System.out.println("================================");
        System.out.println("          BITMAP ANALYSIS");
        System.out.println("================================");
        System.out.println("MTI                 : " + mti);
        System.out.println("Primary Bitmap      : " + primaryBitmap);
        System.out.println("Primary Binary      : " + binaryPrimaryBitmap);
        System.out.println("Secondary Bitmap    : " + secondaryBitmap);
        System.out.println("Secondary Binary    : " + binarySecondaryBitmap);
        System.out.println("================================");
    }
}