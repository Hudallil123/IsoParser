package com.example.iso8583;

public final class BitmapUtil {

    private static final int BITMAP_HEX_LENGTH = 16;

    private static final int BITMAP_BINARY_LENGTH = 64;

    private BitmapUtil() {
    }

    public static String hexToBinary(
            String hex
    ) {

        if (hex == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_BITMAP,
                    "Bitmap tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        if (hex.isEmpty()) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_BITMAP,
                    "Bitmap tidak boleh kosong",
                    null,
                    null,
                    hex
            );
        }

        validateHexBitmap(hex);

        StringBuilder binary = new StringBuilder(hex.length() * 4);

        for (char c : hex.toCharArray()) {

            int value = Character.digit(c, 16);

            binary.append(
                    String.format(
                            "%4s",
                            Integer.toBinaryString(value)
                    ).replace(
                            ' ',
                            '0'
                    )
            );
        }

        return binary.toString();
    }

    public static boolean isFieldPresent(
            String binaryBitmap,
            int field
    ) {

        validateBinaryBitmap(binaryBitmap);

        if (field < 1 || field > binaryBitmap.length()) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_BITMAP,
                    "Field " + field +
                            " berada di luar panjang bitmap. " +
                            "Bitmap length=" +
                            binaryBitmap.length(),
                    field,
                    null,
                    null
            );
        }

        int index = field - 1;

        return binaryBitmap.charAt(index) == '1';
    }

    private static void validateHexBitmap(
            String hex
    ) {

        if (hex.length() != BITMAP_HEX_LENGTH) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_BITMAP,
                    "Panjang bitmap harus " +
                            BITMAP_HEX_LENGTH +
                            " karakter hex. " +
                            "Actual=" +
                            hex.length(),
                    null,
                    null,
                    hex
            );
        }

        for (char c : hex.toCharArray()) {

            if (Character.digit(c, 16) == -1) {

                throw new Iso8583ParseException(
                        Iso8583ErrorCode.INVALID_BITMAP,
                        "Bitmap mengandung karakter " +
                                "hexadecimal tidak valid: " +
                                c,
                        null,
                        null,
                        String.valueOf(c)
                );
            }
        }
    }

    private static void validateBinaryBitmap(
            String binaryBitmap
    ) {

        if (binaryBitmap == null) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_BITMAP,
                    "Binary bitmap tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        if (binaryBitmap.length()
                != BITMAP_BINARY_LENGTH) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_BITMAP,
                    "Panjang binary bitmap harus " +
                            BITMAP_BINARY_LENGTH +
                            " bit. " +
                            "Actual=" +
                            binaryBitmap.length(),
                    null,
                    null,
                    binaryBitmap
            );
        }

        for (char c :
                binaryBitmap.toCharArray()) {

            if (c != '0' && c != '1') {

                throw new Iso8583ParseException(
                        Iso8583ErrorCode.INVALID_BITMAP,
                        "Binary bitmap hanya boleh " +
                                "mengandung 0 atau 1",
                        null,
                        null,
                        binaryBitmap
                );
            }
        }
    }
}