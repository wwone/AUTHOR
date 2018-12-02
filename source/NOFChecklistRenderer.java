import java.util.Map;
import java.io.PrintWriter;

public interface NOFChecklistRenderer
{
    /*
     * Updated 1/19/2018
     *
     * This interface is for the special content
     * creator object. It will offer a method to 
     * create a specific printed index, that for facilities
     * for which no postal history has been seen. This
     * is in contrast to an INDEX. Rather, this is a checklist
     * which may contain links, depending on the output format.
     *
     */
    
    public void renderNOFChecklist(PrintWriter pr, Map nof_by_city) throws Exception;
}
