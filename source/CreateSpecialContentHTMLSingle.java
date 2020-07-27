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
 * Modified 10/8/2019
 *
 * TODO: adding Vanilla to the layout systems available
 *
 * Work on making cover page, title page, etc OPTIONAL
 *
 * This is the "single page" layout, much
 * like what is done now for Kindle
 *
 * NOTE: single page format will probably not work well
 * with large,complex documents like the facility BOOK
 * probably better with articles and newsletters
 *
 * WARNING NOTE, etc, this is back to being a generic
 * HTML creator. It will read flags in the options.json file
 * to drive several features, such as:
 *
 * 1) POEM or SKELETON HTML output
 *
 * 2) Single HTML page, or one page per SECTION (or state, etc)
 *
 * 3) Flag whether we write all the FRONT material HTML files, such
 *    as cover page, title page, preface, etc
 *
 * NOTE: TOC as dynamic "click to see table of contents" does
 * NOT apply to the single page layout.
 *
 */

/*
 * Adapted from Project-Agnostic, Format-Specific  modified 5/1/2017
 */


public  class CreateSpecialContentHTMLSingle extends CreateSpecialContentHTML 
{

	public List g_toc_content = null; // must be filled in before use!

	public boolean g_static_header_written = false;

    public CreateSpecialContentHTMLSingle() throws Exception
    {
	super();
    } // end instantiation
        
	/*
	 * override parent, no indexes, jump provided to top
	 */
        public void endPageSeq(
            PrintWriter pr) throws Exception
        {
            pr.print("<p >\n");
            pr.print("<a href=\"#top\">Top</a></p>\n");
            pr.print(gT("SKELETON_PAGE_CLOSE"));
        } // end endpageseq
	/*
	 * override parent
	 * no TOC at top
	 * only ONE static header at the top of entire output
	 */
	public void createStaticHeaders(Object out,
		Object toc_content,
		AuxiliaryInformation aux) throws Exception
	{
		createStaticHeaders(out,toc_content); // do not pass through aux
	}
	public void createStaticHeaders(Object out,
		Object toc_content) throws Exception
	{
		/*
		 * we don't do anything with TOC content
		 * when static headers built for single page
		 * 
		 * STORE it away for future use (at Index area) 
		 * unless null, then no storage
		 */
		if (toc_content != null)
		{
			g_toc_content = (List)toc_content; // cast must work
		}
// debuggingSystem.err.println("createStaticHeaders called, out: " + out);
		if (g_static_header_written)
		{
			// nothing more to do
		}
		else
		{
			g_static_header_written = true; // only ONCE
			PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK
			Object someobject = null;
			String search_for = getFormat();
			String formatx = search_for; // need to do special processing on the static header ONLY
			if (formatx.equals("VAN"))
			{
				formatx = "VANLIGHT"; // default
			}
			Iterator ii = ((List)static_header_object.get(formatx)).iterator();
			while (ii.hasNext())
			{	
			    someobject =  ii.next();
			String content = (String)someobject;
					if (content.equals("TTOCC"))
					{
						// SPECIAL CASE, make TOC
// IGNORE for now, no TOC at top of the main page (where then?)						createTOCPOEMJavascript(pr, (List)toc_content); // cast must work
					}
					else
					{
						pr.print(content);
					}
			} // end loop on static content
		} // end if want to make the single copy of static header content
	} // end make static headers, no table of contents

	/*
	 * override parent. If any of the static start pages
	 * (title, cover) are wanted, we write all start boilerplate
	 * FIRST, and do NOT repeat it
	 */
    public void createTitlePage(Object out, Object notused) throws Exception
    {
        /*
         * make the single pages on the main HTML file
         */
	PrintWriter pr = (PrintWriter)out; // cast must work
	String search_for = getFormat();
	String formatx = search_for; // need to do special processing on the static header ONLY
	if (formatx.equals("VAN"))
	{
		formatx = "VANLIGHT"; // default
	}
	if (g_options.wantCoverPage())
	{
		// first, make boilerplate if have not done so
		createStaticHeaders(pr,null);
		// now, make cover page content
		BookUtils.createAPage(pr, 
		(List)cover_page_object.get(formatx)); 
	}
	if (g_options.wantTitlePage())
	{
		// first, make boilerplate if have not done so
		createStaticHeaders(out,null);
		// now, make title page
		BookUtils.createAPage(pr, 
		(List)title_page_object.get(formatx)); 
	}
	if (g_options.wantFrontMaterialPage())
	{
		// first, make boilerplate if have not done so
		createStaticHeaders(out,null);
		// make front material on same output file
		BookUtils.createAPage(pr, 
		(List)front_page_object.get(formatx));
	}
	if (g_options.wantPrefacePage())
	{
		// first, make boilerplate if have not done so
		createStaticHeaders(out,null);
		// create preface material
		BookUtils.createAPage(pr, 
		(List)preface_page_object.get(formatx));
	}
        
    } // end create title page (and more)

	/*
	 * override parent
	 * ALL INDEX LINKS are relative to the current page
	 * NO FILE NAMES
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
		int the_format = 999; // dummy to starto		
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
		/*
			ALL REFERENCES ARE LOCAL to the single page
                    if (the_entry.target == null)
                    {
			}
		 */
                        // target is current xhtml file
                        bb.append(
                            gT("INDEX_CRUMB1") +
                            the_entry.id +    // id jump within current file
                            gT("INDEX_CRUMB2") +
                            the_entry.long_title +
                            gT("INDEX_CRUMB_END"));
/*
 * NO FILE REFERENCES
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
*/
                } // end for each item in list
                all_items.add(bb.toString()); // one long line
            } // end if combining items into one "line"
            else
            { // each item alone (two column)
                while (inds.hasNext())
                {
                    the_entry = (IndexEntry)inds.next(); // HAS TO BE THIS TYPE, otherwise bad design
                    // render each item alone
                    
/*
 * NO FILE references
                    if (the_entry.target == null)
                    {
 */
                        // target is current xhtml file
                        all_items.add(
                            gT("INDEX_CRUMB1") +
                            the_entry.id +    // id jump within current file
                            gT("INDEX_CRUMB2") +
                            the_entry.long_title +
                            gT("INDEX_CRUMB_END"));
/*
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
*/
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
                                   "#index" + // SAME PAGE in single output
                                   //"index.html" +
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

	/*
	 * override parent
	 */
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
					 */
					pr.println("<a name=\"index\"></a>");
				}
				else
				{
					/*
					 * make heading for the index page
					 * this page will contain breadcrumbs
					 * 
					 * we need a target for the "#index" link that is everywhere
					 */
					pr.println(gT("HEADER_ID1") +
					"index" +
					gT("HEADER_ID2") +
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
				/*
				 * HERE we make a TOC, if it is wanted
				 */
				if (g_options.wantTOC())
				{
					String search_for = getFormat();
					if (search_for.equalsIgnoreCase("POEM"))
					{
						createTOCPOEM(pr, g_toc_content); 
					}
					if (search_for.equalsIgnoreCase("SKEL"))
					{
						createTOCSKEL(pr, g_toc_content);
					}
					/*
					 * DO THE FOLLOWING need to be separate methods? Or is a generic
					 * "createTOCVAN" good enough?
					 */
					if (search_for.equalsIgnoreCase("VANDARK"))
					{
						createTOCVAN(pr, g_toc_content);
					}
					if (search_for.equalsIgnoreCase("VANLIGHT"))
					{
						createTOCVAN(pr, g_toc_content);
					}
				} // end if want TOC included
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
	 * override parent
	 * 
	 * special version of TOC for single page 
	 */
    public void createTOCPOEM(PrintWriter pr,
	List all_toc) throws Exception
    {
		// TOC title heading first, not at top...
        
                 pr.print("<h3  >Table of Contents</h3>\n");
                 //pr.print("<h3 id=\"top\" >Table of Contents</h3>\n");

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
		// odd, we will just add an empty item
		all_toc.add(new TOCEntry("",""));
		// BAD BAD BAD all_toc.add(" ");
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
         
	/*
	 * override parent
	 * 
	 * special version of TOC for single page 
	 */
    public void createTOCSKEL(PrintWriter pr,
	List all_toc) throws Exception
	{
		// TOC title heading first
//            HTMLSink.fullWidthSkeleton(pr);
        
            fullWidthSkeleton(pr); // this is child should work
                 pr.print("<h3  >Table of Contents</h3>\n");
                 //pr.print("<h3 id=\"top\" >Table of Contents</h3>\n");
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
         
	/*
	 * override parent
	 * 
	 * special version of TOC for single page 
	 */
    public void createTOCVAN(PrintWriter pr,
	List all_toc) throws Exception
	{
		// TOC title heading first
//            HTMLSink.fullWidthSkeleton(pr);
        
            fullWidthVanilla(pr); // this is child should work
                 pr.print("<h3  >Table of Contents</h3>\n");
                 //pr.print("<h3 id=\"top\" >Table of Contents</h3>\n");
//            HTMLSink.finishSkeleton(pr); // finish col and row only
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
        //NEEDS WORK HERE
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
} // end make TOC VAN version

    public ReplacementString getProjectKeyValue(String xx) throws Exception
    {
		throw new Exception("HTML (single) sink does not use project values");
	}
    
}  // end special content for HTML output sinks
