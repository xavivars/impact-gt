package es.ua.impact.gt;

public class ReplaceRule implements Comparable<ReplaceRule> {
    private final String from;
    private final String to;
    private final Integer order;
    
    public ReplaceRule(String f,String t,int o) {
        from = f;
        to = t;
        order = o;
    }
    
    @Override
    public int compareTo(ReplaceRule t) {
        return order.compareTo(t.order);
    }
    
    public String getSource() { return from; }
    public String getTarget() { return to; }
}
