import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * container for the hierarchy item for
 * the index/bookmark structure
 * contains a List that is either more
 * IndexGroup objects, or IndexEntry items
 *
 * Updated 7/24/2014
 *
 * Added "breadcrumbs" which is a flag indicating that
 * the renderer ought to display the items under this
 * group as breadcrumbs, that is no newlines between entries,
 * so they splatter across the page.
 *
 * Added "navigate to top" where we can optionally
 * ask for a marker to be placed that will allow
 * the user to go back up. Needed for long lists.
 */
public class IndexGroup
{
    public String id; // used for cross-references, internal-destination in FOP, anchor in HTML, etc
    public String short_title;
    public String long_title;
    public List children; // either more IndexGroup items, or IndexEntry items ONLY, no MIX!
    public boolean breadcrumbs;
    public ReturnInformation navigate_to_top;
    
    /*
     * simple constructor, optional items are defaulted to empty
     */
    public IndexGroup(String iid,
                      String shor,
                      String longg)
    {
        id = iid;
        short_title = shor;
        long_title = longg;
        children = new ArrayList(100);
        breadcrumbs = false; // default
        navigate_to_top = null; // default
    }

    /*
     * full-up constructor with various optional items
     */
    public IndexGroup(String iid,
                      String shor,
                      String longg,
                      boolean crumb,
                      String nav,
                      String comm)
    {
        id = iid;
        short_title = shor;
        long_title = longg;
        children = new ArrayList(100);
        breadcrumbs = crumb;
        navigate_to_top = new ReturnInformation(nav,comm);
    }

    /*
     * full-up constructor with various optional items
     */
    public IndexGroup(String iid,
                      String shor,
                      String longg,
                      boolean crumb,
                      ReturnInformation nav)
    {
        id = iid;
        short_title = shor;
        long_title = longg;
        children = new ArrayList(100);
        breadcrumbs = crumb;
        navigate_to_top = nav;
    }

    /*
     * helper for toString() override method
     */
    public StringBuffer toStringMain()
    {
        StringBuffer bb = new StringBuffer();
        bb.append("IndexGroup, ID: " + 
                  id + ", TitleS:" + 
                  short_title + ", TitleL:" + 
                  long_title + ", Breadcrumbs:" +
                  breadcrumbs);
        if (navigate_to_top != null)
        {
            bb.append(", Top: " + navigate_to_top);
        }
        bb.append("\n\n"); // done with relevant heading info
        return bb;
    }
    
    /*
     * full toString() override, all children are listed
     */
    public String toString()
    {
        StringBuffer bb = toStringMain();
        Iterator ii = children.iterator();
        while (ii.hasNext())
        {
            bb.append(ii.next().toString() + "\n");   
        }
        return bb.toString();
    } // end tostring full override
    
    /*
     * short toString(), shows only first child entry
     */
    public String toStringBrief()
    {
        StringBuffer bb = toStringMain();
        Iterator ii = children.iterator();
        boolean first_time = true;
        Object working = null;
        while (ii.hasNext())
        {
            working = ii.next();
            if (working instanceof IndexGroup)
            {
                bb.append(((IndexGroup)working).toStringBrief() + "\n");   
            }
            else
            {
                bb.append(working.toString() + "\n");   
            }
            if (first_time)
            {
                break; // we have the first item only
            }
        }
        return bb.toString();
    } // end tostring brief version
                  
} // end indexgroup container