import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.function.BiFunction;

import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.SimpleFormatter;
import java.util.Calendar;

class Parser {
    
    private static final Logger logger = Logger.getLogger("xyz.wrabzy.qrcode.Parser");
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

    private static final Pattern NUMERIC = Pattern.compile("\\d+");
    private static final Pattern ALPHA = Pattern.compile("[-A-Z $%*+./:]+");
    private static final Pattern ALPHANUMERIC = Pattern.compile("[-A-Z0-9 $%*+./:]+");
    private static final Pattern BINARY = Pattern.compile("[^-A-Z0-9 $%*+./:]+");
    private static final BiFunction<String, Integer, byte[]> NCODER = Coder::encodeNumeric;
    private static final BiFunction<String, Integer, byte[]> ACODER = Coder::encodeAlphanumeric;
    private static final BiFunction<String, Integer, byte[]> BCODER = Coder::encodeBinary;

    private static Map<String, ArrayList<Segment[]>> definedSegments = new HashMap<>();

    public static ArrayList<Segment[]> defineSegmentingVariants(String message) throws UnableToEncodeException {
        QRCode.checkMessage(message);
        
        if (definedSegments.containsKey(message)) {
            logger.fine("No segment definition is required because the message is already segmented.");
            return definedSegments.get(message);
        }
    
        Matcher mNumeric = NUMERIC.matcher(message);
        Matcher mAlpha = ALPHA.matcher(message);
        Matcher mExclusiveBinary = BINARY.matcher(message);
    
        int structBitFlags = 0b000;
    
        if (mNumeric.find()) {
            structBitFlags = structBitFlags | 0b100;
        }
    
        if (mAlpha.find()) {
            structBitFlags = structBitFlags | 0b010;
        }
    
        if (mExclusiveBinary.find()) {
            structBitFlags = structBitFlags | 0b001;
        }
    
        ArrayList<Segment[]> segmentingVariants = new ArrayList<>();
    
        ArrayList<Segment> mixedSegments;
        Segment[] segments;
        switch(structBitFlags) {
            case 0b001:
                logger.fine("The message contains only characters that can only be encoded binary.");
                segmentingVariants.add(new Segment[]{new Segment(message, 0, BCODER, "binary")});
                break;
            case 0b010:
                logger.fine("The message contains only characters other than digits that can be alphanumeric encoded.");
                segmentingVariants.add(new Segment[]{new Segment(message, 0, ACODER, "alphanumeric")});
                break;
            case 0b011:
                logger.fine("The message contains characters other than digits that can be alphanumeric encoded and characters that can only be encoded binary.");
                // There are two variants:
                // first - mixed encoding (firstly - alphanumeric, then the rest of the characters - binary)
                // second - all characters binary encoding
                // variant with lowest version number wins
                mixedSegments = new ArrayList<>();
                do {
                    mixedSegments.add(new Segment(mAlpha.group(), mAlpha.start(), ACODER, "alphanumeric"));
                } while (mAlpha.find());
                do {
                    mixedSegments.add(new Segment(mExclusiveBinary.group(), mExclusiveBinary.start(), BCODER, "binary"));
                } while (mExclusiveBinary.find());
                segments = mixedSegments.toArray(new Segment[mixedSegments.size()]);
                Arrays.sort(segments, (f, s) -> f.getStartIndex() - s.getStartIndex());
                segmentingVariants.add(segments);
                segmentingVariants.add(new Segment[]{new Segment(message, 0, BCODER, "binary")});
                break;
            case 0b100:
                logger.fine("The message contains only digits.");
                segmentingVariants.add(new Segment[]{new Segment(message, 0, NCODER, "numeric")});
                break;
            case 0b101:
                logger.fine("The message contains digits and characters that can only be encoded binary.");
                // There are two variants:
                // first - mixed encoding (firstly - numeric, then the rest of the characters - binary)
                // second - all characters binary encoding
                // variant with lowest version number wins
                mixedSegments = new ArrayList<>();
                do {
                    mixedSegments.add(new Segment(mNumeric.group(), mNumeric.start(), NCODER, "numeric"));
                } while (mNumeric.find());
                do {
                    mixedSegments.add(new Segment(mExclusiveBinary.group(), mExclusiveBinary.start(), BCODER, "binary"));
                } while (mExclusiveBinary.find());
                segments = mixedSegments.toArray(new Segment[mixedSegments.size()]);
                Arrays.sort(segments, (f, s) -> f.getStartIndex() - s.getStartIndex());
                segmentingVariants.add(segments);
                segmentingVariants.add(new Segment[]{new Segment(message, 0, BCODER, "binary")});
                break;
            case 0b110:
                logger.fine("The message contains digits and characters other than digits that can be alphanumeric encoded.");
                // There are two variants:
                // first - mixed encoding (firstly - numeric, then the rest of the characters - alphanumeric)
                // second - all characters alphanumeric encoding
                // variant with lowest version number wins
                mixedSegments = new ArrayList<>();
                do {
                    mixedSegments.add(new Segment(mNumeric.group(), mNumeric.start(), NCODER, "numeric"));
                } while (mNumeric.find());
                do {
                    mixedSegments.add(new Segment(mAlpha.group(), mAlpha.start(), ACODER, "alphanumeric"));
                } while (mAlpha.find());
                segments = mixedSegments.toArray(new Segment[mixedSegments.size()]);
                Arrays.sort(segments, (f, s) -> f.getStartIndex() - s.getStartIndex());
                segmentingVariants.add(segments);
                segmentingVariants.add(new Segment[]{new Segment(message, 0, ACODER, "alphanumeric")});
                break;
            case 0b111:
                logger.fine("The message contains all kinds of characters.");
                // There are three variants:
                // first - mixed encoding (firstly - numeric, secondary - alphanumeric, then the rest of the characters - binary)
                // second - mixed encoding (firstly alphanumeric, then the rest of the characters - binary)
                // third - all characters binary encoding
                // variant with lowest version number wins
                mixedSegments = new ArrayList<>();
                do {
                    mixedSegments.add(new Segment(mNumeric.group(), mNumeric.start(), NCODER, "numeric"));
                } while (mNumeric.find());
                do {
                    mixedSegments.add(new Segment(mAlpha.group(), mAlpha.start(), ACODER, "alphanumeric"));
                } while (mAlpha.find());
                do {
                    mixedSegments.add(new Segment(mExclusiveBinary.group(), mExclusiveBinary.start(), BCODER, "binary"));
                } while (mExclusiveBinary.find());
                segments = mixedSegments.toArray(new Segment[mixedSegments.size()]);
                Arrays.sort(segments, (f, s) -> f.getStartIndex() - s.getStartIndex());
                segmentingVariants.add(segments);
                
                mixedSegments = new ArrayList<>();
                mAlpha = ALPHANUMERIC.matcher(message);
                mExclusiveBinary = mExclusiveBinary.reset();
                while (mAlpha.find()) {
                    mixedSegments.add(new Segment(mAlpha.group(), mAlpha.start(), ACODER, "alphanumeric"));
                }
                while (mExclusiveBinary.find()) {
                    mixedSegments.add(new Segment(mExclusiveBinary.group(), mExclusiveBinary.start(), BCODER, "binary"));
                }
                segments = mixedSegments.toArray(new Segment[mixedSegments.size()]);
                Arrays.sort(segments, (f, s) -> f.getStartIndex() - s.getStartIndex());
                segmentingVariants.add(segments);
                
                segmentingVariants.add(new Segment[]{new Segment(message, 0, BCODER, "binary")});
        }
    
        definedSegments.put(message, segmentingVariants);
        
        logger.fine("The following segmentation options have been identified: " + segmentingVariants.toString());
        return segmentingVariants;
    }
}