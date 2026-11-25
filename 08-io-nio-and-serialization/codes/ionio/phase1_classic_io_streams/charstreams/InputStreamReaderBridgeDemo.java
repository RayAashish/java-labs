package ionio.phase1_classic_io_streams.charstreams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Author: raysi
 * Package: com.raysi.ionio.phase1_classic_io_streams.charstreams.codes
 *
 * Demonstrates:
 * 1. The Adapter Pattern in InputStreamReader: Bridging 8-bit byte streams to 16-bit character streams.
 * 2. UTF-8 multi-byte decoding mechanics:
 *    - 1-byte ASCII ('A')
 *    - 2-byte Latin ('ñ')
 *    - 3-byte Indic / Devanagari ('ॐ', 'अ')
 *    - 4-byte SMP Emojis ('🚀') -> decoded into a Java UTF-16 high/low surrogate pair!
 * 3. The catastrophe of Charset mismatch (Mojibake data corruption).
 * 4. Controlling decoding error behaviors: CodingErrorAction.REPORT vs REPLACE ( / \uFFFD).
 */
public class InputStreamReaderBridgeDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("==========================================================================");
        System.out.println(">>> DEMO: InputStreamReader Adapter & Multi-Byte UTF-8 Decoding <<<");
        System.out.println(">>> Author: raysi | Bytes-to-Chars Bridge Internals");
        System.out.println("==========================================================================\n");

        // Step 1: Multi-byte Unicode string inspection
        String testText = "A ñ ॐ 🚀";
        System.out.println("[Step 1] Inspecting string: \"" + testText + "\"");
        System.out.println("   -> String.length() (char count / UTF-16 code units): " + testText.length());
        System.out.println("   -> Character codePointCount: " + testText.codePointCount(0, testText.length()));

        byte[] utf8Bytes = testText.getBytes(StandardCharsets.UTF_8);
        System.out.println("   -> Total UTF-8 bytes: " + utf8Bytes.length);
        System.out.print("   -> Raw hex bytes: ");
        for (byte b : utf8Bytes) {
            System.out.printf("0x%02X ", b);
        }
        System.out.println("\n");

        // Step 2: The Adapter Pattern Bridge via InputStreamReader with explicit UTF-8
        System.out.println("[Step 2] Reading UTF-8 bytes via InputStreamReader (Adapter Pattern)...");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(utf8Bytes);
             InputStreamReader isr = new InputStreamReader(bais, StandardCharsets.UTF_8)) {

            System.out.println("   -> InputStreamReader encoding: " + isr.getEncoding());
            int ch;
            int index = 0;
            while ((ch = isr.read()) != -1) {
                System.out.printf("   ├── Char [%d]: '%c' (Unicode U+%04X)%s%n",
                        index++,
                        (char) ch,
                        ch,
                        Character.isSurrogate((char) ch) ? " [SURROGATE PAIR COMPONENT]" : "");
            }
        }
        System.out.println();

        // Step 3: Mojibake Corruption Demonstration (decoding UTF-8 bytes with ISO-8859-1)
        System.out.println("[Step 3] The Mojibake Trap: Decoding UTF-8 bytes with ISO-8859-1 (Latin-1)...");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(utf8Bytes);
             InputStreamReader isr = new InputStreamReader(bais, StandardCharsets.ISO_8859_1)) {

            StringBuilder corrupted = new StringBuilder();
            int ch;
            while ((ch = isr.read()) != -1) {
                corrupted.append((char) ch);
            }
            System.out.println("   -> Original Text:  \"" + testText + "\"");
            System.out.println("   -> Corrupted Text: \"" + corrupted + "\"");
            System.out.println("   [CRITICAL] Never omit Charset! Default platform charset leads to silent bugs across environments.");
        }
        System.out.println();

        // Step 4: Strict CharsetDecoder Error Handling (REPORT vs REPLACE)
        System.out.println("[Step 4] Handling Malformed UTF-8 Bytes via CharsetDecoder...");
        byte[] malformedBytes = new byte[]{ (byte) 0x41, (byte) 0xFF, (byte) 0xFE, (byte) 0x42 }; // 0xFF is invalid UTF-8

        // Strategy A: REPLACE with \uFFFD (Standard InputStreamReader default behavior)
        CharsetDecoder replacingDecoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .replaceWith("?");
        CharBuffer replacedChars = replacingDecoder.decode(ByteBuffer.wrap(malformedBytes));
        System.out.println("   -> Replacing Mode: \"" + replacedChars + "\" (Invalid bytes substituted)");

        // Strategy B: REPORT (Throws CharacterCodingException)
        CharsetDecoder reportingDecoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT);
        try {
            reportingDecoder.decode(ByteBuffer.wrap(malformedBytes));
            System.out.println("   -> Reporting Mode: Success (Unexpected!)");
        } catch (CharacterCodingException e) {
            System.out.println("   -> Reporting Mode: Caught expected " + e.getClass().getSimpleName()
                    + "! (Strict security boundary triggered)");
        }

        System.out.println("\n==========================================================================");
        System.out.println(">>> SUMMARY <<<");
        System.out.println("InputStreamReader bridges InputStream (byte oriented) to Reader (char oriented)");
        System.out.println("using an internal StreamDecoder that consumes a variable number of bytes (1-4)");
        System.out.println("to yield complete Unicode characters.");
        System.out.println("==========================================================================");
    }
}
