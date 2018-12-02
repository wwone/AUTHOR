import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.util.TreeMap;
import java.util.List;

/*
 * REFACTOR edited May 2, 2010 
 * 
 * Changed 12/1/2018 
 * 
 * Remove dependency on old XMLUtils 
 * 
 * The code here is the source for HTML
 * creation.
 *
 */
public abstract class GenericSource
{
    /*
     * globals 
     */
   
    
    public GenericSink g_sink;
    
    
    public GenericSource() 
    {   
        g_sink = null; // will eventually be set from children objects
    } // end constructor
    
} // end generic source
