package es.ua.impact;

import java.io.Writer;

public interface LexiconUpdater {
    public void update();
    public void save(Writer w);
}
