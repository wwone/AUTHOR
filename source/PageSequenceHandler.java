import java.io.PrintWriter;

public interface PageSequenceHandler
{
    /*
     * Updated 6/24/2014
     *
     * This interface is used mostly with
     * FOP processing, but could apply to
     * any creator object that starts a "page"
     * flow and ends it
     */
    
    public void endPageSeq(
        PrintWriter pr) throws Exception;
    
    public void startFlow(
        PrintWriter pr,
        String page_number_string) throws Exception;
    
}