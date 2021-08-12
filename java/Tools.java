import java.util.Arrays;
import java.util.ArrayList;

class Tools {
    public static String toString(ArrayList al) {
        StringBuilder sb = new StringBuilder();
        al.forEach(o -> {
            sb.append("    ");
            if (o.getClass().isArray()) {
                for (int i = 0; i < o.length; i++) {
                    sb.append("wut");
                }
            }
            else sb.append(o.toString());
            
            sb.append("\n");
        });
        return sb.toString();
    }
    
    public static void main(String[] args) {
        ArrayList<double[]> test = new  ArrayList<>();
        test.add(new double[]{.1, .2, .3, .4, .5});
        test.add(new double[]{.6, .7, .8, .9, .0});
        System.out.println(Tools.toString(test));
    }
}