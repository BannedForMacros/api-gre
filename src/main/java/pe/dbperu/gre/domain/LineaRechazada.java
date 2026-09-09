package pe.dbperu.gre.domain;

/** Linea que el DataMart descartaria. Antes desaparecia sin dejar rastro. */
public class LineaRechazada {
    private final int item;
    private final int codArticulo;
    private final String motivo;

    public LineaRechazada(int item, int codArticulo, String motivo) {
        this.item = item;
        this.codArticulo = codArticulo;
        this.motivo = motivo;
    }

    public int getItem()        { return item; }
    public int getCodArticulo() { return codArticulo; }
    public String getMotivo()   { return motivo; }
}
