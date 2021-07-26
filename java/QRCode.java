import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.BiFunction;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.SimpleFormatter;
import java.util.Calendar;


public class QRCode {
    
    private static final Logger logger = Logger.getLogger("xyz.wrabzy.qrcode.QRCode");
    static {
        logger.setUseParentHandlers(false);
        Handler handler = new ConsoleHandler();
        logger.addHandler(handler);
        
        logger.setLevel(java.util.logging.Level.FINEST);
        handler.setLevel(java.util.logging.Level.FINEST);
        
        handler.setFormatter(new SimpleFormatter() {
            @Override
            public String format(LogRecord record) {
                StringBuilder formatted = new StringBuilder();
                Calendar recordTime = Calendar.getInstance();
                recordTime.setTimeInMillis(record.getMillis());
                formatted.append(String.format("[%d.%02d.%02d %02d:%02d:%02d][%s][%s][%d]%n", 
                    recordTime.get(Calendar.YEAR),
                    recordTime.get(Calendar.MONTH),
                    recordTime.get(Calendar.DAY_OF_MONTH),
                    recordTime.get(Calendar.HOUR_OF_DAY),
                    recordTime.get(Calendar.MINUTE),
                    recordTime.get(Calendar.SECOND),
                    record.getLoggerName(),
                    record.getSourceMethodName(),
                    record.getThreadID()));
                if (record.getParameters() != null) {
                    formatted.append("with parameters:\n");
                    for (Object parameter: record.getParameters()) {
                        formatted.append(parameter.toString());
                        formatted.append("\n");
                    }
                }
                formatted.append(String.format("{%n    %s%n}%n%n", record.getMessage()));
                return formatted.toString();
            }
        });
    }
    
    private final String MESSAGE;
    private final Level LEVEL;
    private final byte[][] CODE;
    private final int VERSION;

    private QRCode (String message, Level level, int possibleVersion) throws UnableToEncodeException {
        MESSAGE = message;
        LEVEL = level;
        CODE = generateCode(possibleVersion);
        VERSION = (CODE.length - 25) / 4;
        
        logger.fine(toString());
    }
    
    public String getMessage() { return MESSAGE; }
    public Level getLevel() { return LEVEL; }
    public int getVersion() { return VERSION; }

    @Override
    public String toString() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE.length; i++) {
            for (int j = 0; j < CODE[i].length; j++) {
                if (CODE[i][j] == 1) code.append("@");
                else if (CODE[i][j] == 0) code.append(" ");
                else code.append("?");
            }
            code.append("\n");
        }
        return code.toString();
    }
    
    public static QRCode encode(String message) throws UnableToEncodeException {
        checkMessage(message);
        logger.fine("Encoding the message without specifying the version and level.\n    Message: \"" + message + "\"");
        QRCode qr;
        try {
            logger.fine("Trying to encode with H level");
            qr = new QRCode(message, Level.H, 1);
            logger.fine("Successfully coded with H level\n    Message: \"" + message + "\"");
        } catch (UnableToEncodeException uteeh) {
            try {
                logger.fine("Attempt to encode with H level failed\n    Trying to encode with Q level");
                qr = new QRCode(message, Level.Q, 1);
                logger.fine("Successfully coded with Q level\n    Message: \"" + message + "\"");
            } catch (UnableToEncodeException uteeq) {
                try {
                    logger.fine("Attempt to encode with Q level failed\n    Trying to encode with M level");
                    qr = new QRCode(message, Level.M, 1);
                    logger.fine("Successfully coded with M level\n    Message: \"" + message + "\"");
                } catch (UnableToEncodeException uteem) { 
                    try {
                        logger.fine("Attempt to encode with M level failed\n    Trying to encode with L level");
                        qr = new QRCode(message, Level.L, 1);
                        logger.fine("Successfully coded with L level\n    Message: \"" + message + "\"");                        
                    } catch (UnableToEncodeException uteel) { 
                        logger.fine("Attempt to encode with L level failed\n    The message cannot be encoded.\n    Message: \"" + message + "\"");
                        throw uteel;
                    }
                }
            }
        }
        return qr;
    }
    
    public static QRCode encode(String message, Level level) throws UnableToEncodeException {
        checkMessage(message);
        QRCode qr;
        if (level != null) {
            try {
                logger.fine("Encoding the message with " + level + " level without specifying the version\n    Message: \"" + message + "\"");
                qr = new QRCode(message, level, 1);
                logger.fine("Successfully coded with " + level + " level\n    Message: \"" + message + "\"");
            } catch (UnableToEncodeException utee) {
                logger.fine("The message cannot be encoded with " + level + " level.\n    Message: \"" + message + "\"");
                throw utee;
            }
        } else {
            qr = QRCode.encode(message);
        }
        return qr;
    }
    
    public static QRCode encode(String message, int version) throws UnableToEncodeException, TargetVersionException {
        checkMessage(message);
        checkVersion(version);
        logger.fine("Encoding the message with " + version + " version without specifying the level\n    Message: \"" + message + "\"");
        QRCode qr;
        Level level = Level.H;
        try {
            logger.fine("Trying to encode with " + version + " version and " + level + " level");
            qr = new QRCode(message, level, version);
            if (version != qr.getVersion()) throw new TargetVersionException();
        } catch (UnableToEncodeException eh) {
            logger.fine("Attempt to encode with " + version + " version and " + level + " level failed");
            try {
                level = Level.Q;
                logger.fine("Trying to encode with " + version + " version and " + level + " level");
                qr = new QRCode(message, Level.Q, version);
                if (version != qr.getVersion()) throw new TargetVersionException();
            } catch (UnableToEncodeException eq) {
                logger.fine("Attempt to encode with " + version + " version and " + level + " level failed");
                try {
                    level = Level.M;
                    logger.fine("Trying to encode with " + version + " version and " + level + " level");
                    qr = new QRCode(message, Level.Q, version);
                    if (version != qr.getVersion()) throw new TargetVersionException();
                } catch (UnableToEncodeException em) {
                    logger.fine("Attempt to encode with " + version + " version and " + level + " level failed");
                    try {
                        level = Level.L;
                        logger.fine("Trying to encode with " + version + " version and " + level + " level");
                        qr = new QRCode(message, Level.Q, version);
                        if (version != qr.getVersion()) throw new TargetVersionException("Message \"" + message +"\" is too long to encode with version " + version);
                    } catch (UnableToEncodeException el) {
                        logger.fine("Attempt to encode with " + version + " version and " + level + " level failed\n    The message cannot be encoded with version " + version + ".\n    Message: \"" + message + "\"");
                        throw el;
                    }
                }
            }
        }
        logger.fine("Successfully coded with " + version + " version and " + level + " level\n    Message: \"" + message + "\"");
        return qr;
    }
    
    public static QRCode encode(String message, Level level, int version) throws UnableToEncodeException, TargetVersionException {
        checkMessage(message);
        checkVersion(version);
        QRCode qr;
        if (level != null) {
            logger.fine("Encoding the message with " + level + " level and " + version + " version\n    Message: \"" + message + "\"");
            qr = new QRCode(message, level, version);
            if (version != qr.getVersion()) throw new TargetVersionException("Message \"" + message +"\" is too long to encode with version " + version);
            logger.fine("Successfully coded with " + level + " level and " + version + " version\n    Message: \"" + message + "\"");
        } else {
            qr = QRCode.encode(message, version);
        }
        return qr;
    }


    // package access
    static void checkMessage(String message) throws UnableToEncodeException {
        if (message == null || message.length() == 0) throw new UnableToEncodeException("Unable to encode empty message");
    }
    
    private static void checkVersion(int version) throws TargetVersionException {
        if (version < 1 || version > 40) 
            throw new TargetVersionException("Version " + version + " can't be used. " +
                                             "Version number must be in range [1 - 40].");
    }
    
    private byte[][] generateCode(int targetVersion) throws UnableToEncodeException {
        logger.fine("The code will be generated in 6 steps.");
        
        logger.fine("Step 1. Encoding the message");
        Map.Entry<Integer, byte[]> versionAndEncodedMessage = encodeMessage(targetVersion);
        int version = versionAndEncodedMessage.getKey();
        byte[] encodedMessage = versionAndEncodedMessage.getValue();
        logger.fine("End of step 1. Encoded message: " + Arrays.toString(encodedMessage) + "\n    Version: " + version);
        
        logger.fine("Step 2. Filling in the code");
        byte[] filled = fill(encodedMessage, version);
        logger.fine("End of step 2. Filled code: " + Arrays.toString(filled));
        
        logger.fine("Step 3. Splitting the code into blocks");
        byte[][] blocks = splitIntoBlocks(filled, version);
        logger.fine("End of step 3. Splitted code: " + Arrays.deepToString(blocks));
       
        logger.fine("Step 4. Generating correction blocks");
        byte[][] correctionBlocks = Corrector.makeCorrectionBlocks(blocks, LEVEL, version);
        logger.fine("End of step 4. Correction blocks: " + Arrays.deepToString(correctionBlocks));
        
        logger.fine("Step 5. Combining data blocks and corrections");
        ArrayList<Byte> qred = combine(blocks, correctionBlocks);
        logger.fine("End of step 5. Combined blocks: " + qred.toString());
        
        logger.fine("Step 6. Code markup");
        byte[][] code = markup(qred, version);
        logger.fine("End of step 6. Marked up code: " + Arrays.deepToString(code));
        
        logger.fine("End of QRCode generation");
        
        return code;
    }    
    
    private Map.Entry<Integer, byte[]> encodeMessage(int targetVersion) throws UnableToEncodeException {
        ArrayList<Segment[]> variants = Parser.defineSegmentingVariants(MESSAGE);
        TreeMap<Integer, byte[]> encodedVariants = new TreeMap<>();
        for (Segment[] variant: variants) {
            logger.fine("Encoding segments: " + Arrays.toString(variant));
            try {
                int possibleVersion = -1;
                int factVersion = targetVersion;
                byte[] encoded = null;
                for (int i = 0; i < 3 && possibleVersion != factVersion; i++) {
                    possibleVersion = factVersion;
                    byte[][] encodedSegments = new byte[variant.length][];
                    for (int j = 0; j < variant.length; j++) {
                        encodedSegments[j] = variant[j].getCoder().apply(variant[j].getContent(), possibleVersion);
                    }
                    logger.fine("Encoded segments: " + Arrays.deepToString(encodedSegments));
                    encoded = Coder.concat(encodedSegments);
                    factVersion = LEVEL.getVersion(encoded.length);
                }
                logger.fine("Message encoded with " + factVersion + " version: " + Arrays.toString(encoded) + "\n    Target version was " + targetVersion);
                if (factVersion < targetVersion) {
                    factVersion = targetVersion;
                    logger.fine("Since the actual version is smaller than the target version, the actual version is promoted to the target version");
                }
                encodedVariants.put(factVersion, encoded);
            } catch (IllegalArgumentException iae) {
                logger.fine("An exception was thrown while decoding a message: " + iae.getMessage());
            }
        }
        if (encodedVariants.size() > 0) {
            return encodedVariants.firstEntry();
        } else {
            throw new UnableToEncodeException("Impossible to encode message \"" + MESSAGE + " with level " + LEVEL);
        }
    }
    
    private byte[] fill(byte[] codeToBeFilled, int version) {
        int targetLength = LEVEL.getSize(version);
        int existingLength = codeToBeFilled.length;
        if (existingLength < targetLength) {
            byte[] filledCode = new byte[targetLength];
            int index = 0;
            while (index < existingLength) {
                filledCode[index] = codeToBeFilled[index++];
            }
            while (existingLength % 8 != 0) {
                filledCode[index++] = 0;
                existingLength++;
            }
            boolean oddStep = true;
            while (existingLength < targetLength) {
                if (oddStep) {
                    filledCode[index++] = 1;
                    filledCode[index++] = 1;
                    filledCode[index++] = 1;
                    filledCode[index++] = 0;
                    filledCode[index++] = 1;
                    filledCode[index++] = 1;
                    filledCode[index++] = 0;
                    filledCode[index++] = 0;
                } else {
                    filledCode[index++] = 0;
                    filledCode[index++] = 0;
                    filledCode[index++] = 0;
                    filledCode[index++] = 1;
                    filledCode[index++] = 0;
                    filledCode[index++] = 0;
                    filledCode[index++] = 0;
                    filledCode[index++] = 1;
                }
                existingLength += 8;
                oddStep = !oddStep;
            }
            return filledCode;
        } else {
            return codeToBeFilled;
        }
    }
    
    private byte[][] splitIntoBlocks(byte[] data, int version) {
        int nOfBlocks = LEVEL.howManyBlocks(version);
        byte[][] blocks = new byte[nOfBlocks][];
        if (nOfBlocks > 1) {
            int bytes = LEVEL.getSize(version) / 8;
            int bytesInBlock = bytes / nOfBlocks;
            int augmentedBlocks = bytes % nOfBlocks;
            int dataPointer = 0;
            for(int i = 0; i < blocks.length; i++) {
                int thisBlockLength = (bytesInBlock + (i >= nOfBlocks - augmentedBlocks ? 1 : 0)) * 8;
                blocks[i] = new byte[thisBlockLength];
                for(int j = 0; j < thisBlockLength; j++) {
                    blocks[i][j] = data[dataPointer++];
                }
            }
        } else {
            blocks[0] = data;
        }
        return blocks;
    }
    
    private ArrayList<Byte> combine(byte[][] dataBlocks, byte[][] correctionBlocks) {
        ArrayList<Byte> combined = new ArrayList<>();
        for (int bytePointer = 0; bytePointer < dataBlocks[dataBlocks.length - 1].length; bytePointer += 8) {
            for (int blocksPointer = 0; blocksPointer < dataBlocks.length; blocksPointer++) {
                if (bytePointer < dataBlocks[blocksPointer].length) {
                    int bitsPointer = bytePointer;
                    do {
                        combined.add(dataBlocks[blocksPointer][bitsPointer++]);
                    } while (bitsPointer % 8 != 0);
                }
            }
        }
        for (int bytePointer = 0; bytePointer < correctionBlocks[correctionBlocks.length - 1].length; bytePointer += 8) {
            for (int blocksPointer = 0; blocksPointer < correctionBlocks.length; blocksPointer++) {
                int bitsPointer = bytePointer;
                do {
                    combined.add(correctionBlocks[blocksPointer][bitsPointer]);
                } while (++bitsPointer % 8 != 0);
            }
        }
        return combined;
    }
    
    private static final int[][] alignmentPatterns = 
        new int[][] {      {-1},                     {18},                     {22},                     {26},                     {30},
                           {34},                {6,22,38},                {6,24,42},                {6,26,46},                {6,28,50},
                      {6,30,54},                {6,32,58},                {6,34,62},             {6,26,46,66},             {6,26,48,70},
                   {6,26,50,74},             {6,30,54,78},             {6,30,56,82},             {6,30,58,86},             {6,34,62,90},
                {6,28,50,72,94},          {6,26,50,74,98},         {6,30,54,78,102},         {6,28,54,80,106},         {6,32,58,84,110},
               {6,30,58,86,114},         {6,34,62,90,118},      {6,26,50,74,98,122},     {6,30,54,78,102,126},     {6,26,52,78,104,130},
           {6,30,56,82,108,134},     {6,34,60,86,112,138},     {6,30,58,86,114,142},     {6,34,62,90,118,146}, {6,30,54,78,102,126,150},
       {6,24,50,76,102,128,154}, {6,28,54,80,106,132,158}, {6,32,58,84,110,136,162}, {6,26,54,82,110,138,166}, {6,30,58,86,114,142,170}};
    
    private static final byte[][][] versionCodes = 
        new byte[][][] {{{0,0,0,0,1,0},{0,1,1,1,1,0},{1,0,0,1,1,0}}, {{0,1,0,0,0,1},{0,1,1,1,0,0},{1,1,1,0,0,0}}, {{1,1,0,1,1,1},{0,1,1,0,0,0},{0,0,0,1,0,0}},
                        {{1,0,1,0,0,1},{1,1,1,1,1,0},{0,0,0,0,0,0}}, {{0,0,1,1,1,1},{1,1,1,0,1,0},{1,1,1,1,0,0}}, {{0,0,1,1,0,1},{1,0,0,1,0,0},{0,1,1,0,1,0}},
                        {{1,0,1,0,1,1},{1,0,0,0,0,0},{1,0,0,1,1,0}}, {{1,1,0,1,0,1},{0,0,0,1,1,0},{1,0,0,0,1,0}}, {{0,1,0,0,1,1},{0,0,0,0,1,0},{0,1,1,1,1,0}},
                        {{0,1,1,1,0,0},{0,1,0,0,0,1},{0,1,1,1,0,0}}, {{1,1,1,0,1,0},{0,1,0,1,0,1},{1,0,0,0,0,0}}, {{1,0,0,1,0,0},{1,1,0,0,1,1},{1,0,0,1,0,0}},
                        {{0,0,0,0,1,0},{1,1,0,1,1,1},{0,1,1,0,0,0}}, {{0,0,0,0,0,0},{1,0,1,0,0,1},{1,1,1,1,1,0}}, {{1,0,0,1,1,0},{1,0,1,1,0,1},{0,0,0,0,1,0}},
                        {{1,1,1,0,0,0},{0,0,1,0,1,1},{0,0,0,1,1,0}}, {{0,1,1,1,1,0},{0,0,1,1,1,1},{1,1,1,0,1,0}}, {{0,0,1,1,0,1},{0,0,1,1,0,1},{1,0,0,1,0,0}},
                        {{1,0,1,0,1,1},{0,0,1,0,0,1},{0,1,1,0,0,0}}, {{1,1,0,1,0,1},{1,0,1,1,1,1},{0,1,1,1,0,0}}, {{0,1,0,0,1,1},{1,0,1,0,1,1},{1,0,0,0,0,0}},
                        {{0,1,0,0,0,1},{1,1,0,1,0,1},{0,0,0,1,1,0}}, {{1,1,0,1,1,1},{1,1,0,0,0,1},{1,1,1,0,1,0}}, {{1,0,1,0,0,1},{0,1,0,1,1,1},{1,1,1,1,1,0}},
                        {{0,0,1,1,1,1},{0,1,0,0,1,1},{0,0,0,0,1,0}}, {{1,0,1,0,0,0},{0,1,1,0,0,0},{1,0,1,1,0,1}}, {{0,0,1,1,1,0},{0,1,1,1,0,0},{0,1,0,0,0,1}},
                        {{0,1,0,0,0,0},{1,1,1,0,1,0},{0,1,0,1,0,1}}, {{1,1,0,1,1,0},{1,1,1,1,1,0},{1,0,1,0,0,1}}, {{1,1,0,1,0,0},{1,0,0,0,0,0},{0,0,1,1,1,1}},
                        {{0,1,0,0,1,0},{1,0,0,1,0,0},{1,1,0,0,1,1}}, {{0,0,1,1,0,0},{0,0,0,0,1,0},{1,1,0,1,1,1}}, {{1,0,1,0,1,0},{0,0,0,1,1,0},{0,0,1,0,1,1}},
                        {{1,1,1,0,0,1},{0,0,0,1,0,0},{0,1,0,1,0,1}}};  
                        
    private byte[][] markup(ArrayList<Byte> qred, int version) {
        int size = 2 * 4;
        if (version == 1) size += 21;
        else size = size + alignmentPatterns[version - 1][alignmentPatterns[version - 1].length - 1] + 7;
        byte[][] qrcode = new byte[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                qrcode[i][j] = -1;
            }
        }
        // making border 4x4
        for (int i = 0; i < size; i++) {
            if (i < 4 || i > size - 5) {
                for (int j = 0; j < size; j++) {
                    qrcode[i][j] = 0;
                }
            } else {
                for (int j = 0; j < 4; j++) {
                    qrcode[i][j] = 0;
                }
                for (int j = size - 1; j > size - 5; j--) {
                    qrcode[i][j] = 0;
                }
            }
        }
        // searching patterns
        for (int i = 4; i < 12; i++) {
            // top left
            for (int j = 4; j < 12; j++) {
                if (i == 11 || j == 11) qrcode[i][j] = 0;
                else if ((i == 4 || i == 10) && j < 11) qrcode[i][j] = 1;
                else if (i > 4 && i < 10 && (j == 4 || j == 10)) qrcode[i][j] = 1;
                else if ((i == 5 || i == 9) && j < 10) qrcode[i][j] = 0;
                else if (i > 5 && i < 9 && (j == 5 || j == 9)) qrcode[i][j] = 0;
                else qrcode[i][j] = 1;
            }
            // top right
            for (int j = size - 5; j > size - 13; j--) {
                if (i == 11 || j == size - 12) qrcode[i][j] = 0;
                else if ((i == 4 || i == 10) && j > size - 12) qrcode[i][j] = 1;
                else if (i > 4 && i < 10 && (j == size - 5 || j == size - 11)) qrcode[i][j] = 1;
                else if ((i == 5 || i == 9) && j > size - 11) qrcode[i][j] = 0;
                else if (i > 5 && i < 9 && (j == size - 6 || j == size - 10)) qrcode[i][j] = 0;
                else qrcode[i][j] = 1;
            }
        }
        // bot left
        for (int i = size - 5; i > size - 13; i--) {
            for (int j = 4; j < 12; j++) {
                if (i == size - 12 || j == 11) qrcode[i][j] = 0;
                else if ((i == size - 5 || i == size - 11) && j < 11) qrcode[i][j] = 1;
                else if (i < size - 5 && i > size - 11 && (j == 4 || j == 10)) qrcode[i][j] = 1;
                else if ((i == size - 6 || i == size - 10) && j < 10) qrcode[i][j] = 0;
                else if (i < size - 6 && i > size - 10 && (j == 5 || j == 9)) qrcode[i][j] = 0;
                else qrcode[i][j] = 1;
            }
        }
        
        // alignment patterns
        if (version > 1) {
            for (int i = 0; i < alignmentPatterns[version - 1].length; i++) {
                for (int j = 0; j < alignmentPatterns[version - 1].length; j++) {
                    int centerX = alignmentPatterns[version - 1][i] + 4;
                    int centerY = alignmentPatterns[version - 1][j] + 4;
                    boolean canFit = true;
                    for (int m = centerX - 2; m < centerX + 3; m++) {
                        for (int n = centerY - 2; n < centerY + 3; n++) {
                            canFit = canFit && (qrcode[m][n] == -1);
                        }
                    }
                    if (canFit) {
                        for (int m = centerX - 2; m < centerX + 3; m++) {
                            for (int n = centerY - 2; n < centerY + 3; n++) {
                                if (m == centerX - 2 || m == centerX + 2) qrcode[m][n] = 1;
                                else if ((m > centerX - 2 || m < centerX + 2) && (n == centerY - 2 || n == centerY + 2)) qrcode[m][n] = 1;
                                else if (m == centerX && n == centerY) qrcode[m][n] = 1;
                                else qrcode[m][n] = 0;
                            }
                        }
                    }
                }
            }
        }
        
        // sync lines
        // horizontal 
        {   
            boolean black = true;
            int i = 10;
            for (int j = 12; j < size - 12; j++) {
                if (qrcode[i][j] == -1) {
                    qrcode[i][j] = (byte) (black ? 1 : 0);
                }
                black = !black;
            }
        }
        // vertical 
        {   
            boolean black = true;
            int j = 10;
            for (int i = 12; i < size - 12; i++) {
                if (qrcode[i][j] == -1) {
                    qrcode[i][j] = (byte) (black ? 1 : 0);
                }
                black = !black;
            }
        }
        
        // version code
        if (version > 6) {
            for (int i = size - 15, n = 0; i < size - 12; i++, n++) {
                for (int j = 4; j < 10; j++) {
                    qrcode[i][j] = versionCodes[version - 7][n][j - 4];
                    qrcode[j][i] = versionCodes[version - 7][n][j - 4];
                }
            }
        }
        
        // trying on masks
        TreeMap<Integer, byte[][]> qrcodes = new TreeMap<>();
        tryOnMask(qred, 0, qrcodes, qrcode, (x, y) -> (x + y) % 2 == 0);
        tryOnMask(qred, 1, qrcodes, qrcode, (x, y) -> y % 2 == 0);
        tryOnMask(qred, 2, qrcodes, qrcode, (x, y) -> x % 3 == 0);
        tryOnMask(qred, 3, qrcodes, qrcode, (x, y) -> (x + y) % 3 == 0);
        tryOnMask(qred, 4, qrcodes, qrcode, (x, y) -> (x / 3 + y / 2) % 2 == 0);
        tryOnMask(qred, 5, qrcodes, qrcode, (x, y) -> (x * y) % 2 + (x * y) % 3 == 0);
        tryOnMask(qred, 6, qrcodes, qrcode, (x, y) -> ((x * y) % 2 + (x * y) % 3) % 2 == 0);
        tryOnMask(qred, 7, qrcodes, qrcode, (x, y) -> ((x * y) % 3 + (x + y) % 2) % 2 == 0);
        
        logger.fine("Chosen mask with penalty: " + qrcodes.firstEntry().getKey());
        return qrcodes.firstEntry().getValue();
    }
    
    private void tryOnMask(ArrayList<Byte> qred, int nOfMask, TreeMap<Integer, byte[][]> qrcodes, byte[][] qrBase, BiPredicate<Integer, Integer> condition) {
        byte[][] qrcode = new byte[qrBase.length][];
        int qrcodePointer = 0;
        for (byte[] qrline: qrBase) {
            qrcode[qrcodePointer++] = Arrays.copyOf(qrline, qrline.length);
        }
        
        // top left mask code
        {
            int i = 4;
            int maskCodePointer = 0;
            while (i < 10) {
                qrcode[12][i++] = LEVEL.getMaskCode(nOfMask)[maskCodePointer++];
            }
            qrcode[12][++i] = LEVEL.getMaskCode(nOfMask)[maskCodePointer++];
            i++;
            qrcode[i--][12] = LEVEL.getMaskCode(nOfMask)[maskCodePointer++];
            qrcode[i--][12] = LEVEL.getMaskCode(nOfMask)[maskCodePointer++];
            i--;
            while (i > 3) {
                qrcode[i--][12] = LEVEL.getMaskCode(nOfMask)[maskCodePointer++];
            }
        }
        
        // bot left and top right mask codes
        {
            int i = qrcode.length - 5;
            int maskCodePointer = 0;
            while (i > qrcode.length - 12) {
                qrcode[i--][12] = LEVEL.getMaskCode(nOfMask)[maskCodePointer++];
            }
            qrcode[i][12] = 1;
            i = qrcode.length - 12;
            while (i < qrcode.length - 4) {
                qrcode[12][i++] = LEVEL.getMaskCode(nOfMask)[maskCodePointer++];
            }
        }
        
        // data filling
        int rightCol = qrcode.length - 5;
        int leftCol;
        int row;
        int qredPointer = 0;
        boolean toTop = true;
        while (rightCol > 3) {
            leftCol = rightCol - 1;
            if (toTop) {
                row = qrcode.length - 5;
                while (row > 3) {
                    if (qrcode[row][rightCol] == -1) {
                        if (qredPointer < qred.size()) {
                            qrcode[row][rightCol] = condition.test(rightCol - 4, row - 4) ? invert(qred.get(qredPointer++)) : qred.get(qredPointer++);
                        } else qrcode[row][rightCol] = 0;
                    }
                    if (qrcode[row][leftCol] == -1) {
                        if (qredPointer < qred.size()) {
                            qrcode[row][leftCol] = condition.test(leftCol - 4, row - 4) ? invert(qred.get(qredPointer++)) : qred.get(qredPointer++);
                        } else qrcode[row][leftCol] = 0;
                    }  
                    row--;
                }
            } else {
                row = 4;
                while (row < qrcode.length - 4) {
                    if (qrcode[row][rightCol] == -1) {
                        if (qredPointer < qred.size()) {
                            qrcode[row][rightCol] = condition.test(rightCol - 4, row - 4) ? invert(qred.get(qredPointer++)) : qred.get(qredPointer++);
                        } else qrcode[row][rightCol] = 0;
                    }
                    if (qrcode[row][leftCol] == -1) {
                        if (qredPointer < qred.size()) {
                            qrcode[row][leftCol] = condition.test(leftCol - 4, row - 4) ? invert(qred.get(qredPointer++)) : qred.get(qredPointer++);
                        } else qrcode[row][leftCol] = 0;
                    }  
                    row++;
                }
            }
            toTop = !toTop;
            rightCol -= 2;
            if (rightCol == 10) rightCol -= 1;
        }
        
        int penalty = rateTheMask(qrcode);
        
        logger.fine("Mask " + nOfMask + " with penalty: " + penalty);
        qrcodes.put(penalty, qrcode);
    }
    
    private byte invert(byte b) {
        if (b == 1) return 0;
        else if (b == 0) return 1;
        else throw new IllegalArgumentException();
    }
    
    private static int rateTheMask(byte[][] qrcode) {
        int rule1 = 0;
        int rule2 = 0;
        int rule3 = 0;
        int rule4 = 0;
        
        // rule1
        // horizontal
        for (int i = 4; i < qrcode.length - 4; i++) {
            for (int j = 4; j < qrcode.length - 8;) {
                int penalty = 0;
                int c = j;
                byte value = qrcode[i][c];
                do {
                    penalty++;
                    c++;
                } while (c < qrcode.length - 4 && value == qrcode[i][c]);
                if (penalty > 4) {
                    rule1 = rule1 + (penalty - 2);
                    j = c;
                } else {
                    j++;
                }
            }
        }
        // vertical
        for (int j = 4; j < qrcode.length - 4; j++) {
            for (int i = 4; i < qrcode.length - 8;) {
                int penalty = 0;
                int c = i;
                byte value = qrcode[c][j];
                do {
                    penalty++;
                    c++;
                } while (c < qrcode.length - 4 && value == qrcode[c][j]);
                if (penalty > 4) {
                    rule1 = rule1 + (penalty - 2);
                    i = c;
                } else {
                    i++;
                }
            }
        }
        
        // rule2
        for (int i = 4; i < qrcode.length - 5; i++) {
            for (int j = 4; j < qrcode.length - 5; j++) {
                byte value = qrcode[i][j];
                if (value == qrcode[i + 1][j] && 
                    value == qrcode[i][j + 1] && 
                    value == qrcode[i + 1][j + 1]) rule2 += 3;
            }
        }
        
        // rule3
        // horizontal
        for (int i = 4; i < qrcode.length - 4; i++) {
            for (int j = 4; j < qrcode.length - 14;) {
                if (qrcode[i][j] == 1) {
                    if (qrcode[i][j + 1] == 0 &&
                        qrcode[i][j + 2] == 1 &&
                        qrcode[i][j + 3] == 1 &&
                        qrcode[i][j + 4] == 1 &&
                        qrcode[i][j + 5] == 0 &&
                        qrcode[i][j + 6] == 1 &&
                        qrcode[i][j + 7] == 0 &&
                        qrcode[i][j + 8] == 0 &&
                        qrcode[i][j + 9] == 0 &&
                        qrcode[i][j + 10] == 0) {
                            rule3 += 40;
                            j += 7;
                    } else {
                        j++;
                    }
                } else {
                    if (qrcode[i][j + 1] == 0 &&
                        qrcode[i][j + 2] == 0 &&
                        qrcode[i][j + 3] == 0 &&
                        qrcode[i][j + 4] == 1 &&
                        qrcode[i][j + 5] == 0 &&
                        qrcode[i][j + 6] == 1 &&
                        qrcode[i][j + 7] == 1 &&
                        qrcode[i][j + 8] == 1 &&
                        qrcode[i][j + 9] == 0 &&
                        qrcode[i][j + 10] == 1) {
                            rule3 += 40;
                            if (j + 14 < qrcode.length - 4 &&
                                qrcode[i][j + 11] == 0 &&
                                qrcode[i][j + 12] == 0 &&
                                qrcode[i][j + 13] == 0 &&
                                qrcode[i][j + 14] == 0) {
                                    j += 15;
                            } else {
                                j += 8;
                            }
                    } else {
                        j++;
                    }
                }
            }
        }
        
        // vertical
        for (int j = 4; j < qrcode.length - 4; j++) {
            for (int i = 4; i < qrcode.length - 14;) {
                if (qrcode[i][j] == 1) {
                    if (qrcode[i + 1][j] == 0 &&
                        qrcode[i + 2][j] == 1 &&
                        qrcode[i + 3][j] == 1 &&
                        qrcode[i + 4][j] == 1 &&
                        qrcode[i + 5][j] == 0 &&
                        qrcode[i + 6][j] == 1 &&
                        qrcode[i + 7][j] == 0 &&
                        qrcode[i + 8][j] == 0 &&
                        qrcode[i + 9][j] == 0 &&
                        qrcode[i + 10][j] == 0) {
                            rule3 += 40;
                            i += 7;
                    } else {
                        i++;
                    }
                } else {
                    if (qrcode[i + 1][j] == 0 &&
                        qrcode[i + 2][j] == 0 &&
                        qrcode[i + 3][j] == 0 &&
                        qrcode[i + 4][j] == 1 &&
                        qrcode[i + 5][j] == 0 &&
                        qrcode[i + 6][j] == 1 &&
                        qrcode[i + 7][j] == 1 &&
                        qrcode[i + 8][j] == 1 &&
                        qrcode[i + 9][j] == 0 &&
                        qrcode[i + 10][j] == 1) {
                            rule3 += 40;
                            if (i + 14 < qrcode.length - 4 &&
                                qrcode[i + 11][j] == 0 &&
                                qrcode[i + 12][j] == 0 &&
                                qrcode[i + 13][j] == 0 &&
                                qrcode[i + 14][j] == 0) {
                                    i += 15;
                            } else {
                                i += 8;
                            }
                    } else {
                        i++;
                    }
                }
            }
        }
        
        // rule4
        int blacks = 0;
        int total = 0;
        for (int i = 4; i < qrcode.length - 4; i++) {
            for (int j = 4; j < qrcode.length - 4; j++) {
                if (qrcode[i][j] == 1) blacks++;
                total++;
            }
        }
        double blacksToTotal = ((double) blacks) / total;
        blacksToTotal *= 100;
        blacksToTotal -= 50;
        rule4 = ((int) Math.abs(blacksToTotal)) * 2;
        return rule1 + rule2 + rule3 + rule4;
    }
    
    public static void main(String[] args) throws UnableToEncodeException, TargetVersionException {
        QRCode first = QRCode.encode("Hello, Habrahabr!", Level.Q);
        QRPic pic = new QRPic(7, first.CODE);
        pic.show();
    }
    
    
}












