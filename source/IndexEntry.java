
/*
 * container for an
 * index/bookmark item ONLY!
 * Updated 7/22/2014
 *
 * Added "target". This target is not used by all media, so may
 * not always be populated.
 */
public class IndexEntry
{
    public String id; // used for cross-references, internal-destination in FOP, anchor in HTML, etc
    public String long_title;
    public String target; // additional information when the destination is in another file (target)
    
    /*
     * missing TARGET
     */
    public IndexEntry(String iid,
                      String longg)
    {
        id = iid;
        long_title = longg;
        target = null;
    }
    
    /*
     * target included
     */
    public IndexEntry(String iid,
                      String longg,
                      String targ)
    {
        id = iid;
        long_title = longg;
        target = targ;
    }

    public String toString()
    {
        return "IndexEntry, ID: " + id + ", Target: " + target + ", Text:" + long_title;
    }

}


