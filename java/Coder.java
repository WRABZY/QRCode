import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.UnsupportedEncodingException;

public class Coder {
    
    public static final Map<Character, Integer> ALPHANUMERIC_MAP;
    static {
        Map<Character, Integer> processingMap = new HashMap<>(45);
        processingMap.put('0', 0);
        processingMap.put('1', 1);
        processingMap.put('2', 2);
        processingMap.put('3', 3);
        processingMap.put('4', 4);
        processingMap.put('5', 5);
        processingMap.put('6', 6);
        processingMap.put('7', 7);
        processingMap.put('8', 8);
        processingMap.put('9', 9);
        processingMap.put('A', 10);
        processingMap.put('B', 11);
        processingMap.put('C', 12);
        processingMap.put('D', 13);
        processingMap.put('E', 14);
        processingMap.put('F', 15);
        processingMap.put('G', 16);
        processingMap.put('H', 17);
        processingMap.put('I', 18);
        processingMap.put('J', 19);
        processingMap.put('K', 20);
        processingMap.put('L', 21);
        processingMap.put('M', 22);
        processingMap.put('N', 23);
        processingMap.put('O', 24);
        processingMap.put('P', 25);
        processingMap.put('Q', 26);
        processingMap.put('R', 27);
        processingMap.put('S', 28);
        processingMap.put('T', 29);
        processingMap.put('U', 30);
        processingMap.put('V', 31);
        processingMap.put('W', 32);
        processingMap.put('X', 33);
        processingMap.put('Y', 34);
        processingMap.put('Z', 35);
        processingMap.put(' ', 36);
        processingMap.put('$', 37);
        processingMap.put('%', 38);
        processingMap.put('*', 39);
        processingMap.put('+', 40);
        processingMap.put('-', 41);
        processingMap.put('.', 42);
        processingMap.put('/', 43);
        processingMap.put(':', 44);
        ALPHANUMERIC_MAP = Collections.unmodifiableMap(processingMap);
    }
    
    public static byte[] encodeNumeric(String message, int version) {
        
        checkVersion(version);
        
        // 1. Encoding method code
        byte[] encodingMethod = new byte[]{0, 0, 0, 1};
        
        // 2. Number of characters to be encoded 
        byte[] numberOfCharacters = nOfCharacters(0b0001, message.length(), version);
        
        // 3. Message encoding
        byte[] encodedMessage = encodeNum(message);
        
        // 4. Concatenation
        byte[] result = new byte[encodingMethod.length + numberOfCharacters.length + encodedMessage.length];
        int resultPointer = 0;
        for (int i = 0; i < encodingMethod.length; i++) {
            result[resultPointer++] = encodingMethod[i];
        }
        for (int i = 0; i < numberOfCharacters.length; i++) {
            result[resultPointer++] = numberOfCharacters[i];
        }
        for (int i = 0; i < encodedMessage.length; i++) {
            result[resultPointer++] = encodedMessage[i];
        }
        
        return result;
    }
    
    public static byte[] encodeAlphanumeric(String message, int version) {
        
        checkVersion(version);
        
        // 1. Encoding method code
        byte[] encodingMethod = new byte[]{0, 0, 1, 0};
        
        // 2. Number of characters to be encoded 
        byte[] numberOfCharacters = nOfCharacters(0b0010, message.length(), version);
        
        // 3. Message encoding
        byte[] encodedMessage = encodeAlphanum(message);
        
        // 4. Concatenation
        byte[] result = new byte[encodingMethod.length + numberOfCharacters.length + encodedMessage.length];
        int resultPointer = 0;
        for (int i = 0; i < encodingMethod.length; i++) {
            result[resultPointer++] = encodingMethod[i];
        }
        for (int i = 0; i < numberOfCharacters.length; i++) {
            result[resultPointer++] = numberOfCharacters[i];
        }
        for (int i = 0; i < encodedMessage.length; i++) {
            result[resultPointer++] = encodedMessage[i];
        }
        
        return result;
    }  
        
    public static byte[] encodeBinary(String message, int version) {
        
        checkVersion(version);
        
        // 1. Encoding method code
        byte[] encodingMethod = new byte[]{0, 1, 0, 0};
        
        // 2. Number of characters to be encoded 
        /*
            Binary encoding uses the number of bytes in the encoded sequence, 
            not the number of characters to be encoded. 
            This information will be determined later. 
        */
        
        // 3. Message encoding
        byte[] encodedMessage = encodeBin(message);
        
        // 2. Return to number of bytes in the encoded sequence
        int nOfBytes = encodedMessage.length / 8;
        byte[] numberOfBytesCode;
        if (version < 10) {
            numberOfBytesCode = new byte[8];
        } else {
            numberOfBytesCode = new byte[16];
        }
        String binaryForm = Integer.toBinaryString(nOfBytes);
        int binaryFormPointer = binaryForm.length() - 1;
        int nbcPointer = numberOfBytesCode.length - 1;
        while (nbcPointer > -1 && binaryFormPointer > -1) {
            numberOfBytesCode[nbcPointer--] = (byte) Character.digit(binaryForm.charAt(binaryFormPointer--), 10);
        }
        while (nbcPointer > -1) {
           numberOfBytesCode[nbcPointer--] = 0;
        }
        
        // 4. Concatenation
        byte[] result = new byte[encodingMethod.length + numberOfBytesCode.length + encodedMessage.length];
        int resultPointer = 0;
        for (int i = 0; i < encodingMethod.length; i++) {
            result[resultPointer++] = encodingMethod[i];
        }
        for (int i = 0; i < numberOfBytesCode.length; i++) {
            result[resultPointer++] = numberOfBytesCode[i];
        }
        for (int i = 0; i < encodedMessage.length; i++) {
            result[resultPointer++] = encodedMessage[i];
        }
        
        return result;
    }
    
    static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array: arrays) length += array.length;
        byte[] concated = new byte[length];
        int cIndex = 0;
        for (byte[] array: arrays) {
            for (int aIndex = 0; aIndex < array.length; aIndex++) {
                concated[cIndex++] = array[aIndex];
            }
        }
        return concated;
    }
    
    private static void checkVersion(int version) {
        if (version < 1 || version > 40) 
            throw new IllegalArgumentException("Version " + version + " can't be used. " +
                                               "Version number must be in range [1 - 40].");
    }
    
    private static byte[] nOfCharacters(int encodingMethod, int length, int version) {
        
        byte[] numberOfCharactersCode;
        
        /* Detailed code, the essence of what is happening:
        if (encodingMethod == 0b0001) {
            if (version < 10) {
                numberOfCharactersCode = new byte[10];
            } else if (version < 27) {
                numberOfCharactersCode = new byte[12];
            } else {
                numberOfCharactersCode = new byte[14];
            }
        } else if (encodingMethod == 0b0010) {
            if (version < 10) {
                numberOfCharactersCode = new byte[9];
            } else if (version < 27) {
                numberOfCharactersCode = new byte[11];
            } else {
                numberOfCharactersCode = new byte[13];
            }
        } else {
            throw new IllegalArgumentException("Wrong encoding method code: " + encodingMethod + ". " +
                                               "This method can only be used with 1 (0b0001) and 2 (0b0010) encoding methods.");
        }
        */
        
        // Short code: 
        if (version < 10) {
            numberOfCharactersCode = new byte[11 - encodingMethod];
        } else if (version < 27) {
            numberOfCharactersCode = new byte[13 - encodingMethod];
        } else {
            numberOfCharactersCode = new byte[15 - encodingMethod];
        }
        // Since the method is private, I take responsibility for using the correct encoding method codes. 
        
        int nccPointer = numberOfCharactersCode.length - 1;
        String binaryForm = Integer.toBinaryString(length);
        for (int i = binaryForm.length() - 1; i > -1; i--) {
            numberOfCharactersCode[nccPointer--] = (byte) Character.digit(binaryForm.charAt(i), 10);
        }
        while (nccPointer > -1) {
           numberOfCharactersCode[nccPointer--] = 0;
        }
        
        return numberOfCharactersCode;
    }
    
    private static byte[] encodeNum(String message) {
        int length = message.length();
        int encodedLength = length / 3 * 10;
        if (length % 3 == 2) encodedLength += 7;
        else if (length % 3 == 1) encodedLength += 4;
        
        byte[] encodedMessage = new byte[encodedLength];
        int encodedPointer = 0;
        
        int from = 0;
        int to = length < 3 ? length : 3;
        while (to <= length) {
            try {
                int number = Integer.parseInt(message.substring(from, to));
                int nBits = 10;
                if (to - from == 2) nBits = 7;
                else if (to - from == 1) nBits = 4;
                StringBuilder binaryForm = new StringBuilder(Integer.toBinaryString(number));
                while (binaryForm.length() < nBits) binaryForm.insert(0, "0");
                for (int i = 0; i < binaryForm.length(); i++) {
                    encodedMessage[encodedPointer++] = (byte) Character.digit(binaryForm.charAt(i), 10);
                }
                from = to;
                to += 3;
                if (to > length && from < length) to = length;
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("The digital encoding method is not applicable for the message: \""
                                                   + message +
                                                   "\" because it contains characters other than digits.");
            }
        }
        
        return encodedMessage;
    }
    
    private static byte[] encodeAlphanum(String message) {
        int length = message.length();
        int encodedLength = length / 2 * 11 + length % 2 * 6;
        
        byte[] encodedMessage = new byte[encodedLength];
        int encodedPointer = 0;
        
        int firstIndex = 0;
        int secondIndex = length == 1 ? 0 : 1;
        while (secondIndex < length) {
            int number = ALPHANUMERIC_MAP.getOrDefault(message.charAt(firstIndex), -1);
            if (number == -1)
                throw new IllegalArgumentException("The alphanumeric encoding method is not applicable for the message: \""
                                                   + message +
                                                   "\" because it contains character '" + message.charAt(firstIndex) + "'.");
            int nBits = 6;
            if (secondIndex > firstIndex) {
                int secondNumber = ALPHANUMERIC_MAP.getOrDefault(message.charAt(secondIndex), -1);
                if (secondNumber == -1)
                    throw new IllegalArgumentException("The alphanumeric encoding method is not applicable for the message: \""
                                                       + message +
                                                       "\" because it contains character '" + message.charAt(secondIndex) + "'.");
                number = 45 * number + secondNumber;
                nBits = 11;
            }
            StringBuilder binaryForm = new StringBuilder(Integer.toBinaryString(number));
            while (binaryForm.length() < nBits) binaryForm.insert(0, "0");
            for (int i = 0; i < binaryForm.length(); i++) {
                encodedMessage[encodedPointer++] = (byte) Character.digit(binaryForm.charAt(i), 10);
            }
            firstIndex += 2;
            secondIndex += 2;
            if (secondIndex == length) secondIndex = firstIndex;
        }
        
        return encodedMessage;
    }
    
    private static byte[] encodeBin(String message) {
        try {
            ArrayList<Byte> boofer = new ArrayList<>(message.length() * 16);
            for (byte b: message.getBytes("UTF-8")) {
                StringBuilder binaryForm = new StringBuilder(Integer.toBinaryString(Byte.toUnsignedInt(b)));
                while (binaryForm.length() < 8) binaryForm.insert(0, "0");
                for (int i = 0; i < binaryForm.length(); i++) {
                    boofer.add((byte) Character.digit(binaryForm.charAt(i), 10));
                }
            }
            byte[] encodedMessage = new byte[boofer.size()];
            int encodedPointer = 0;
            for (Byte b: boofer) {
                encodedMessage[encodedPointer++] = b;
            }
            return encodedMessage;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("The UTF-8 encoding method is not applicable for the message: \"" + message + "\"");
        }
    }
    
    // Test
    public static void main(String[] args) {
        
        { // Numeric coding simple test
            byte[] test = encodeNum("12345678");
            byte[] mustBe = new byte[]{0,0,0,1,1,1,1,0,1,1,0,1,1,1,0,0,1,0,0,0,1,0,0,1,1,1,0};
            testByteArrays("Numeric simple test", test, mustBe);
        }
        
        
        { // Alphanumeric coding simple test
            byte[] test = encodeAlphanum("HELLO");
            byte[] mustBe = new byte[]{0,1,1,0,0,0,0,1,0,1,1,0,1,1,1,1,0,0,0,1,1,0,0,1,1,0,0,0};
            testByteArrays("Alphanumeric simple test", test, mustBe);
        }
        
        { // Binary coding simple test
            byte[] test = encodeBin("Хабр");
            byte[] mustBe = new byte[]{1,1,0,1,0,0,0,0,1,0,1,0,0,1,0,1,1,1,0,1,0,0,0,0,1,0,1,1,0,0,0,0,1,1,0,1,0,0,0,0,1,0,1,1,0,0,0,1,1,1,0,1,0,0,0,1,1,0,0,0,0,0,0,0};
            testByteArrays("Binary simple test", test, mustBe);
        }
        
    }
    
    private static void testByteArrays(String testName, byte[] test, byte[] mustBe) {
        System.out.printf("%s: %s%n", testName, (Arrays.equals(test, mustBe) ? "ok" : "failed"));
        if (!Arrays.equals(test, mustBe)) {
            System.out.println("recd: " + Arrays.toString(test));
            System.out.println("instd:" + Arrays.toString(mustBe));
        }
    }
}