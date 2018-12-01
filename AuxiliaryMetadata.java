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
	}

	public AuxiliaryMetadata(String in,String tit)
	{
		title = tit;
		String xx[] = in.split(":"); // 1 (one) colon delimiter
		description = xx[0]; // first is additional description verbiage
		keywords = xx[1]; // second is additional keywords
	}
}
