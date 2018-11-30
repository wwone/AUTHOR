import java.util.List;
import java.io.PrintWriter;


public interface BookmarkRenderer
{
    /*
     * Updated 6/25/2014
     *
     * This interface is for the special content
     * creator object. It will offer a method to 
     * create bookmarks as used in PDF files. They are
     * very like printed indexes, which show up in PDF
     * and others
     *
     * The List that is passed contains structures that meet the
     * following criteria:
     *
     * All objects in the List are IndexGroup
     *   any IndexGroup may contain a List of IndexEntry objects,
     *   which are the last branches of the tree
     *   If an IndexGroup contains a List of IndexGroup objects,
     *   then it is a sub-index, and the processor reading it
     *   has to recurse to handle it.
     *
     *
     *
     */
    
    public void renderBookmarks(PrintWriter pr, List all) throws Exception;
}
