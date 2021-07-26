import java.util.function.BiFunction;

class Segment {
    private final String segment;
    private final int from;
    private final BiFunction<String, Integer, byte[]> applicableCoder;
    private final String coderType;

    Segment(String segment, int from, BiFunction coder, String coderType) {
        this.segment = segment;
        this.from = from;
        applicableCoder = coder;
        this.coderType = coderType;
    }
    
    public String getContent() {
        return segment;
    }
    
    public int getStartIndex() {
        return from;
    }
    
    public BiFunction<String, Integer, byte[]> getCoder() {
        return applicableCoder;
    }
    
    @Override
    public String toString() {
        return String.format("S[%s]:%d:%s", segment, from, coderType);
        
    }
}