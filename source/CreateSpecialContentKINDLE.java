import java.io.PrintWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayDeque;

// for JSON processing

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;


/*
 * modified 6/17/2018
 * Project-Agnostic, Format-Specific  created 4/22/2017
 * adding ability to pick TOC, preface page, index types, etc
 * using jackson for JSON
 */

/*
 *
 * Modified to handle high-Latin characters in
 * index entries, such as tilde, and accented
 */

public  class CreateSpecialContentKINDLE extends SpecialContentCreator
{
    /*
     * for the Kindle HTML format, we flow out all the HTML
     * needed. There is only ONE html page.
     *
     * There are 3 (was 4) components to the front matter:
     * OPTIONAL
     * x) start html (all boilerplate to start kindle HTML file)
     * b) title page (title/author only)
     * c) front matter (all the title/author, etc)
     * d) preface (specific to Kindle)
     *
     * NO LONGER USED: a) cover (which has main cover image in it)
     *
     * These HTML items live on a separate file, because
     * we have to insert a TOC section between them
     * and the body of the book.
     */

    private static final String CONTENT_OPF_FILE = "kindle_content.opf";
    private static final String TOC_NCX_FILE = "kindle_toc.ncx";
    
    List static_header_object = null;
    List page_head_object = null;
    List title_page_object = null;
    List start_html_object = null;
    List cover_page_object = null;
    List front_page_object = null;
    List preface_page_object = null;
    List table_of_contents_object = null;
    List content_opf_object = null;
    List toc_ncx_object = null;
      /*
     * project-specific strings that are to be replaced
     *
     * key = string to search for in boilerplate
     * 
     * value = ReplacementString object that has the
     *   replacement string, and a flag to indicate special processing
     */
    TreeMap project_keys = null; 
    
// just for HTML
    String[] special_keys = {
    	"PROJECT_FRONT_MATTER", // 0
//      "PROJECT_TITLE" should NEVER be used here
    	"PROJECT_KEYWORDS", // 1
    	"PROJECT_COPYRIGHT", // 2
    	//"PROJECT_COPYXXXXXXnotusedRIGHT", // 2 (sorry don't know why this was removed, it is now back)
    	"PROJECT_TOC_NCX_CONTENT", // 3
    	"PROJECT_EPUB_KEYWORDS", // 4, same as EPUB
    	"PROJECT_CONTENT_MANIFEST", // 5
    	"PROJECT_IMAGE_MANIFEST", // 6
    	"PROJECT_SPINE_CONTENTS" // 7
    };
    
    
    public CreateSpecialContentKINDLE() throws Exception
    {
        /*
         * read in the data that is used by this object
         */
        Map<String,Object> userData = BookUtils.readJSON(
		this.getClass().getName(), false);
        
        // userData is a Map containing the named arrays
	populate_key_values ((List)userData.get("boilerplate")); // let base class handle the boilerplate
            static_header_object = (List)userData.get("static_header");
            title_page_object = (List)userData.get("title_page");
            start_html_object = (List)userData.get("start_html");
            cover_page_object = (List)userData.get("cover_page");
            front_page_object = (List)userData.get("front_page");
            preface_page_object = (List)userData.get("preface_page");
            table_of_contents_object = (List)userData.get("table_of_contents");
            static_header_object = (List)userData.get("static_header");
            content_opf_object = (List)userData.get("kindle_content_opf");
            toc_ncx_object = (List)userData.get("kindle_toc_ncx");
            /*
             * JSON objects all appear to be just fine, we will unpeel
             * them when they are needed for output
             */
    } // end instantiation
    
    /*
     * all 3 (was 4) of the front matter items will be streamed to
     * a single output file that is later combined with
     * others for the input HTML.
     */
    public void createTitlePage(Object out, Object toc_items) throws Exception
    {
	/*
	 * various title page components are OPTIONAL
	 */
        // append "1" to filename as this is the first part
        PrintWriter pr = new PrintWriter(new File(KINDLESink.KINDLE_FILE +
                                                  "1.html"));
	// following REQUIRED
	BookUtils.createAPage(pr, start_html_object); // REQUIRED
//        createAPage(pr,cover_page_object, "cover page");
	if (g_options.wantTitlePage())
	{
		BookUtils.createAPage(pr, title_page_object); // , "title page");
	}
	if (g_options.wantFrontMaterialPage())
	{
		BookUtils.createAPage(pr, front_page_object); // , "Front matter");
	}
	if (g_options.wantPrefacePage())
	{
		createPrefacePage(pr, preface_page_object, "preface");
	}
        // making TOCNCX at this point is wrong, because we need actual link locations
 //       createTOCNCX(toc_items); // done only once
        createCONTENTOPF(toc_items); //  done only once
        pr.flush();
        pr.close(); // done with this separate file
	// NOTE, the file opened here for writing may be EMPTY, depending on desired front material
    } // end create title page
    
    /*
     * toc.ncx file 
     */
    public void createTOCNCX(Object toc_items) throws Exception
    {
	// look for marker "<!-- TOC placeholder PROJECT_TOC_NCX_CONTENT -->"
        //System.out.println("creating toc_ncx");
        PrintWriter pr = new PrintWriter(new File(TOC_NCX_FILE));
        /*
         * toc content here
         */
        Object someobject = null;
	/*
	 * BY THE TIME WE GET HERE, the toc_ncx_object
	 * should have been modified with various key values
	 * BUT not for the table of contents items, which are
	 * only known at this time. We watch for the marker.
	 */
        Iterator ii = ((List)toc_ncx_object).iterator();
        while (ii.hasNext())
        {	
            someobject =  ii.next();
            if (someobject instanceof String)
            {
                String content = (String)someobject;
		// look for marker "<!-- TOC placeholder PROJECT_TOC_NCX_CONTENT -->"
		if (content.indexOf("PROJECT_TOC_NCX_CONTENT") >= 0)
		{
                    /*
                     * pass through all TOC entries, DO NOT
                     * use any down to and including the Introduction.
                     * at end, we must insert the Indexes ourselves
                     */
                    List toc_elements = (List)toc_items; // this cast MUST WORK
                    TOCEntry the_entry = null;
                    // search for Introduction
                    int start_position = 0;
                    for (int inner2 = 0 ; inner2 < toc_elements.size() ; inner2++)
                    {
                        the_entry = (TOCEntry)toc_elements.get(inner2);
                        System.err.println("toc_elements: " +
                                           inner2 + " == " +
                                           the_entry);
                        if (the_entry.toc_title.equals("Introduction"))
                        {
                            start_position = inner2 + 1; // next after intro
                            break; // out of search loop
                        }
                    }
                  System.err.println("making TOC.NCX, start position is: " +
                                      start_position);
                    System.err.println("number of TOC elements: " +
                                       toc_elements.size());
                    int counter = 6; // for Kindle (start with 7 on EPUB
                    // only process these toc items
                    for (int inner3 = start_position ; inner3 < toc_elements.size() ; inner3++)
                    {	
                        the_entry = (TOCEntry)toc_elements.get(inner3);
                        pr.println("<navPoint id=\"navPoint-" +
                                   counter + "\" playOrder=\"" +
                                   counter + "\">");
                        pr.println("<navLabel>");
                        pr.println("<text>" +
                                   the_entry.toc_title +
                                   "</text>");
                        pr.println("</navLabel>");
                        pr.println("<content src=\"" +
                                   the_entry.link +
                                   "\" />");
                        pr.println("</navPoint>");
                        counter++;
			//	"\">" +
			//	"</a></p>");

                        //pr.println(the_entry); // debugging

                    } // end for each toc item
                    /*
                     * NOW, we manually add the index pointers
                     *
                     * NOTE, there are multiple indexes
                     * in Kindle, a single index.xhtml file for EPUB
                     
                     <navPoint id="navPoint-70" playOrder="70">
                     <navLabel>
                     <text>State Listing</text>
                     </navLabel>
                     <content src="kindleindex.html#_state_index" />
                     </navPoint>
                     <navPoint id="navPoint-71" playOrder="71">
                     <navLabel>
                     <text>City Index</text>
                     </navLabel>
                     <content src="kindleindex.html#_city_index" />
                     </navPoint>
                     <navPoint id="navPoint-72" playOrder="72">
                     <navLabel>
                     <text>General Index</text>
                     </navLabel>
                     <content src="kindleindex.html#_general_index" />
                     </navPoint>
                     <navPoint id="navPoint-73" playOrder="73">
                     <navLabel>
                     <text>Facility Index</text>
                     </navLabel>
                     <content src="kindleindex.html#_facility_index" />
                     </navPoint>
                     <navPoint id="navPoint-74" playOrder="74">
                     <navLabel>
                     <text>Index of Facilities with No Postal History</text>
                     </navLabel>
                     <content src="kindleindex.html#_facilityno_index" />
                     </navPoint>
                     
                     
                     
                     */
                    /* not used here
                    pr.println("<navPoint id=\"navPoint-" +
                               counter + "\" playOrder=\"" +
                               counter + "\">");
                    pr.println("<navLabel>");
                    pr.println("<text>Indexes</text>");
                    pr.println("</navLabel>");
                    pr.println("<content src=\"index.xhtml\" />");
                    pr.println("</navPoint>");
                    */
		} // special marker for TOC items
		else
		{
                    // not special, print it
                    pr.println(content);
		} // end not special marker
                continue; // done for now
            }
            //  now if there is a fall-through, the objects are somebody we don't know about
            throw new Exception("Problems with JSON inside toc_ncx: " + someobject.getClass().getName());
        } // end write the content to the stream
	/*
	 * Internal content of TOC are manifest items
	 */
	List toc_elements = (List)toc_items; // this cast MUST WORK
	TOCEntry the_entry = null;
	ii = toc_elements.iterator();
	while (ii.hasNext())
	{	
            the_entry = (TOCEntry)ii.next();
            //	pr.println("<p class=\"p_toc\" style = \"margin-left: 2em;\"><a href=\"" +
            //	the_entry.link +
            //	"\">" +
            //	the_entry.toc_title +
            //	"</a></p>");

            //pr.println(the_entry); // debugging

	}
        
        pr.flush();
        pr.close();
    } // end create toc.ncx file
    
    /*
     * content.opf file  NO IMAGES in the manifest for Kindle!
     */
    public void createCONTENTOPF(Object toc_items) throws Exception
    {
	// look for marker "<!-- TOC placeholder PROJECT_CONTENT_MANIFEST -->"
        //System.out.println("creating toc_ncx");
        PrintWriter pr = new PrintWriter(new File(CONTENT_OPF_FILE));
        /*
         * toc content here
         */
        Object someobject = null;
	/*
	 * BY THE TIME WE GET HERE, the content_opf_object
	 * should have been modified with various key values
	 * BUT not for the table of contents items, which are
	 * only known at this time. We watch for the marker.
	 */
        Iterator ii = ((List)content_opf_object).iterator();
        while (ii.hasNext())
        {	
            someobject =  ii.next();
            if (someobject instanceof String)
            {
                String content = (String)someobject;
		// look for marker "<!-- content placeholder PROJECT_CONTENT_MANIFEST -->"
		if (content.indexOf("PROJECT_CONTENT_MANIFEST") >= 0)
		{
                    /*
                     * pass through all TOC entries, DO NOT
                     * use any down to the Introduction.
                     * at end, we must insert the Indexes ourselves
                     */
                    List toc_elements = (List)toc_items; // this cast MUST WORK
                    TOCEntry the_entry = null;
                    // search for Introduction
                    // PROJECT_CONTENT_MANIFEST will contain lines like:
                    // <item id="item2" href="AL.xhtml" media-type="application/xhtml+xml" />
                    int start_position = 0;
                    for (int inner2 = 0 ; inner2 < toc_elements.size() ; inner2++)
                    {
                        the_entry = (TOCEntry)toc_elements.get(inner2);
                        if (the_entry.toc_title.equals("Introduction"))
                        {
                            start_position = inner2 + 1; // next after intro
                            break; // out of search loop
                        }
                    }
                    int counter = 2;
                    // only process these toc items
                    for (int inner3 = start_position ; inner3 < toc_elements.size() ; inner3++)
                    {	
                        the_entry = (TOCEntry)toc_elements.get(inner3);
                        // <item id="item2" href="AL.xhtml" media-type="application/xhtml+xml" />
                        
                        pr.println("<item id=\"item" +
                                   counter + "\" href=\"" +
                                   the_entry.link +
                                   "\" media-type=\"application/xhtml+xml\" />");
                                   
                        counter++;

                    } // end for each toc item
		} // special marker for TOC items
		else
		{
                    // not TOC items, check for cover image (NO OTHER IMAGES)
                    if (content.indexOf("PROJECT_COVER") >= 0)
                    {
                        /*
                         * the COVER IMAGE is not in the manifest
                         * as it is not referenced within the book body
                         * add a reference here
                         */
                        ReplacementString result = (ReplacementString)project_keys.get("PROJECT_COVER_IMAGE");
                        // if null, we have problems
                        pr.println("<item id=\"cover-image\" href=\"" +
                                   result.rep +
                                   "\" media-type=\"image/jpeg\" />");
                        
                    } // end if cover image wanted
                    else
                    {
                        // not TOC and not cover image, sooooo.....
                        // not special, just print it
                        pr.println(content);
                    } // end not TOC, or cover image
		} // end not special TOC marker
                continue; // done for now
            } // end if string
            //  now if there is a fall-through, the objects are somebody we don't know about
            throw new Exception("Problems with JSON inside content_opf: " + someobject.getClass().getName());
        } // end write the content to the stream
        pr.flush();
        pr.close();
    } // end create content.opf file
    
    /*
     * special for preface. Special  action
     * here, because the preface is two strings,
     * one for heading, the other for contents
     */
    private void createPrefacePage(PrintWriter pr,
                             List page_object,
                             String desc) throws Exception
    {
        Object someobject = null;
        List arr = (List)page_object;
        
        pr.print("<mbp:pagebreak /><h1 id=\"preface_page\">" + 
        arr.get(0) +  // no substitution
         "</h1>"); // first line of text is  heading
        // put in project-specific information
         String content = singleStringReplace((String)arr.get(1),
                project_keys); // content fetched from project, ready to split
                 
          String[] content_lines = content.split(":");
          for (int inner = 0 ; inner < content_lines.length ; inner++)
          {
	                pr.println("<p>" + content_lines[inner] + "</p>");
          }
                
/*
 * KLUGE -- put the top marker here for now, but we really want a table of contents!!
 */
        pr.print("<a name=\"top\"></a>\n");
    } // end special preface page
    
    
    
    public void createMetadata(Object out) throws Exception
    {
        
    } // end create metadata
    
    public void createStaticHeaders(Object out, Object notused,
		AuxiliaryInformation aux) throws Exception
    {
		createStaticHeaders(out,notused); // do not pass through aux
	}
    public void createStaticHeaders(Object out, Object notused) throws Exception
    {
        PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK
        
        pr.print("<mbp:pagebreak />"); // only "header" between sections is a new page marker for Kindle
        
    } // end make static headers
    

    public void makeTableOfContents(PrintWriter pr)
    {
    } // end make table of contents
    
    
    public void renderIndex_orig(PrintWriter pr, List all, int level) throws Exception
    {
    }
    
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
                        // already there                    HTMLSink.fullWidth960(pr); // full width layout
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

                        // HTMLSink.finish960(pr); // finish full-across block
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
                        // already there                    HTMLSink.fullWidth960(pr); // full width layout
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
                        // HTMLSink.finish960(pr); // finish full-across block
                        /*
                         * NOW, we create the index, based on this structure
                         */
                        break;
                    } // end popup case 
                    case COMPLETE_INDEX:
                    {
                        /*
                         * make heading for the index page, only if we have some content
                         */
			if (g_options.wantGeneralIndexONLY())
			{
				/*
				 * no need for index start page
				 * BUT, need "index" jump address
				 * empty name will work
				 */
				pr.println("<a name=\"index\"></a>");
			}
			else
			{
				// 
				pr.println(gT("HEADER_ID1") +
					   "index" +
					   gT("HEADER_ID2") +
					   "All Document Indexes" +
					   gT("LINE_BREAK") +
					   gT("LINE_BREAK") +
					   gT("HEADER1_END"));
				if (g_options.wantTOC())
				{
					// add a TOC reference, just to assist the reader
					pr.println(gT("INDEX_TARGET10") +
					   gT("INDEX_TARGET2") +
					   "TOC" +
					   gT("INDEX_CRUMB2") +
					   "Table of Contents" +
					   gT("INDEX_CRUMB10_END") +
					   gT("SEPARATOR_PARAGRAPH"));
				} // end if want toc
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
			} // end else want some sort of index besides general
			/*
                         * NOW, we create the index, based on this structure
                         */
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
    } // end render the indexes into FOP
    
	/*
 	 * work with high-Latin characters, such as tilde, and accent
	 */
    public void renderIndexList(PrintWriter pr, 
                                List all, 
                                boolean breadcrumbs,
                                ArrayDeque to_top,
                                int index_type) throws Exception
    {
        IndexEntry the_entry = null;
        ReturnInformation to_return = null;
        ArrayList all_items = new ArrayList(50);
        /*
         * ALL items in list are Index contents (no higher up container objects)
         */
        // debugging System.out.println("\nHandling index list, size: " + all.size() + ", Top: " + to_top.size() + "\n"); //  + ", first item: " +  all.get(0));
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
                        BookUtils.eT(the_entry.id) +    // id jump within current file
                        // was the_entry.id +    // id jump within current file
                        gT("INDEX_CRUMB2") +
                        BookUtils.eT(the_entry.long_title) +
                        // was the_entry.long_title +
                        gT("INDEX_CRUMB_END"));
                }
                else
                {
                    bb.append(gT("INDEX_TARGET1") +
                 // NOT USED             the_entry.target + g_file_extension +
                              gT("INDEX_TARGET2") +
                              BookUtils.eT(the_entry.id) +    // id jump within target
                              // was the_entry.id +    // id jump within target
                              gT("INDEX_CRUMB2") +
                              BookUtils.eT(the_entry.long_title) +
                              // was the_entry.long_title +
                              gT("INDEX_CRUMB_END"));
                }
            } // end for each item in list
            all_items.add(bb.toString()); // one long line
        } // end if combining items into one "line"
        else
        { // each item alone 
            while (inds.hasNext())
            {
                the_entry = (IndexEntry)inds.next(); // HAS TO BE THIS TYPE, otherwise bad design
                // render each item alone
                    
                if (the_entry.target == null)
                {
                    // target is current xhtml file
                    all_items.add(
                        gT("INDEX_CRUMB1") +
                        BookUtils.eT(the_entry.id) +    // id jump within current file
                        // was the_entry.id +    // id jump within current file
                        gT("INDEX_CRUMB2") +
                        BookUtils.eT(the_entry.long_title) +
                        // was the_entry.long_title +
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
                       // NOT USED                   the_entry.target + g_file_extension +
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
                   //             the_entry.target + g_file_extension +
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
        } // end single items 
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
                               "#index" +
                               gT("INDEX_CRUMB2") +
                               "Back to Top (All Document Indexes)" + // full indexes for entire document
                               gT("INDEX_CRUMB_END") +
                               gT("PARAGRAPH_END"));
                    break;
                } // end complete index
            } // end switch on index type
            all_items.add(bbx.toString()); // add the final line
          //  HTMLSink.make2Columns(pr,all_items);
            /*
             * the code above was adapted from HTML, which uses
             * a 2-column layout. We will just write all of them out.
             * The strings to be printed are in a List
             */
            Iterator alli = all_items.iterator();
            while (alli.hasNext())
            {
                pr.println(gT("PARAGRAPH_START") +
                           alli.next().toString() +
                           gT("PARAGRAPH_END"));
            }
            /*
             * we are done with this level of the return stack, pop the
             * top item out for the next pushdown to handle correctly
             */
            to_top.pop(); // ignore return
        } // end if any return stack to display
    } // end render only a list of index items
                        
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
            // debugging System.out.println("Handling: " + inner_group.toStringBrief());
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
                //      HTMLSink.finish960(pr);
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
                     * end of heading and possible breadcrumbs, we are done with full width block
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
                          List toc_list,
                          Map index_flags) throws Exception
    {
        /*
         * we KLUGE here. This is called after
         * the toc list is complete
         * only the 3rd paramter is used
         *
         * ALL OTHER params are null, and useless
         *
         * We are called at the end of processing
         */
          
    //    g_tit.createTOC(null, // no printwriter 
      //                  null, // no Map objects
        //                g_toc_list, // 3rd param is a List, good
          //              null); // last is a Map not used
               createTOCNCX(toc_list); 
        

    }
    
    public void endPageSeq(
        PrintWriter pr) throws Exception
    {
    }
    
    public void startFlow(
        PrintWriter pr,
        String page_number_string) throws Exception
    {
    }

	// new method uses helper in base class
	public void modifyMetaData() throws Exception
	{
		// create array of List s to be modified
		List [] to_process_list = new List[]
		{
		 title_page_object,
		 start_html_object,
		 front_page_object,
		 preface_page_object,
                toc_ncx_object,
		 content_opf_object
		};
	//	 stringReplacer(static_header_object,project_keys); // replace in static header
	//	 stringReplacer(cover_page_object,project_keys); // replace in cover page
   //		 stringReplacer(table_of_contents_object,project_keys); // replace in toc 
		/* 
		 * create and modify the Tree, and store
		 * the new version in our memory (not global)
		 * 
		 * the helper will also run stringReplacer on
		 * the list of List objects to be modified.
		 * 
		 */
		project_keys = processMetaData(
			special_keys,
			to_process_list,false);
	} // end modifyMetaData
    
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
                    case 3:
                    {
                        // "PROJECT_TOC_NCX_CONTENT"
                        /*
                         * we do not have all the data yet for
                         * the Table of Contents. So we
                         * have to put in a placeholder, and
                         * let the actual processor do the work
                         */
                        result.append("<!-- TOC placeholder PROJECT_TOC_NCX_CONTENT -->");
		
                        break;
                    } // end 3 which is PROJECT_TOC_NCX_CONTENT
                    case 4:
                    {
                        // "PROJECT_EPUB_KEYWORDS"
                        // SAME as kindle keywords, using dc:subject tag
                        String xx[] = rval.rep.split(":"); // 1 (one) colon delimiter
                        for (int inner  = 0 ; inner < xx.length ; inner++)
                        {
                            // each keyword is in a separate metadata tag
                            result.append("<dc:subject>" +
                                          xx[inner] + "</dc:subject>\n");
                            
                        }  // end loop through all strings that are separate keywords
                        break;
                    } // end 4 which is PROJECT_EPUB_KEYWORDS
                    case 5:
                    {
                        // "PROJECT_CONTENT_MANIFEST"
                        /*
                         * we do not have all the data yet for
                         * the Table of Contents. So we
                         * have to put in a placeholder, and
                         * let the actual processor do the work
                         */
                        result.append("<!-- CONTENTS placeholder PROJECT_CONTENT_MANIFEST -->");
		
                        break;
                    } // end 5 which is PROJECT_CONTENT_MANIFEST
                    case 6:
                    {
                        // "PROJECT_IMAGE_MANIFEST"
                        /*
                         * we do not have all the data yet for
                         * an image manifest. So we
                         * have to put in a placeholder, and
                         * let the actual processor do the work
                         */
                        result.append("<!-- CONTENTS placeholder PROJECT_IMAGE_MANIFEST -->");
		
                        break;
                    } // end 6 which is PROJECT_IMAGE_MANIFEST
                    case 7:
                    {
                        // "PROJECT_SPINE_CONTENTS"
                        /*
                         * we do not have all the data yet for
                         * spine contents. So we
                         * have to put in a placeholder, and
                         * let the actual processor do the work
                         */
                        result.append("<!-- CONTENTS placeholder PROJECT_SPINE_CONTENTS -->");
		
                        break;
                    } // end 7 which is PROJECT_SPINE_CONTENTS
		} // end switch on special code segments
		return result.toString();		
	}	// end specialReplacementProcessing
	
	
    public Object getMetaData(String json_file) throws Exception
    {
    		return getProjectSpecificMetaData(json_file); // use helper in base class
    }
        
    public void renderNOFChecklist(PrintWriter pr, Map nof_by_city) throws Exception
    {
    }

    public ReplacementString getProjectKeyValue(String xx) throws Exception
    {
		throw new Exception("Kindle sink does not use project values");
	}
        
} // end create special content KINDLE
