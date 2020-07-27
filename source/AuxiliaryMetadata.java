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
 * last edited 
 * Sun 26 Jul 2020 09:40:55 AM CDT
 *
 * Have the Constructor do difficult things like issue messages
 * to stderr, when given a bad metadata string. Try to alert the 
 * user (cannot throw an exception....)
 *
 * remove dependency on old XMLUtils
 *
 * object for auxiliary metadata, such as 
 * that specified in the AUTHOR file, and to be
 * placed into metadata areas of the final output,
 * such as HTML headers 
 *
 */
public class AuxiliaryMetadata extends AuxiliaryInformation
{
    /*
     * globals 
     */

	public String description = "";

	public String keywords = "";

	public String title = "";

	public AuxiliaryMetadata(String in)
	{
		String xx[] = in.split(":"); // 1 (one) colon delimiter
		description = xx[0]; // first is additional description verbiage
		keywords = xx[1]; // second is additional keywords
		title = null;
	} // end constructor with a single string

	/*
	 * this constructor ought to throw an exception in case the metadata is incorrectly
	 * formatted. For now, we will spit an error out to stderr and make the metadata junky
	 */
	public AuxiliaryMetadata(String in,String tit)
	{
		title = tit;
		String xx[] = in.split(":"); // 1 (one) colon delimiter
		if (xx.length < 2)
		{
			// formatting wrong
			System.err.println("ERROR in Section metadata for : " + tit + ", --" + in + "--"); 
			description = "ERRORERROR";
			keywords = "ERROR,ERROR,ERROR";
		} 
		else
		{
			description = xx[0]; // first is additional description verbiage
			keywords = xx[1]; // second is additional keywords
		}
	} // end constructor with strings
}
