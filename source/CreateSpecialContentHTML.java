import java.io.PrintWriter;
import java.io.File;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayDeque;
import java.util.Properties;

// for JSON processing

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/*
 * Modified 
 * Wed 15 Jul 2020 08:49:57 AM CDT
 *
 * TODO: 
 *
 * Add Vanilla to layout systems
 *   There are two variants, vanlight and vandark, process them
 *   correctly.
 *
 * BUG: when the JSON is not formatted for the right type
 *   of HTML layout (VAN, SKEL, etc), the code throws an Exception
 *   but provides no helpful information (fixed)
 *
 * Work on making cover page, title page, etc OPTIONAL
 *
 * In due time, work on making a "single page" layout, much
 * like what is done now for Kindle
 *
 * WARNING NOTE: the method of dealing with multiple
 * HTML types (POEM, SKEL) are too complex. The JSON
 * processor can recognize named arrays, and it will make
 * them into Map objects. So rather than read through
 * each one, looking for the front marker on each list,
 * we can just do a Map lookup. This will take
 * a bit of work to set up, but is much cleaner. (DONE)
 * 
 *
 * WARNING NOTE, etc, this is back to being a generic
 * HTML creator. It will read flags in the options.json file
 * to drive several features, such as:
 *
 * 1) POEM, VANILLALIGHT, VANILLADARK, or SKELETON HTML output
 *    As of 2020, less attention will be given to POEM and SKELETON,
 *    so issues may creep in as Vanilla is chosen as the layout
 *    system of choice for HTML output.
 *
 * 2) Single HTML page, or one page per SECTION (or state, etc)
 *
 * 3) Flag whether we write all the FRONT material HTML files, such
 *    as cover page, title page, preface, etc
 *
 * JSON objects are a bit more complex than for other
 * processing. Inside each JSON object is a named array of
 * Strings, the array name being SKEL or POEM.
 *
 *
 *
 * change TOC to be dynamic "click to see table of contents", use
 *      Javascript to reveal and hide TOC (DONE)
 *
 */

/*
 * Adapted from Project-Agnostic, Format-Specific  modified 5/1/2017
 */


public  class CreateSpecialContentHTML extends HTMLContentCreator
{
    /*
     * for the HTML format, we flow out all the HTML files
     * needed.
     *
     * There are 4:
     *
     * a) cover (which has main cover image in it)
     * b) title page (title/author only)
     * c) front matter (all the title/author, etc)
     * d) preface (specific to HTML
     *
     */
    private static final String TITLE_HTML_FILE = "title_page.html";
    private static final String COVER_HTML_FILE = "cover_page.html";
    private static final String FRONT_HTML_FILE = "front_page.html";
    private static final String PREFACE_HTML_FILE = "preface_page.html";
    private static final String g_file_extensionq = ".html\"";
    private static final String g_file_extension = ".html";
    
    Map static_header_object = null;
    Map title_page_object = null;
    Map cover_page_object = null;
    Map front_page_object = null;
    Map preface_page_object = null;
    Map table_of_contents_object = null;

	
    /*
     * repository of options that drive this object
     * created in the getMetaData method AFTER instantiation
     */
    Properties options = null;

    /*
     * project-specific strings that are to be replaced
     *
     * key = string to search for in boilerplate
     * 
     * value = ReplacementString object that has the
     *   replacement string, and a flag to indicate special processing
     */
    TreeMap project_keys = null; 

	/*
	 * taken from the JSON that is unique to this object.
	 * 
	 * we will process this later and populate the keys
	 */
    Map boilerplate_object = null; // similar to other objects in JSON
    
// just for HTML
    String[] special_keys = {
    	"PROJECT_FRONT_MATTER", // 0
//      "PROJECT_TITLE" should NEVER be used here
    	"PROJECT_KEYWORDS", // 1
    	"PROJECT_COPYRIGHT" // 2
    };
    
    public CreateSpecialContentHTML() throws Exception
    {
        /*
         * read in the data that is used by this object
         */
        Map<String,Object> userData = 
		BookUtils.readJSON(this.getClass().getName(),false); // no debug
/*
 * OOPS, have chicken-egg problem. key values depends on
 * the type of HTML we are making (e.g. POEM vs SKELETON)
 * Don't know the name until later. Must save structure and work
 * with it later.
 */
		// run local version later when we know what kind of HTML, populate_key_values (mapper); // let base class handle the boilerplate
	boilerplate_object = (Map)userData.get("boilerplate"); // save for later
/* System.err.println("userData: " +userData);
System.err.println("Title Page: " + userData.get("title_page"));
*/
            static_header_object = (Map)userData.get("static_header"); // has entries for POEM, Vanilla (both types), and SKEL
            title_page_object = (Map)userData.get("title_page"); // has entries for POEM, VAN (ONE type), and SKEL
            cover_page_object = (Map)userData.get("cover_page"); // has entries for POEM, VAN (ONE type), and SKEL
            front_page_object = (Map)userData.get("front_page"); // has entries for POEM, VAN (ONE type), and SKEL
            preface_page_object = (Map)userData.get("preface_page"); // has entries for POEM, VAN (ONE type), and SKEL
            table_of_contents_object = (Map)userData.get("table_of_contents"); // has entries for POEM, VAN (ONE type), and SKEL
            /*
             * JSON objects all appear to be just fine, we will unpeel
             * them when they are needed for output
             */
    } // end instantiation
    
    /*
     * Build 4 separate pages
     * a) cover
     * b) title
     * c) front
     * d) preface
     *
     */
    public void createTitlePage(Object out, Object notused) throws Exception
    {
        /*
         * make the single pages
         * FOR NOW, they are individual HTML files.
         * once I figure out how to do a "single page" system, they will
         * be created as one stream (see Kindle creation for a model)
	 *
	 * WARNING: here we are using the createAPage method from BookUtils, NOT the method
	 *   embedded in this object (createSpecialContentHTML)
         */
	String search_for = getFormat();
	if (g_options.wantCoverPage())
	{
		BookUtils.createAPage(COVER_HTML_FILE, (List)cover_page_object.get(search_for),true); // close after use
	}
	if (g_options.wantTitlePage())
	{
		BookUtils.createAPage(TITLE_HTML_FILE, (List)title_page_object.get(search_for),true); // close after use
	}
	if (g_options.wantFrontMaterialPage())
	{
		BookUtils.createAPage(FRONT_HTML_FILE, (List)front_page_object.get(search_for),true); // close after use
	}
	if (g_options.wantPrefacePage())
	{
		BookUtils.createAPage(PREFACE_HTML_FILE, (List)preface_page_object.get(search_for),true); // close after use
	}
        
    } // end create title page (and more)
 
    public void createMetadata(Object out) throws Exception
    {
        // NO metadata right now, the object is null
      //  PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK
        
    } // end create metadata
    
	/*
	 * create the static headers, making inline changes as needed, based
	 * on PROJECT option specifications
	 *
	 * NOTE: unlike other boilerplate, there are entries in the static header
	 * JSON for VANILLA in both "light" (default) and "dark" modes. We have to
	 * treat the option setting a bit differently than for the other boilerplate
	 * JSON objects.
	 * 
	 * TOC content will be passed as the second
	 * object
	 * Auxiliary metadata will be passed as third object
	 * this is added to the keywords and description areas
	 */
    public void createStaticHeaders(Object out,
	Object toc_content,
	AuxiliaryInformation aux) throws Exception
    {
	String more_description = "";
	String more_keywords = "";
	String replacement_title = null;
	if (aux instanceof AuxiliaryMetadata)
	{
		AuxiliaryMetadata met = (AuxiliaryMetadata)aux;
		more_description = met.description;
		more_keywords = met.keywords;
		replacement_title = met.title; // may be null
	}
	/*
	 * put TOC in as Javascript, which is handled by 
	 * "hide/show" element (this was experimental, it works)
	 */
        PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK
        Object someobject = null;
	String search_for = getFormat();
	if (search_for.equals("VAN"))
	{
		// set default as light
		// any other entry is taken as-is, including VANLIGHT and VANDARK
		search_for = "VANLIGHT";
	}
        Iterator ii = ((List)static_header_object.get(search_for)).iterator();
        while (ii.hasNext())
        {	
            someobject =  ii.next();
	String content = (String)someobject;
			if (content.equals("TTOCC"))
			{
				// SPECIAL CASE, make TOC
				// if toc is null, bypass this text
				if (toc_content != null)
				{
					createTOCPOEMJavascript(pr, (List)toc_content); // cast must work
				}
			}
			else
			{
				String content2 = checkForDescription(content,more_description);
				String content3 = checkForKeywords(content2,more_keywords);
				String content4 = checkForReplacementTitle(content3,replacement_title);
				pr.print(content4);
			}
	} // end loop
        /*
         * RIGHT AFTER the static headers, we add necessary tags for
         * open/close Table of Contents. The actual TOC content was
         * written as Javascript data before here...
	 */
	if (toc_content != null)
	{
		// appearance is different depending on the Vanilla mode
		if (search_for.equals("VANDARK"))
		{
			// for DARK ONLY (color is yellow)
			pr.println("\n<h3 id='top' style=' color: #ff0;' onclick='reverse_toc();'>");
			pr.println("<span style='font-size:75%;'>Table of Contents</span> <span style='font-size:50%;'>(Click to Open/Close)</span></h3>");
		}
		else
		{
			// everyone else (testing blue)
			pr.println("<h3 id=\"top\" style=' color: #00F;' onclick=\"reverse_toc();\">");
			//pr.println("Table of Contents <span style=\"font-size:50%;\">(Click to Open/Close)</span></h3>");
			pr.println("<span style='font-size:75%;'>Table of Contents</span> <span style='font-size:50%;'>(Click to Open/Close)</span></h3>");
		}

		/*
		 * Bottom links, breadcrumbs, etc are NOT needed 
		 * if only General index wanted
		 * 
		 * Following <div> is NEEDED to make the Javascript 
		 * work correctly for the table of contents!
		 * 
		 */

		pr.println("<div id=\"TOC_TOP\">");  // toc is written inside this div
		pr.println("</div> <!-- table area -->");  // by javascript
	}
        /*
not doing following
         * RIGHT AFTER the static headers, which include the "body" item,
         * we put an abbreviated table of contents
* TESTING, do it as Javascript
	if (search_for.equalsIgnoreCase("POEM"))
	{
		createTOCPOEM(pr, (List)toc_content); // cast must work
	}
	if (search_for.equalsIgnoreCase("SKEL"))
	{
		createTOCSKEL(pr, (List)toc_content);
	}
	// if NOT MATCH, problem         
         */
} // end make static headers, including table of contents

public String returnAdditionalString(String target,
	String content,String more_stuff)
{
	// example String target = "<meta name=\"keywords\" content=\";
	int target_length = target.length();
	int location = content.indexOf(target);
	if (location >= 0)
	{
		// return target, additional stuff, remainder
		return target + // starting string
			more_stuff + // add new stuff
			content.substring(target_length); // remainder
	}
	else
	{
		// nothing to alter, return original string
		return content;
	}
} // end check for additional HTML meta content to be inserted

public String checkForKeywords(String content,String more_keywords)
{
	return returnAdditionalString(
		"<meta name=\"keywords\" content=\"",
		content,more_keywords);
} // end check for additional keywords to be inserted

public String checkForDescription(String content,
		String more_description)
{
	return returnAdditionalString(
		"<meta name=\"description\" content=\"",
		content,more_description);
}

public String checkForReplacementTitle(String content,
		String new_title)
{
	if (new_title == null)
	{
		// no replacement
		return content;
	}
	else
	{
		int pos1 = content.indexOf("<title>");
		if (pos1 >= 0)
		{
			// replace title text
			String front = content.substring(0,pos1);
			String rest1 = content.substring(pos1+7); // rest after <title>
			int pos2= rest1.indexOf("<"); // start of </title>
			return
				front + // up to <title>
				"<title>" + // restore <title>
				new_title +  // new title
				rest1.substring(pos2); // to end of HTML line
		}
		else
		{
			// doesn't contain <title>
			return content;
		}
	} // end if there is a new title to replace old one
} // end check for replacement title
    
	/*
	 * SPECIAL version for HTML objects, as the inner
	 * structure is different than others
	 *
	 * NOTE: elsewhere in this object, we are using createAPage,
	 * but it is the BookUtils utility method, NOT the following.
	 */
        public void createAPage(String filename, List page_object)
	throws Exception
    {
	String search_for = getFormat();
        PrintWriter pr = new PrintWriter(new File(filename));
        Object someobject = null;
        Iterator ii = page_object.iterator();
        while (ii.hasNext())
        {	
            someobject =  ii.next();
            if (someobject instanceof List)
            {
		List arr = (List)someobject; // cast
		/*
		 * first string in array is format
		 */
		String the_type = (String)arr.get(0); // cast must work
		if (search_for.equalsIgnoreCase(the_type))
		{
			// we WANT to use this one, copy rest of strings
			int leng = arr.size();
			for (int inner = 1 ; inner < leng ; inner++)
			{
				pr.print(arr.get(inner));
			} // end loop
			pr.flush();
			pr.close(); 
			return;  // no more work to do
		} // end if we FOUND the desired type
            } 
		else
		{
            throw new Exception("Problems with JSON inside static_header: " + someobject.getClass().getName());
		}
        } // end loop on the string arrays in the object
            //  now if there is a fall-through, NO MATCH, problem
} // end create a page

	public void createTOCPOEMJavascript(PrintWriter pr,
	List all_toc) throws Exception
	{
		TOCEntry the_item_left = null;
		TOCEntry the_item_right = null;
		TOCEntry dummy_TOC = new TOCEntry( // needed for odd/even
					"",
					"");

		/*
		 * TOC structure is:
		 *	 the List contains TOCEntry objects
		 * which will be written as a list of strings in Javascript
		 */
        
		int tot = all_toc.size();
		if ((tot % 2) != 0)
		{
			// odd, we will just add a blank item BAD
			all_toc.add(dummy_TOC);
		}
		tot = all_toc.size();
 // recalculate, always even
		int half = tot / 2;  
		pr.println("<script type=\"text/javascript\">");
		pr.println("   var TOCArray = new Array(");
		// write as left-right pairs
		for (int inner = 0 ; inner < half ; inner++)
		{
			the_item_left = (TOCEntry)(all_toc.get(inner)); // left side toc item
			the_item_right = (TOCEntry)(all_toc.get(inner + half)); // right side toc item
			pr.println("\"" + the_item_left.link + "\",\"" + 
				the_item_left.toc_title + "\"," + // left col item
			"\"" + the_item_right.link + "\",\"" + 
				the_item_right.toc_title + "\","  // right col item
				);
		} // end loop on all items
		pr.println(");</script>");
	} // make TOC as javascript

    public void createTOCPOEM(PrintWriter pr,
	List all_toc) throws Exception
    {
		// TOC title heading first
        
                 pr.print("<h3 id=\"top\" >Table of Contents</h3>\n");

	TOCEntry the_item_left = null;
	TOCEntry the_item_right = null;

	/*
	 * TOC structure is:
	 *	 the List contains TOCEntry objects
	 * which will be written    in the 2-column layout
	 */
        
            /*
             * 2-column layout follows. 
             * for POEM, we use ordinary table
             */
        int tot = all_toc.size();
	if ((tot % 2) != 0)
	{
		// odd, we will just add a blank item BAD
		all_toc.add(" ");
	}
        tot = all_toc.size();
 // recalculate, always even
        int half = tot / 2;  
	pr.println("<table>");
	// write as left-right pairs
	for (int inner = 0 ; inner < half ; inner++)
	{
                the_item_left = (TOCEntry)(all_toc.get(inner)); // left side toc item
                the_item_right = (TOCEntry)(all_toc.get(inner + half)); // right side toc item
		pr.println("<tr><td>" +
                "<a href=\"" + the_item_left.link +
			"\">" + the_item_left.toc_title + "</a>" + // left col item
			"</td><td>" +
                "<a href=\"" + the_item_right.link +
			"\">" + the_item_right.toc_title + "</a>" + // right col item
			"</td></tr>");
	} // end loop on all items
	pr.println("</table>");
} // end make TOC using POEM
         
    public void createTOCSKEL(PrintWriter pr,
	List all_toc) throws Exception
	{
		// TOC title heading first
//            HTMLSink.fullWidthSkeleton(pr);
        
            fullWidthSkeleton(pr); // this is child should work
                 pr.print("<h3 id=\"top\" >Table of Contents</h3>\n");
//            HTMLSink.finishSkeleton(pr); // finish col and row only
            finishSkeleton(pr); // finish col and row only

	TOCEntry the_item = null;

	/*
	 * TOC structure is:
	 *	 the List contains TOCEntry objects
	 * which will be written    in the 2-column layout
	 */
        
            /*
             * 2-column layout follows. 
             * for 960, we passed each half in its own
             * flow. For skeleton, we do alternate entries.
             * so, 10 items, first row is item 1 and 6
             * second is 2 and 7, third is 3 and 8, etc, etc
             * NO HELPER for skeleton
             */
// no longer use            HTMLSink.make2Columns(pr,inner);
            int tot = all_toc.size();
            int half = tot / 2;  // will truncate, so 1 becomes 0, 2 becomes 1, 3 becomes 1, 4 becomes 2, etc
            /*
             * if the number of items is odd, we have to add one to the row count. we will not
             * print any trailing item, in this case. if the number of items is even, nothing needs
             * to be done
             */
            if ((tot & 1) != 0) // ok, figure out if odd (strange....)
            {
                half++;
            }
            for (int rownum = 0 ; rownum < half ; rownum++)
            {
                pr.println("<div class=\"row\">" + "<!-- row " + (rownum + 1) + " -->"); // one row per pair
                pr.print("<div class=\"five columns\" ");
                if (rownum == 0)
                {
                    pr.print("style=\"margin-top: 5%\""); // padding on first row only
                }
                pr.println(">"); // first column
                the_item = (TOCEntry)(all_toc.get(rownum)); // col item
                pr.println("<a href=\"" + the_item.link +
			"\">" + the_item.toc_title + "</a>"); // col item
                pr.println("</div><!-- first column -->");
                if ((rownum + half) < tot)
                {
                    // allow second column item
                    pr.print("<div class=\"five columns\" ");
                    if (rownum == 0)
                    {
                        pr.print("style=\"margin-top: 5%\""); // padding on first row only
                    }
                    pr.println(">"); // second column
                the_item = (TOCEntry)(all_toc.get(rownum + half)); // col item
                pr.println("<a href=\"" + the_item.link +
			"\">" + the_item.toc_title + "</a>"); // col item
                    pr.println("</div><!-- second column -->");
                }
                pr.println("</div>"  + "<!-- end row " + (rownum + 1) + " -->"); // one row per pair
            } // loop through half the items
} // end make TOC SKEL version
         
    public void createTOCVAN(PrintWriter pr,
	List all_toc) throws Exception
	{
		// TOC title heading first
//            HTMLSink.fullWidthSkeleton(pr);
        
            fullWidthVanilla(pr); // this is child should work
                 pr.print("<h3 id=\"top\" >Table of Contents</h3>\n");
//            HTMLSink.finishVanilla(pr); // finish col and row only
            finishVanilla(pr); // finish col and row only

	TOCEntry the_item = null;

	/*
	 * TOC structure is:
	 *	 the List contains TOCEntry objects
	 * which will be written    in the 2-column layout
	 */
        
            /*
             * 2-column layout follows. 
             * for 960, we passed each half in its own
             * flow. For vanilla, we do alternate entries.
             * so, 10 items, first row is item 1 and 6
             * second is 2 and 7, third is 3 and 8, etc, etc
             * NO HELPER for skeleton
             */
// no longer use            HTMLSink.make2Columns(pr,inner);
            int tot = all_toc.size();
            int half = tot / 2;  // will truncate, so 1 becomes 0, 2 becomes 1, 3 becomes 1, 4 becomes 2, etc
            /*
             * if the number of items is odd, we have to add one to the row count. we will not
             * print any trailing item, in this case. if the number of items is even, nothing needs
             * to be done
             */
            if ((tot & 1) != 0) // ok, figure out if odd (strange....)
            {
                half++;
            }
            for (int rownum = 0 ; rownum < half ; rownum++)
            {
                pr.println("<div class=\"row\">" + "<!-- row " + (rownum + 1) + " -->"); // one row per pair
                pr.print("<div class=\"five columns\" ");
// HERE HERE VANILLA
                if (rownum == 0)
                {
                    pr.print("style=\"margin-top: 5%\""); // padding on first row only
                }
                pr.println(">"); // first column
                the_item = (TOCEntry)(all_toc.get(rownum)); // col item
                pr.println("<a href=\"" + the_item.link +
			"\">" + the_item.toc_title + "</a>"); // col item
                pr.println("</div><!-- first column -->");
                if ((rownum + half) < tot)
                {
                    // allow second column item
                    pr.print("<div class=\"five columns\" ");
                    if (rownum == 0)
                    {
                        pr.print("style=\"margin-top: 5%\""); // padding on first row only
                    }
                    pr.println(">"); // second column
                the_item = (TOCEntry)(all_toc.get(rownum + half)); // col item
                pr.println("<a href=\"" + the_item.link +
			"\">" + the_item.toc_title + "</a>"); // col item
                    pr.println("</div><!-- second column -->");
                }
                pr.println("</div>"  + "<!-- end row " + (rownum + 1) + " -->"); // one row per pair
            } // loop through half the items
} // end make TOC VAN version
        
// HERE HERE do we need a different one for SKELETON?
        public void renderIndex(PrintWriter pr, List all, int level) throws Exception
        {
            //    System.out.println(all);
            /*
             * check type of object at the top of this List, given the
             * design it MUST be a Group, as are all in this highest
             * level list. Deeper down, some groups have only
             * entries, others have more groups under them
             */
            Object working = all.get(0); // type is very important
            if (working instanceof IndexEntry)
            {
                throw new Exception("Index Structure Wrong: " + working); // NOT HERE
            }
            else
            {
                if (working instanceof IndexGroup)
                {
                    switch (level)
                    {
                        case SIMPLE_INDEX:
                        {
                            /*
                             * make heading for the index page
                             */
                            pr.println("<!-- in simple_index code -->");
                            pr.println(gT("HEADER_ID1") +
                                       "_index" +
                                       gT("HEADER_ID2") +
                                       "Indexes" +
                                       gT("LINE_BREAK") +
                                       gT("LINE_BREAK") +
                                       gT("HEADER1_END"));
                            /*
                             * BREADCRUMBS needed for HTML
                             */
                            /*
                             * HOW do we know where to drop breadcrumbs? Well, the first-level
                             * objects will all be IndexGroup objects and we will make
                             * breadcrumbs for THEM ONLY (no recursion, etc, etc)
                             */
                            /*
                             * Loop through all in the list, creating breadcrumbs
                             */
                            Iterator ingrp = all.iterator();
                            IndexGroup inner_group = null;
                            
                            /*
                             * we have not put a marker at the top of this
                             * web page, because the name of the page
                             * should be enough to get from other pages.
                             * HOWEVER, we may want one for the "top" indicators!?!!?
                             */
                            pr.print(gT("PARAGRAPH_START")); // treat real breadcrumb style
                            while (ingrp.hasNext())
                            {
                                inner_group = (IndexGroup)ingrp.next(); // MUST WORK or throw exception for bad structure
                                pr.print(gT("INDEX_CRUMB1") +
                                         inner_group.id +
                                         gT("INDEX_CRUMB2") +
                                         inner_group.short_title + " -- " +
                                         gT("INDEX_CRUMB_END")); 
                            } // end if putting in bread crumb for a particular top-level group
                            pr.print(gT("PARAGRAPH_END")); // treat real breadcrumb style

                            /*
                             * NOW, we create the index, based on this structure
                             */
                            break;
                        } // end simple case (state for HTML, might be different for other types)
                        case POPUP_INDEX:
                        {
                            /*
                             * make heading for the index page
                             */
                            pr.println(gT("HEADER_ID1") +
                                       "_index" +
                                       gT("HEADER_ID2") +
                                       "Indexes" +
                                       gT("LINE_BREAK") +
                                       gT("LINE_BREAK") +
                                       gT("HEADER1_END"));
                            /*
                             * BREADCRUMBS needed for HTML
                             */
                            /*
                             * HOW do we know where to drop breadcrumbs? Well, the first-level
                             * objects will all be IndexGroup objects and we will make
                             * breadcrumbs for THEM ONLY (no recursion, etc, etc)
                             */
                            /*
                             * Loop through all in the list, creating breadcrumbs
                             */
                            Iterator ingrp = all.iterator();
                            IndexGroup inner_group = null;
                            
                            /*
                             * we have not put a marker at the top of this
                             * web page, because the name of the page
                             * should be enough to get from other pages.
                             * HOWEVER, we may want one for the "top" indicators!?!!?
                             */
                            pr.print(gT("PARAGRAPH_START")); // treat real breadcrumb style
                            while (ingrp.hasNext())
                            {
                                inner_group = (IndexGroup)ingrp.next(); // MUST WORK or throw exception for bad structure
                                pr.print(gT("INDEX_CRUMB1") +
                                         inner_group.id +
                                         gT("INDEX_CRUMB2") +
                                         inner_group.short_title + " -- " +
                                         gT("INDEX_CRUMB_END")); 
                            } // end if putting in bread crumb for a particular top-level group
                            pr.print(gT("PARAGRAPH_END")); // treat real breadcrumb style
                            pr.print(gT("PARAGRAPH_START")); // add link for closing window

                            pr.print(gT("POPUP_CLOSE"));
// was "<a href=\"javascript:self.close()\">Close</a> the popup.");
                            pr.print(gT("PARAGRAPH_END")); 
                            /*
                             * NOW, we create the index, based on this structure
                             */
                            break;
                        } // end popup case 
                        case COMPLETE_INDEX:
                        {
				if (g_options.wantGeneralIndexONLY())
				{
					/* 
					 * don't need this breadcrumb page
					 * BUT do need a target for the "#index" link that is everywhere
					pr.println("<a name=\"index\"></a>");
					 */
				}
				else
				{
					/*
					 * make heading for the index page
					 * this page will contain breadcrumbs
					 */
					pr.println(gT("HEADER1_START") +
					"All Document Indexes" +
					gT("LINE_BREAK") +
					gT("LINE_BREAK") +
					gT("HEADER1_END"));
					/*
					 * BREADCRUMBS needed for HTML
					 */
				    /*
				     * Loop through all in the list, creating breadcrumbs
				     */
				    Iterator ingrp = all.iterator();
				    IndexGroup inner_group = null;
                            
				    /*
				     * we have not put a marker at the top of this
				     * web page, because the name of the page
				     * should be enough to get from other pages.
				     * HOWEVER, we may want one for the "top" indicators!?!!?
				     */
				    pr.print(gT("PARAGRAPH_START")); // treat real breadcrumb style
				    while (ingrp.hasNext())
				    {
					inner_group = (IndexGroup)ingrp.next(); // MUST WORK or throw exception for bad structure
					pr.print(gT("INDEX_CRUMB1") +
						 inner_group.id +
						 gT("INDEX_CRUMB2") +
						 inner_group.short_title + " -- " +
						 gT("INDEX_CRUMB_END")); 
				    } // end if putting in bread crumb for a particular top-level group
				    pr.print(gT("PARAGRAPH_END")); // treat real breadcrumb style
				    /*
				     * NOW, we create the index, based on this structure
				     */
				} // end else, various indexes wanted, not JUST general
                            break;
                        } // end complete index for HTML
                    } // end switch on the type of index wanted
                        
                    renderIndexGroupList(pr,all,1,new ArrayDeque(),level); // probably will recurse, no back to top yet
                } // end if the right kind of group
                else
                {
                    throw new Exception("Index Structure Wrong: " + working);
                }
            } // end not index entry
        } // end render the indexes into HTML
    
	/*
	 * create list entries that work correctly
	 * with higher-Latin characters such as tilde, and accented
	 * doing this for COMPLETE_INDEX output for HTML
	 */
        public void renderIndexList(PrintWriter pr, 
                                    List all, 
                                    boolean breadcrumbs,
                                    ArrayDeque to_top,
                                    int index_type) throws Exception
        {
		/*
		 * we must pass a format value to the 2column layout
		 */
		String format = getFormat();
		/*
		 * format is set to dummy, first. HOWEVER this causes strange errors later in the processing	
		 * BAD BOB
		 */
		int the_format = 999; // dummy to start	HOWEVER this causes strange errors later in the processing	
		if (format.equalsIgnoreCase("POEM"))
		{
			the_format = FORMAT_POEM;
		}
		if (format.equalsIgnoreCase("SKEL"))
		{
			the_format = FORMAT_SKELETON;
		}
		if (format.equalsIgnoreCase("VANLIGHT"))
		{
			the_format = FORMAT_VANILLA_LIGHT;
		}
		if (format.equalsIgnoreCase("VANDARK"))
		{
			the_format = FORMAT_VANILLA_DARK;
		}
            IndexEntry the_entry = null;
            ReturnInformation to_return = null;
            ArrayList all_items = new ArrayList(50);
            /*
             * ALL items in list are Index contents (no higher up container objects)
             */
            // debugSystem.out.println("\nHandling index list, size: " + all.size() + ", Top: " + to_top.size() + "\n"); //  + ", first item: " +  all.get(0));
            // System.out.flush();
            Iterator inds = all.iterator();
            /*
             * all items in the list will be rendered inside of a 2-column
             * layout
             */
            if (breadcrumbs)
            {
                StringBuffer bb = new StringBuffer();
                while (inds.hasNext())
                {
                    the_entry = (IndexEntry)inds.next(); // HAS TO BE THIS TYPE, otherwise bad design
                    if (the_entry.target == null)
                    {
                        // target is current xhtml file
                        bb.append(
                            gT("INDEX_CRUMB1") +
                            the_entry.id +    // id jump within current file
                            gT("INDEX_CRUMB2") +
                            the_entry.long_title +
                            gT("INDEX_CRUMB_END"));
                    }
                    else
                    {
                        bb.append(gT("INDEX_TARGET1") +
                                  the_entry.target + g_file_extension +
                                  gT("INDEX_TARGET2") +
                                  the_entry.id +    // id jump within target
                                  gT("INDEX_CRUMB2") +
                                  the_entry.long_title +
                                  gT("INDEX_CRUMB_END"));
                    }
                } // end for each item in list
                all_items.add(bb.toString()); // one long line
            } // end if combining items into one "line"
            else
            { // each item alone (two column)
                while (inds.hasNext())
                {
                    the_entry = (IndexEntry)inds.next(); // HAS TO BE THIS TYPE, otherwise bad design
                    // render each item alone
                    
                    if (the_entry.target == null)
                    {
                        // target is current xhtml file
                        all_items.add(
                            gT("INDEX_CRUMB1") +
                            the_entry.id +    // id jump within current file
                            gT("INDEX_CRUMB2") +
                            the_entry.long_title +
                            gT("INDEX_CRUMB_END"));
                    } // end there is NO target file
                    else
                    { // else there is a target file
                        switch (index_type)
                        {
                            case SIMPLE_INDEX:
                            case COMPLETE_INDEX:
                            {
                                all_items.add(gT("INDEX_TARGET1") +
                                 the_entry.target + g_file_extension +
                                 gT("INDEX_TARGET2") +
                                 BookUtils.eT(the_entry.id) +    // id jump within target
                                 // was the_entry.id +    // id jump within target
                                 gT("INDEX_CRUMB2") +
                                 BookUtils.eT(the_entry.long_title) +
                                 // was the_entry.long_title +
                                 gT("INDEX_CRUMB_END"));
                                break;
                            } // end non popup
                            case POPUP_INDEX:
                            {
                                all_items.add(
                                    gT("INDEX_TARGET1") +
                                    "javascript:change_parent('" +
                                    the_entry.target + g_file_extension +
                                    gT("INDEX_TARGET2") +
                                    the_entry.id +    // id jump within target
                                    "');\" title=\"" +
                                    the_entry.long_title +
                                    gT("INDEX_CRUMB2") +
                                    the_entry.long_title +
                                    gT("INDEX_CRUMB_END"));
                                break;
                            } // end  popup
                        } // end switch on index type
                    } // end if there is a target file
                } // else, render each item alone
            } // end single items in 2-col
            /*
             * end of list, do we add "top" navigation?
             * unpeel the stack (to_top) and see what there is
             */
            if (to_top != null)
            {
                Iterator tops = to_top.iterator();
                StringBuffer bbx = new StringBuffer();
         //       pr.print(gT("PARAGRAPH_START")); // back to top are shown as breadcrumbs
                while (tops.hasNext())
                {
                    to_return = (ReturnInformation)tops.next();
                    bbx.append(gT("INDEX_CRUMB1") +
                             to_return.id +
                             gT("INDEX_CRUMB2") +
                             "Back to Top (" + to_return.short_title + ") -- " +
                             gT("INDEX_CRUMB_END"));
                } // end for each item in stack
                /*
                 * now, a final return to top for all indexes
                 */
                switch (index_type)
                {
                    case SIMPLE_INDEX:
                    case POPUP_INDEX:
                    {
                        bbx.append(gT("INDEX_TARGET1") +
                                   "#_index" +
                                   gT("INDEX_CRUMB2") +
                                   "Back to Top (Indexes)" + // only state-level indexes
                                   gT("INDEX_CRUMB_END") +
                                   gT("PARAGRAPH_END"));
                        break;
                    } // end simple index
                    case COMPLETE_INDEX:
                    {
                        bbx.append(gT("INDEX_TARGET1") +
                                   "index.html" +
                                   gT("INDEX_CRUMB2") +
                                   "Back to Top (All Document Indexes)" + // full indexes for entire document
                                   gT("INDEX_CRUMB_END") +
                                   gT("PARAGRAPH_END"));
                        break;
                    } // end complete index
                } // end switch on index type
                all_items.add(bbx.toString()); // add the final line
                /*
                 * we use 2-column layout for most indexes, but
                 * for the popup facility, we are one column
                 */
                switch (index_type)
                {
                    case POPUP_INDEX:
                    {
                        make1Column(pr,all_items);
                        break;
                    }
                    case COMPLETE_INDEX:
                    case SIMPLE_INDEX:
                    {
                        make2Columns(pr,all_items,
//FORMAT_POEM);// we know we are POEM formatting (look at the name).
			// was set at start of this method
			the_format);
                        break;
                    }
                } // end switch on index type
                /*
                 * we are done with this level of the return stack, pop the
                 * top item out for the next pushdown to handle correctly
                 */
                to_top.pop(); // ignore return
            } // end if any return stack to display
        } // end render only a list of index items
                    
        public  void make1Column(PrintWriter pr, List items)
        {
            // everyone in one column, SPECIAL FOR POPUP FACILITY WINDOW, have to keep it narrow
            //
            pr.print("   <div class=\"row\">   <div class=\"three columns\" style=\"margin-top: 5%\">");
            
            Iterator ii = items.iterator();
            while (ii.hasNext())
            {
                pr.println("<p>" + ii.next() +
                           "</p>");
            }
        } // end make 1 column
        
        public void renderIndexGroupList(PrintWriter pr, 
                                         List all, 
                                         int level,
                                         ArrayDeque to_top,
                                         int index_type) throws Exception
        {
            IndexGroup inner_group = null;
            Object working = null;
            Iterator ingrp = all.iterator();
            while (ingrp.hasNext())
            {
                inner_group = (IndexGroup)ingrp.next(); // MUST WORK or throw exception for bad structure
                // debug System.out.println("Handling: " + inner_group.toStringBrief());
                /*
                 * depending on level of index, we MAY handle things differently
                 */
                //            if (level >= 2)
                //          {
                pr.print(gT("HEADER_ID1") +
                         inner_group.id +
                         gT("HEADER_ID2") +
                         inner_group.long_title +   
                         gT("HEADER1_END"));
                /*
                 * hmmm, if no objects below, we are done
                 */
                if (inner_group.children.size() == 0)
                {
                  throw new Exception("This Index Group EMPTY! " + inner_group.long_title);
                //    return;
                }
                /*
                 * now, check to see what kind of objects are the children
                 */
                working = inner_group.children.get(0);
                if (working instanceof IndexEntry)
                {
                    /*
                     * no breadcrumbs, we finish the full block, and the
                     * members of the list will be in a 2-column layout
			* (table)
                     */
                    to_top.push(inner_group.navigate_to_top); // push in the "back to top"
                    renderIndexList(pr,inner_group.children,
                                    inner_group.breadcrumbs,
                                    to_top,
                                    index_type); // this method pops up the item we just pushed in
                    // done, now go to next item in group list
                }
                else
                {
                    if (working instanceof IndexGroup)
                    {
                        /*
                         * more groups below, do we want to create breadcrumbs into
                         * those groups?
                         */
                        if (inner_group.breadcrumbs)
                        {
                            // heading already created, just process all groups (not their contents yet)
                            Iterator crumb = inner_group.children.iterator();
                            IndexGroup desired = null;
                            pr.print(gT("PARAGRAPH_START"));
                            while (crumb.hasNext())
                            {
                                working = crumb.next();
                                if (working instanceof IndexGroup)
                                {
                                    desired = (IndexGroup)working;
                                    pr.print(gT("INDEX_CRUMB1") +
                                             desired.id +
                                             gT("INDEX_CRUMB2") +
                                             desired.short_title +
                                             gT("INDEX_CRUMB_END"));
                                }
                                else
                                {
                                    throw new Exception("wrong structure: " + working);
                                }
                            } // end each crumb item
                            pr.print(gT("PARAGRAPH_END"));
                        } // end if breadcrumbs wanted
                        /*
                         * end of heading and possible breadcrumbs
                         */
                        // have to recurse
                        to_top.push(inner_group.navigate_to_top);
                        renderIndexGroupList(pr,inner_group.children,
                                             level + 1,
                                             to_top,
                                             index_type); // will recurse
                        /*
                         * we are done with this level of the return stack, pop the
                         * top item out for the next pushdown to handle correctly
                         */
                        to_top.pop(); // ignore return
                    }
                    else
                    {
                        throw new Exception("Wrong types mixed in index groups: " + working);
                    }
                }
                // call out bottom after having handled either another imbedded group or a list of index items
            } // end for each group in the list
        } // end traverse and render a group of indexes (recursive)
    
    
        public void renderBookmarks(PrintWriter pr, List all) throws Exception
        {
        }
        
        public void createTOC(PrintWriter pr, 
                              Map all_maps,
                              List appendixes,
                              Map index_flags) throws Exception
        {
        }
        
        public void endPageSeq(
            PrintWriter pr) throws Exception
        {
            pr.print("<p >\n");
// FOLLOWING LINK SEEMS UNNEXESSARY and it doesn't go anywhere
//            pr.print("<a href=\"index" + g_file_extension + "#state_listing\">All Indexes</a></p>\n"); // back to indexes on main web page
            pr.print("<a href=\"index" + g_file_extension + "#top\">All Indexes</a></p>\n"); // back to indexes on main web page
            pr.print("<p >\n");
            pr.print("<a href=\"#top\">Table of Contents</a></p>\n");
            pr.print(gT("LINE_BREAK"));
            pr.print(gT("LINE_BREAK"));
            pr.print(gT("LINE_BREAK"));
            pr.print(gT("LINE_BREAK"));
            pr.print("</p>\n");
            /*
             * finish the page per each format type
            pr.print("</div> <!-- skeleton container wrapper -->\n"); 
                        pr.print("</body>\n");
            pr.print("</html>\n");
             */
            pr.print(gT("SKELETON_PAGE_CLOSE")); // HERE VANILLA
        } // end endpageseq
    
        public void startFlow(
            PrintWriter pr,
            String page_number_string) throws Exception
        {
        }

	// new method uses helper in base class, SOMEWHAT
	public void modifyMetaData() throws Exception
	{
		/* 
		 * create and modify the Tree, and store
		 * the new version in our memory (not global)
		 * 
		 * the helper can also run a "generic" stringReplacer 
		 * on the list of List objects to be modified.
		 * 
		 * PROBLEM: we have a "special" stringReplacer
		 * SOLUTION: pass null to the helper, so it won't
		 * invoke a "generic" stringReplacer, then
		 * do the work ourselves.
		 * 
		 * NOTE: we are using a bunch of globals here 
		 * 
		 */
	String search_for = getFormat();
		project_keys = processMetaData(
			special_keys,
			null,false); // DONT let them run stringReplacer
			//null,true); // debugging on
System.out.println("in csHTML, project_keys: " + project_keys);
		String formatx = search_for; // need to do special processing on the static header ONLY
		if (formatx.equals("VAN"))
		{
			formatx = "VANLIGHT"; // default
		}
		stringReplacerCheck(static_header_object,formatx," in static_header"); // format could be VANLIGHT, VANDARK, SKEL, POEM
		/*
		 * shouldn't we be using the corrected format?
		stringReplacerCheck(title_page_object,search_for," in title_page");
		stringReplacerCheck(cover_page_object,search_for," in cover_page");
		stringReplacerCheck(front_page_object,search_for," in front_page");
		stringReplacerCheck(preface_page_object,search_for," in preface_page");
		stringReplacerCheck(table_of_contents_object,search_for," in table_of_contents");
		*/
		stringReplacerCheck(title_page_object,formatx," in title_page");
		stringReplacerCheck(cover_page_object,formatx," in cover_page");
		stringReplacerCheck(front_page_object,formatx," in front_page");
		stringReplacerCheck(preface_page_object,formatx," in preface_page");
		stringReplacerCheck(table_of_contents_object,formatx," in table_of_contents");
	} // end modifyMetaData

	/*
	 * wrapper for call to stringReplacer
	 * 
	 * check for missing grouping in the JSON, and throws an error we can deal with
	 * 
	 * NOTE: we are using a bunch of globals here 
	 * 
	 */
	public void stringReplacerCheck(Map from, String the_key, String narrative) throws Exception
	{
		Object test = from.get(the_key);
		if (test == null)
		{
			// throw something we understand
			throw new Exception("Cannot find " + the_key + narrative + ", probably missing or badly formatted.");
		}
		/*
		 * fall through if we found SOMETHING in the JSON
		 * This cast had BETTER work!
		 */
		stringReplacer((List)test,
			project_keys);  // use parent method (note use of global)
	}
	
	/*
	 * this code is unique to this format-specific object
	 * each switch() position is based on the special_key
	 * position.
	 * 
	 * return a String that will be pushed back into the
	 * JSON structure, as a complete replacement
	 * for the original string in which the special key was
	 * found
	 */
	public String specialReplacementProcessing(ReplacementString rval)
	{
		StringBuffer result = new StringBuffer();
		switch (rval.flag)
		{
			case 0:
			{
			    	// "PROJECT_FRONT_MATTER"
				result.append("<h1>"); // front is a heading
			    	String xx[] = rval.rep.split(":"); // 1 (one) colon delimiter
        			for (int inner  = 0 ; inner < xx.length ; inner++)
        			{
        				result.append(xx[inner] +
        					"<br/>" + "<br/>");
        				if (inner == 0)   // first line only
        				{
        					result.append("</h1>" +
						"<p style=\"text-align: center;\">" +
						"<br/>");

        				} // end first line only
				}  // end loop through all strings to be treated as separate lines         
				result.append("</p>");
			    	break;
			} // end 0 which is PROJECT_FRONT_MATTER
			case 1:
			{
			    	// "PROJECT_KEYWORDS"
			    	String xx[] = rval.rep.split(":"); // 1 (one) colon delimiter
        			for (int inner  = 0 ; inner < xx.length ; inner++)
        			{
        				result.append(xx[inner] +
        					","); // keywords are comma-delimited
				}  // end loop through all strings to be treated as separate lines         
				result.append("web"); // dummy for end
			    	break;
			} // end 1 which is PROJECT_KEYWORDS
			case 2:
			{
			    	// "PROJECT_COPYRIGHT"
				result.append( "<meta name=\"copyright\" content=\"" + 
			    	rval.rep + "\"/>");
			    	break;
			} // end 2 which is PROJECT_COPYRIGHT
		} // end switch on special code segments
		return result.toString();		
	}	// end specialReplacementProcessing
	
	/*
	 * get Options information from the project json file, as well
	 */
	public Object getMetaData(String json_file) throws Exception
	{
		Object result = getProjectSpecificMetaData(json_file); // use helper in base class
		/*
		 * NOW, read json file for HTML-specific! OPTIONS we will use
		 */
		options = BookUtils.getPropertiesFromJSON("options.json",
		"format_options"); 
		   /*
                 * NOW, we can process the boilerplate key-value pairs
                 * it is different than other objects, because there are levels
                 * unique to each HTML type
                 */
		String search_for = getFormat();
		// use parent method
                populate_key_values((List)boilerplate_object.get(search_for));
                //populate_key_values(getFormat());

		// now that we processed the options, return the metadata info
		return result;
	} // end getmetadata (overrides parent class)

	/*
	 * only property we process right now is format (more to be added)
	 */
	public String getProperty(String key)
	{
	    if (key.equals("HTML_FORMAT"))
		{
			return getFormat();
		}
		else
		{
			return null;
		}
	} // end getproperty
	/*
	 * check for FORMAT and supply matching string
	 * if missing, use POEM
	 * 
	 * uses the OPTIONS storage (not project storage) 
	 */
	public String getFormat()
	{
	    return options.getProperty("HTML_FORMAT","VANLIGHT"); // default is VANILLA light
	} // end get format string 

	/*
	 * get a replacement string from the PROJECT storage
	 * could be a null return
	 */
    public ReplacementString getProjectKeyValue(String xx) throws Exception
    {
	return (ReplacementString)project_keys.get(xx);
	}
}  // end special content for HTML output sinks
