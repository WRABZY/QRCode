import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;

class Corrector {
    public static final Map<Integer, int[]> GENERATING_POLYNOMIAL;
    static {
        Map<Integer, int[]> processingMap = new HashMap<>(13);
        processingMap.put(7, new int[]{87, 229, 146, 149, 238, 102, 21});
        processingMap.put(10, new int[]{251, 67, 46, 61, 118, 70, 64, 94, 32, 45});
        processingMap.put(13, new int[]{74, 152, 176, 100, 86, 100, 106, 104, 130, 218, 206, 140, 78});
        processingMap.put(15, new int[]{8, 183, 61, 91, 202, 37, 51, 58, 58, 237, 140, 124, 5, 99, 105});
        processingMap.put(16, new int[]{120, 104, 107, 109, 102, 161, 76, 3, 91, 191, 147, 169, 182, 194, 225, 120});
        processingMap.put(17, new int[]{43, 139, 206, 78, 43, 239, 123, 206, 214, 147, 24, 99, 150, 39, 243, 163, 136});
        processingMap.put(18, new int[]{215, 234, 158, 94, 184, 97, 118, 170, 79, 187, 152, 148, 252, 179, 5, 98, 96, 153});
        processingMap.put(20, new int[]{17, 60, 79, 50, 61, 163, 26, 187, 202, 180, 221, 225, 83, 239, 156, 164, 212, 212, 188, 190});
        processingMap.put(22, new int[]{210, 171, 247, 242, 93, 230, 14, 109, 221, 53, 200, 74, 8, 172, 98, 80, 219, 134, 160, 105, 165, 231});
        processingMap.put(24, new int[]{229, 121, 135, 48, 211, 117, 251, 126, 159, 180, 169, 152, 192, 226, 228, 218, 111, 0, 117, 232, 87, 96, 227, 21});
        processingMap.put(26, new int[]{173, 125, 158, 2, 103, 182, 118, 17, 145, 201, 111, 28, 165, 53, 161, 21, 245, 142, 13, 102, 48, 227, 153, 145, 218, 70});
        processingMap.put(28, new int[]{168, 223, 200, 104, 224, 234, 108, 180, 110, 190, 195, 147, 205, 27, 232, 201, 21, 43, 245, 87, 42, 195, 212, 119, 242, 37, 9, 123});
        processingMap.put(30, new int[]{41, 173, 145, 152, 216, 31, 179, 182, 50, 48, 110, 86, 239, 96, 222, 125, 42, 173, 226, 193, 224, 130, 156, 37, 251, 216, 238, 40, 192, 180});
        GENERATING_POLYNOMIAL = Collections.unmodifiableMap(processingMap);
    }
    private static final int[][] GF = generateGF();

    public static byte[][] makeCorrectionBlocks(byte[][] blocks, Level level, int version) {
        
        int needToCreateCorrectionBytes = level.getCorrectionBytesPerBlock(version);
        int[] generatingPolynomial = GENERATING_POLYNOMIAL.get(needToCreateCorrectionBytes);
        int[][] correctionBlocks = new int[blocks.length][needToCreateCorrectionBytes];
        int correctionBlocksPointer = 0;
        
        for(byte[] block: blocks) {
            int bytesInBlock = block.length / 8;
            int arrLength = bytesInBlock > needToCreateCorrectionBytes ? bytesInBlock : needToCreateCorrectionBytes;
            int[] array = new int[arrLength];
            int blockPointer = 0;
            int arrayPointer = 0;
            int nextByte = 0;
            while (blockPointer < block.length) {
                if (block[blockPointer] == 1) {
                    nextByte += (int) Math.pow(2, 7 - blockPointer % 8);
                }
                blockPointer++;
                if (blockPointer % 8 == 0) {
                    array[arrayPointer++] = nextByte;
                    nextByte = 0;
                }
            }
            while (arrayPointer < arrLength) {
                array[arrayPointer++] = 0;
            }
            for (int i = 0; i < bytesInBlock; i++) {
                int a = array[0];
                System.out.println("a = " + a);
                for (int j = 0; j < arrLength - 1; j++) {
                    array[j] = array[j + 1];
                }
                array[arrLength - 1] = 0;
                if (a != 0) {
                    int b = GF[1][a];
                    for (int j = 0; j < needToCreateCorrectionBytes; j++) {
                        int c = generatingPolynomial[j] + b;
                        if (c > 254) c %= 255;
                        array[j] ^= GF[0][c];
                    }
                    /*for (int j = 0; j < array.length; j++) {
                        int c = generatingPolynomial[j] + b;
                        if (c > 254) c %= 255;
                        array[j] ^= GF[0][c];
                    }*/
                }
            }
            correctionBlocks[correctionBlocksPointer++] = Arrays.copyOf(array, needToCreateCorrectionBytes);
        }
        
        byte[][] correctionBlocksInBits = new byte[correctionBlocks.length][];
        for (int i = 0; i < correctionBlocksInBits.length; i++) {
            correctionBlocksInBits[i] = new byte[correctionBlocks[i].length * 8];
            int correctionBlocksInBitsPointer = 0;
            for (int j = 0; j < correctionBlocks[i].length; j++) {
                int nextByte = correctionBlocks[i][j];
                int counter = 7;
                while (counter > -1) {
                    int twoPow = (int) Math.pow(2, counter--);
                    if (nextByte >= twoPow) {
                        correctionBlocksInBits[i][correctionBlocksInBitsPointer++] = 1;
                        nextByte -= twoPow;
                    } else {
                        correctionBlocksInBits[i][correctionBlocksInBitsPointer++] = 0;
                    }
                }
            }
        }
        
        return correctionBlocksInBits;
    }
    
    public static int[][] generateGF() {
        int m = 8; // ECMA-130
        int n = 255; // n = 2^m - 1
        int t = 1;
        int k = 253; // k = n - 2^t
        int[] p = new int[]{1, 0, 1, 1, 1, 0, 0, 0, 1}; // ECMA-130 P(x) = x^8 + x^4 + x^3 + x^2 + 1
        int[] alphaTo = new int[n + 1];
        int[] indexOf = new int[n + 1];
        
        // generating
        int mask;
        mask = 1;
        alphaTo[m] = 0;
        for (int i = 0; i < m; i++) {
            alphaTo[i] = mask;
            indexOf[alphaTo[i]] = i;
            if (p[i] != 0) alphaTo[m] ^= mask;
            mask <<= 1;
        }
        indexOf[alphaTo[m]] = m;
        mask >>= 1;
        for (int i = m + 1; i < n; i++) {
            if (alphaTo[i - 1] >= mask) {
                alphaTo[i] = alphaTo[m] ^ ((alphaTo[i - 1]^mask) << 1);
            } else {
                alphaTo[i] = alphaTo[i - 1] << 1;
            }
            indexOf[alphaTo[i]] = i;
        }
        indexOf[0] = -1;
        
        int[][] gf = new int[2][];
        gf[0] = alphaTo;
        gf[1] = indexOf;
        return gf;
    }
    
    public static void main(String[] args) {
        { // Simple test
            byte[][] testBlocks = new byte[][]{{0,1,0,0,0,0,0,0,
                                                1,1,0,0,0,1,0,0,
                                                1,0,0,0,0,1,0,0,
                                                0,1,0,1,0,1,0,0,
                                                1,1,0,0,0,1,0,0,
                                                1,1,0,0,0,1,0,0,
                                                1,1,1,1,0,0,1,0,
                                                1,1,0,0,0,0,1,0,
                                                0,0,0,0,0,1,0,0,
                                                1,0,0,0,0,1,0,0,
                                                0,0,0,1,0,1,0,0,
                                                0,0,1,0,0,1,0,1,
                                                0,0,1,0,0,0,1,0,
                                                0,0,0,1,0,0,0,0,
                                                1,1,1,0,1,1,0,0,
                                                0,0,0,1,0,0,0,1}};
            Level testLevel = Level.H;
            int testVersion = 2;
            byte[][] test = makeCorrectionBlocks(testBlocks, testLevel, testVersion);
            byte[][] mustBe = new byte[][]{{0,0,0,1,0,0,0,0,
                                           0,1,0,1,0,1,0,1,
                                           0,0,0,0,1,1,0,0,
                                           1,1,1,0,0,1,1,1,
                                           0,0,1,1,0,1,1,0,
                                           0,0,1,1,0,1,1,0,
                                           1,0,0,0,1,1,0,0,
                                           0,1,0,0,0,1,1,0,
                                           0,1,1,1,0,1,1,0,
                                           0,1,0,1,0,1,0,0,
                                           0,0,0,0,1,0,1,0,
                                           1,0,1,0,1,1,1,0,
                                           1,1,1,0,1,0,1,1,
                                           1,1,0,0,0,1,0,1,
                                           0,1,1,0,0,0,1,1,
                                           1,1,0,1,1,0,1,0,
                                           0,0,0,0,1,1,0,0,
                                           1,1,1,1,1,1,1,0,
                                           1,1,1,1,0,1,1,0,
                                           0,0,0,0,0,1,0,0,
                                           1,0,1,1,1,1,1,0,
                                           0,0,1,1,1,0,0,0,
                                           0,0,1,0,0,1,1,1,
                                           1,1,0,1,1,0,0,1,
                                           0,1,1,1,0,0,1,1,
                                           1,0,1,1,1,1,0,1,
                                           1,1,0,0,0,0,0,1,
                                           0,0,0,1,1,0,0,0}};
            testByteArrays("Numeric simple test", test, mustBe);
        }
    }
    
    private static void testByteArrays(String testName, byte[][] test, byte[][] mustBe) {
        System.out.printf("%s: %s%n", testName, (Arrays.deepEquals(test, mustBe) ? "ok" : "failed"));
        if (!Arrays.equals(test, mustBe)) {
            System.out.println("recd: " + Arrays.deepToString(test));
            System.out.println("instd:" + Arrays.deepToString(mustBe));
        }
    }
}