import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;


/*
 * last edited 12/1/2018
 *
 * abstract object for auxiliary information
 * to be passed around between the Sink and
 * book creator (AUTHOR reader).
 *
 * children implement various metadata schemes for a document
 *
 */
public abstract class AuxiliaryInformation
{
    /*
     * globals 
     */

	public final static int AUX_META = 0;

	public int auxiliary_type = -1; // must be filled in by children

	public String auxiliary_information = ""; // filled in by children

}
