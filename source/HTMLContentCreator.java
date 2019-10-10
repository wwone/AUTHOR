import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;


/*
 * base class for all HTML-specific special content creators. 
 *
 * Updated 10/8/2019
 * 
 * Adding Vanilla output for that HTML design 
 * 
 * Added code to create the "no postal history" listing 
 * as a separate stand-alone HTML page
 */

public  class HTMLContentCreator extends SpecialContentCreator
{
	
	public static boolean INSIDE_FULL_WIDTH = false; // start out not inside

	public static String NOF_FILE = "NOF.html"; // filename for "no postal history" checklist (webpage)

	/*
	 * Used so that we have more than one way to format
	 * HTML
	 * 
	 */
    public final static int FORMAT_SKELETON  = 0;
    public final static int FORMAT_POEM  = 1;
    public final static int FORMAT_VANILLA  = 1;

	/*
	 * static methods that should not be in
	 * the HTMLSink
	 */
    /*
     * print out a full page width "skeleton" grid starting div
     */
    public  static  void fullWidthSkeleton(PrintWriter pr)
    {
        /*
         * make lines to start a full-width section
         */
         if (INSIDE_FULL_WIDTH)
         {
         		pr.println("<!-- ALREADY IN FULL WIDTH -->\n");
         }
         INSIDE_FULL_WIDTH =  true;
         pr.println("<!-- full width -->\n<div class=\"row\"><div class=\"twelve columns\" style=\"margin-top: 5%\">");

    }
    /*
     * print out a full page width "vanilla" grid starting div
     */
    public  static  void fullWidthVanilla(PrintWriter pr)
    {
        /*
         * make lines to start a full-width section
         */
         if (INSIDE_FULL_WIDTH)
         {
         		pr.println("<!-- ALREADY IN FULL WIDTH -->\n");
         }
         INSIDE_FULL_WIDTH =  true;
         pr.println("<!-- full width -->\n<div class=\"row\"><div class=\"twelve columns\" style=\"margin-top: 5%\">");
// HERE VANILLA NEEDED

    }

    /*
     * print out a first column  width "skeleton" grid starting div
     * for a 2-column layout
	* THESE SHOULD BE IN THE create special content JSON!!
     */
    public  static  void columnOneSkeleton(PrintWriter pr)
    {
        pr.print("<!-- col 1 --><div class=\"row\">   <div class=\"five columns\" style=\"margin-top: 5%\">");
    }

    /*
     * print out a second column  width "skeleton" grid starting div
     * for a 2-column layout
     */
    public  static  void columnTwoSkeleton(PrintWriter pr)
    {
        pr.print("<!-- col 2 --><div class=\"row\">   <div class=\"five columns\" style=\"margin-top: 5%\">");
    }

    /*
     * print final code for a skeleton grid line entry
     * this "line" may have contained several grid components
     *
     * ALSO works with full-width
     */
    public  static  void finishSkeleton(PrintWriter pr)
    {
    		if (!INSIDE_FULL_WIDTH)
    		{
    			pr.println("<!-- not inside full width area -->\n"); // problem?
    		}
        pr.print("<!-- finish col and row --></div> <!-- end of column --></div> <!-- end of row -->\n");
        INSIDE_FULL_WIDTH = false; // no longer inside
    }
    /*
     * print final code for a vanilla grid line entry
     * this "line" may have contained several grid components
     *
     * ALSO works with full-width
     */
    public  static  void finishVanilla(PrintWriter pr)
    {
    		if (!INSIDE_FULL_WIDTH)
    		{
    			pr.println("<!-- not inside full width area -->\n"); // problem?
    		}
        pr.print("<!-- finish col and row --></div> <!-- end of column --></div> <!-- end of row -->\n");
        INSIDE_FULL_WIDTH = false; // no longer inside
    }
    
    


    /*
     * take the list of items and make a 2-column layout for the skeleton system
     * THERE MUST BE AT LEAST ONE ITEM IN THE LIST
     */
    public static void make2ColumnsSkeleton(PrintWriter pr, List items)
    {
        int tot = items.size();
        int half = tot / 2;  // will truncate, so 1 becomes 0, 2 becomes 1, 3 becomes 1, 4 becomes 2, etc
        if (half == 0)
        {
            // special case only one item
            columnOneSkeleton(pr);
            pr.println("<p>" + items.get(0) +
                       "</p>");
            finishSkeleton(pr); 
            return; // that is all, clear to end of line
        }
        else
        {
            // column 1 first
            columnOneSkeleton(pr);
            pr.println("<p>");
            //System.out.println("col1, from: 0 to: " + (half - 1));
            for (int inner1 = 0 ; inner1 < half ; inner1++)
            {
                pr.println(items.get(inner1) + "<br/>");  // break after each
            }
            /*
             * THIS IS NOT RIGHT, skeleton is not a draw-one-col, then draw-another-col,
             * but rather the left-items are in the same row one after another!
             */
            pr.println("</p></div><!-- not right -->\n");
            
            // column 2
            columnTwoSkeleton(pr);
            pr.println("<p>");
            //System.out.println("col2, from: " + half + " to: " + (tot - 1));
            for (int inner2 = half ; inner2 < tot ; inner2++)
            {
                pr.println(items.get(inner2) + "<br/>");  // break after each
            }
            pr.println("</p>");
            finishSkeleton(pr);
        }
    } // end make 2 columns

	/*
	 * Entry method for making a 2-column layout
	 * There is a unique layout for each type: SKEL(eton) and POEM
	 */
    public static void make2Columns(PrintWriter pr, List items,
		int format_flag)
	{
		if (format_flag == FORMAT_SKELETON)
		{
		    make2ColumnsSkeleton(pr, items);
			return;
		}
		if (format_flag == FORMAT_POEM)
		{
		    make2ColumnsPoem(pr, items);
			return;
		}
		if (format_flag == FORMAT_VANILLA)
		{
		    make2ColumnsPoem(pr, items); // FOR NOW, this is just a <table>
			return;
		}
		// fell through, wrong format
		pr.println("<!-- ERROR, WRONG FORMAT -->");
	} // end make2Columns

    /*
     * take the list of items and make a 2-column layout for the POEM system
     * THERE MUST BE AT LEAST ONE ITEM IN THE LIST
	* we use conventional <table> layout
     */
    public static void make2ColumnsPoem(PrintWriter pr, List items)
    {
        int tot = items.size();
	if ((tot % 2) != 0)
	{
		// odd, we will just add a blank item
		items.add(" ");
	}
        tot = items.size();
 // recalculate, always even
        int half = tot / 2;  
	pr.println("<table>");
	// write as left-right pairs
	for (int inner = 0 ; inner < half ; inner++)
	{
		pr.println("<tr><td>" +
		    items.get(inner) + "</td><td>" +
			items.get(inner + half) +
			"</td></tr>");
	}
	pr.println("</table>");
    } // end make 2 columns in POEM format

	/*
	 * because we are trying to descend from
	 * SpecialContentCreator, we have to implement
	 * a bunch of stuff. These will all throw
	 * exceptions. In fact, the HTML-creating children
	 * of this object must override.
	 */    
       public void createStaticHeaders(Object out,
	Object toc_content) throws Exception
	{
		throw new Exception("createStaticHeaders must be overridden!");
	}
       public void createStaticHeaders(Object out,
	Object toc_content,AuxiliaryInformation aux) throws Exception
	{
		throw new Exception("createStaticHeaders must be overridden!");
	}
	    public void createMetadata(Object out) throws Exception
    {
		throw new Exception("createMetadata must be overridden!");
	}
	    public void createTitlePage(Object out, Object notused) throws Exception
    {
		throw new Exception("createTitlePage must be overridden!");
	}
	        public void renderBookmarks(PrintWriter pr, List all) throws Exception
        {
		throw new Exception("renderBookmarks must be overridden!");
        }
        public void renderIndex(PrintWriter pr, List all, int level) throws Exception
        {
		throw new Exception("renderIndex must be overridden!");
        }
        public void createTOC(PrintWriter pr, 
                              Map all_maps,
                              List appendixes,
                              Map index_flags) throws Exception
        {
		throw new Exception("createTOC must be overridden!");
        }

        public void startFlow(
            PrintWriter pr,
            String page_number_string) throws Exception
        {
		throw new Exception("startFlow must be overridden!");
        }

        public void endPageSeq(
            PrintWriter pr) throws Exception
        {
		throw new Exception("endPageSeq must be overridden!");
        }

	public String specialReplacementProcessing(ReplacementString rval)
		{
			// cannot throw exception, rather than change all that, return null
			return null;
        }

    public Object getMetaData(String json_file) throws Exception
    {
		throw new Exception("getMetaData must be overridden!");
    }

    public void modifyMetaData() throws Exception
    {
		throw new Exception("modifyMetaData must be overridden!");
    }

    public ReplacementString getProjectKeyValue(String xx) throws Exception
    {
		throw new Exception("getProjectKeyValue must be overridden!");
	}

	/*
	 * A sorted map is sent, which has all "No Postal History" facilities
	 *
	 * create a simple HTML list output (probably can include links)
	 * 
	 * FOR NOW, write on a separate file, ignore the PrintWriter passed in 
	 */
    public void renderNOFChecklist(PrintWriter pr, Map nof_by_city) throws Exception
    {
	// "pr" ignored
	PrintWriter mypr = new PrintWriter(new File(NOF_FILE));
	/*
	 * MAKE SOME HEADINGS and metadata
	 * 
	 * toc and aux are null, but while we don't want the TOC 
	 * we may want some different text within the meta headers
	 */ 

	AuxiliaryMetadata met = new AuxiliaryMetadata(
		"This is a listing of all known facilities that have not yet shown any postal history material. It is published separately from the full book that lists all known military facilities in the United States during the First World War. See the webpage at 'swansongrp.com/bob.html' for more information. :postal history,missing,listing,no postal history seen,cover,postcard,military facility, ",
		"Checklist of Facilities for Which NO Postal History has Been Seen"
	);
	createStaticHeaders(mypr,null,met); // no toc, but add aux info
	mypr.println("<h2 id=\"checklist\">Checklist of Facilities For Which No Postal History Has Been Seen</h2>");
	mypr.println("<p>The following listing is an additional product created by Robert Swanson, and is sold separately from the full book on the postal history of United States Military facilities of the First World War. See <a href=\"http://swansongrp.com/bob.html\">the book description webpage</a> for more information.</p>");
        Iterator city_it = nof_by_city.keySet().iterator();
                
	ExactFacilityReferenceComparableByCity the_ref2 = null;
        while (city_it.hasNext())
        {
            the_ref2 = (ExactFacilityReferenceComparableByCity)city_it.next(); 
		mypr.println("<p>" + 
			BookUtils.eT(the_ref2.state_name) + ", " +
			 BookUtils.eT(the_ref2.city_name) + ", " +
			    the_ref2.fac_name + "</p>");
        } // end for each facility (NO) index item ordered by state,city
	/*
	 * MAKE SOME footer (kluge, this assumes POEM)
	 */
	mypr.println("<script type=\"text/javascript\" >\n" +
	" final_cleanup(); // final javascript work to prepare to operate\n" +
	"</script>\n" +
	"</body>" +
	"</html>");
	mypr.flush();
	mypr.close();
    } // end create NOF checklist
	
} // end base class for HTML special creators
