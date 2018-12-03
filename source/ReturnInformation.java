import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * container for the ID and text information
 * for a "return to top" marker that will be
 * stored in the output stream
 *
 * Updated 7/24/2014
 *
 */
public class ReturnInformation
{
    public String id; // used for cross-references, internal-destination in FOP, anchor in HTML, etc
    public String short_title;
    
    /*
     * simple constructor
     */
    public ReturnInformation(String iid,
                      String shor)
    {
        id = iid;
        short_title = shor;
    }

    /*
     * full toString() override
     */
    public String toString()
    {
        return "ToTop: " + id + " (" + short_title + ")";
    } // end tostring full override
    
                  
} // end return information container