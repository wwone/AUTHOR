import java.io.PrintWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import java.util.Iterator;
import java.util.List;

/*
 * Updated 7/27/2018 
 * WORKING on getting headings back into the print
 * book. They were curtailed due to complexity.
 * 
 * Remove extra blank page after TOC (DONE)
 * TOC should NOT have links (DONE)
 * Updated 3/20/2018 change to Jackson
 * Updated 2/5 for column count specification
 * Project-Agnostic, Format-Specific  created 4/22/2017
 */

public  class CreateSpecialContentFOPPrint extends 
CreateSpecialContentFOPPDF
{
    /*
     *
     * Use parent object where possible. There are
     * some specific changes for making a PDF that is
     * PRINT-ONLY!
     *
     * for the FOP format, we flow out all the XML
     * 
     * This object creates a lot of content, since these
     * types of content are unique not only to the
     * format, but to the book being created.
     *
     * The front material pattern is:
     *
     * (NO COVER IMAGE!)
     *
     *  2) Title Page (simple name, author)
     *
     *  3) Front material (ISBN, edition, dates, etc)
     *
     *  4) Preface (format-specific text on its own
     *      page
     */

	public List m_project = null; // List with all strings from project.json
	public Properties m_proj = null; // accessable key/value pairs from project.json
    
    public CreateSpecialContentFOPPrint() throws Exception
    {
        super();
    }
	/*
	 * Override parent (which overrides more than that)
	 */
	public Object getMetaData(String json_file) throws Exception
	{
		m_project = getProjectSpecificMetaData(json_file); // use helper in base class
		/*
		 * NOW, read json file for PDF-specific! OPTIONS we will use
		 * (probably none for print PDF, but you never know)
		 */
		options = BookUtils.getPropertiesFromJSON("options.json",
		"format_options"); 

		    m_proj = new Properties();
		/*
		 * options are keyword then value
		 */
		List arr = m_project; // just use it
		int count= arr.size();
		for (int inner = 0 ; inner < count ; inner+=2)
		{
			String key = (String)arr.get(inner);
			String value = (String)arr.get(inner+1);
			    m_proj.setProperty(key,value); // strings
		} // end loop on string pairs
		/*
		 * AT THIS POINT, m_proj is a Properties object
		 * with all of the project-specific key/value pairs
		 */

		// now that we processed the options, return the metadata info
		return m_project; // not sure if anyone uses the return value...
	} // end getmetadata (overrides parent class)

    
    public void renderNOFChecklist(PrintWriter pr, Map nof_by_city) throws Exception
    {
    }

	/*
	 * override parent, similar trickiness is needed
	 * as with interactive PDF, but we are a PRINT output
	 */
    public void createTitlePage(Object out, Object notused) throws Exception
    {
        PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK

	// for printed PDF style, NO COVER PAGE 

	/*
	 * If no title,cover, or preface pages are wanted we DO NOT
	 * enclose them in a page-sequence!
	 * 
	 * ALL of these are OPTIONAL 
	 */
	if ( (g_options.wantPrefacePage()) ||
	 // (g_options.wantCoverPage()) || ignore the wants of caller/user
	 (g_options.wantTitlePage()) )
	{
		// START PAGE-SEQUENCE
		pr.print(gT("TITLE_PAGE_SEQ_FOP"));
		// no page number at bottom
		// no title with links at top
		// START FLOW
		pr.print(gT("FLOW_START_FOP"));
	}
	else
	{
		return; // NOTHING TO DO (don't make it any worse)
	}
        /*
	 * THESE ARE OPTIONAL
         * step 1: cover page NO NO NO 
         * step 2: title page and front material (BOTH)
         * step 3: preface for PDF (and print) users
         */
        /*
         * title_page_object is the "title_page" in JSON
         * as a set of strings, to be printed. If there is
         * an embedded array, the first entry in that
         * array is the FORMATTING code, the second contains
         * all the rest of the content, separated by colons (:)
         *  
         * "cover_page_object" on the other hand is just a simple String List  
         * IGNORED!
         */

        Object someobject = null;
        Iterator ii = null;

/*
 * IGNORE the wants of the user/caller
 * 
 * the parent reads in a "cover_page" object, but we 
 * do not use it, we don't make a cover page, even if someone
 * asked for it (!)
	if (g_options.wantCoverPage())
	{
		BookUtils.createAPage(pr,cover_page_object); // cover only
	}
 */

	if (g_options.wantTitlePage())
	{
		someobject = null;
		ii = title_page_object.iterator();
		while (ii.hasNext())
		{	
		    someobject =  ii.next();
		    if (someobject instanceof String)
		    {
			// print string, but replace project information first
			pr.print(singleStringReplace((String)someobject,
			project_keys));
			continue; // done for now
		    }
		    if (someobject instanceof List)
		    {
			List arr = (List)someobject; // got to cast it
			// NO substitution on first string
			String format1 = (String)arr.get(0); // first string is the formatting object for all the rest
			String content = singleStringReplace((String)arr.get(1),
			project_keys); // content fetched from project, ready to split
					 
			String[] content_lines = content.split(":");
			for (int inner = 0 ; inner < content_lines.length ; inner++)
			{
			    pr.print(format1);
			    pr.print(
				content_lines[inner] +  // already fetched from project info
			   "</fo:block>\n"); // one line of title page content
			}
			continue; // off we go
		    } // end if List
		    //  now if there is a fall-through, the objects are somebody we don't know about
		    throw new Exception("Problems with JSON inside title_page: " + someobject.getClass().getName());
		    
		} // end write the content to the stream
	}  // end if want title page
        /*
         * now, make the preface. we provide the formatting, the
         * JSON object provides text
         * FIRST text item is the page title/header
         * all remaining are individual paragraphs in the default format.
         */
	if (g_options.wantPrefacePage())
	{
		ii = preface_text_object.iterator();
		if (!ii.hasNext())
		{
		    throw new Exception("Problems with JSON in preface, missing starting text");
		}
		someobject =  ii.next();
		if (!(someobject instanceof String))
		{
		    throw new Exception("Problems with JSON in preface, starting text wrong: " +
					 someobject.getClass().getName());
		}
		String the_title = (String)someobject;
		pr.print("<fo:block page-break-before=\"always\" font-size=\"32pt\"\nfont-weight=\"bold\"\nspace-after.optimum=\"10pt\"\nspace-before.optimum=\"0\"\ntext-align=\"center\">" +
			 the_title +
			 gT("STATE_TITLE_BLOCK_FOP_END"));
        
		/*
		 * rest of items SHOULD be strings only, one to a paragraph block...
		 */
		while (ii.hasNext())
		{	
		    someobject =  ii.next();
		    if (someobject instanceof String)
		    {
	   //  testing           pr.print(
	     //                    "<fo:block   space-before=\"30pt\" >\n" +
			pr.print(gT("NORMAL_BLOCK_FOP") +
				 someobject +
				 gT("NORMAL_BLOCK_FOP_END"));
			continue; // done for now
		    }
		    //  now if there is a fall-through, the objects are somebody we don't know about
		    throw new Exception("Problems with JSON inside preface_text: " + someobject.getClass().getName());
		    
		} // end write the content to the stream
	} // end if want preface page

	/*
	 * based on logic above, SOMETHING got
	 * printed, title and/or preface
	 * SOOOO we must terminate the flow and page-sequence
	 */
        
        pr.print(gT("FLOW_END_FOP"));
        endPageSeq(pr); // end page-sequence for title/preface
    } // end create title page, and preface, if wanted
    
//    public void createMetadata(Object writer) throws Exception
	//{
//	}
    
	/*
	 * override parent, headers are a bit different with print
	 */
    public void createStaticHeaders(Object out, Object notused,
		AuxiliaryInformation aux) throws Exception
    {
		createStaticHeaders(out,notused); // no pass through of aux
	}

	/*
	 * override parent, we do things DIFFERENTLY for printed book
	 * 
	 * This matter has become quite complex, so we cannot 
	 * use the simple strings stored in the JSON file.
	 * Thus, this method hard-codes the FOP needed.
	 * 
	 * In the cases of body pages, the "the_state" value is
	 * embedded, so the header looks right. 
	 * 
	 * In the case of TOC and Introduction, no "the_state" value is
	 * set. Same issue with Indexes. Perhaps these can be
	 * set correctly, which might save a LOT of code.
	 */
    public void createStaticHeaders(Object out, Object notused) throws Exception
    {
        PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK
	/*
	 * top of page static stuff got too complex to work on.
	 * SO, the "the_state" name, right-justified will ONLY be 
	 * displayed. It is really a section sub-title
	 */
	pr.println("<!-- title printed at top of each page --><fo:static-content flow-name=\"xsl-region-before\">");
        
	// the value here gets filled in from the project.json file!
	//pr.println("<fo:block>PROJECT_TITLE " + m_project); 
	//pr.println("<fo:block>" + m_proj.get("PROJECT_TITLE") ); 
	pr.println("<fo:block text-align=\"end\" font-weight=\"bold\">");
	// NOT using non-breaking spaces between title and sub-title
	// FOLOWING WILL BE VARIABLE, but static 30 items for testing

	//pr.println("&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160; &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160; &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;");
	// most body pages work with "the_state" value
	// following will be right-aligned on ALL PAGES
	pr.println("<fo:retrieve-marker retrieve-class-name=\"the_state\"/>");
//	pr.println("TABLE OF CONTENTS"); force TOC in that area
//	pr.println("INDEX"); "the_state" must be actual index subtitle

	pr.println("</fo:block>\n</fo:static-content>");

    } // end create static headers

    
    public void renderIndexList(PrintWriter pr, List all) throws Exception
    {
        IndexEntry the_entry = null;
        /*
         * ALL items in list are Index contents (no higher up container objects)
         */
        // debugging System.out.println("Handling index list, size: " + all.size() + ", first item: " +
         // debugging                   all.get(0));
        // debugging System.out.flush();
        Iterator inds = all.iterator();
        while (inds.hasNext())
        {
            the_entry = (IndexEntry)inds.next(); // HAS TO BE THIS TYPE, otherwise bad design
            pr.print(gT("NORMAL_BLOCK_FOP_REST") +
             //        gT("LINK_FOP1") +
               //      the_entry.id +    NO LINKS, use page numbers
                 //    gT("LINK_END") +
                     the_entry.long_title +
                     "   (" +
                     gT("PAGE_NUMBER_LINK") +
                     the_entry.id +
                     gT("PAGE_NUMBER_LINK_END") +                         
")" +
                     gT("NORMAL_BLOCK_FOP_END"));
                     
                     
        }
    } // end render only a list of index items

    public void renderIndexGroupList(PrintWriter pr, List all, int level) throws Exception
    {
        IndexGroup inner_group = null;
        Object working = null;
        Iterator ingrp = all.iterator();
        while (ingrp.hasNext())
        {
            inner_group = (IndexGroup)ingrp.next(); // MUST WORK or throw exception for bad structure
            // debugging System.out.println("Handling Group: " + inner_group.long_title);
 // debugging            System.out.flush();
//            pr.print(gT("HEADING1_BLOCK_FOP") +
            /*
             * depending on level of index, we will do a page break.
             * Level 2 and DEEPER will not perform a page break 
             */
            if (level >= 2)
            {
		/*
		 * SORRY FOLKS, this formatting is just too complex
		 * to use the string repository. ALSO, we want to
		 * have the index name in the page heading...
		 */
		pr.print("<fo:block font-size=\"16pt\"\nfont-weight=\"bold\"\nspace-after.optimum=\"10pt\"\nspace-before.optimum=\"0\"\ntext-align=\"center\" id=\"" +
			inner_group.id +
			"\">\n" +
			// no marker for sub-heads "<fo:marker marker-class-name=\"the_state\">" +
//			inner_group.long_title +   // index name for page heading
//			"</fo:marker>\n" + // index title text follows
			inner_group.long_title +   // sub heading is the longer name with explanatory front, such as "facilities"
                         // short title does NOT include explanatory front, such as "cities"    inner_group.short_title +   // sub heading is the shorter name
			"</fo:block>\n");
            } // end if level requires new title, but no heading change
            else
            {
                /* this code causes a page break */
		pr.print("<fo:block page-break-before=\"always\" font-size=\"16pt\"\nfont-weight=\"bold\"\nspace-after.optimum=\"10pt\"\nspace-before.optimum=\"0\"\ntext-align=\"center\" id=\"" +
                         inner_group.id +
			"\">\n" +
			// marker is used ONLY for top-level titles, it becomes the header
			"<fo:marker marker-class-name=\"the_state\">" +
			inner_group.long_title +   // index name for page heading
			"</fo:marker>\n" + // index title text follows
                         inner_group.long_title + 
                         //                     inner_group.long_title + " [" + level + "] " +  // debugging for level number
			"</fo:block>\n");
            } // end if new page, new title, and new header
            /*
             * now, check to see what kind of objects are the children
             */
            working = inner_group.children.get(0);
            if (working instanceof IndexEntry)
            {
                renderIndexList(pr,inner_group.children);
                // done, now go to next item in group list
            }
            else
            {
                if (working instanceof IndexGroup)
                {
                    // have to recurse
                    renderIndexGroupList(pr,inner_group.children,level + 1); // will recurse
                }
                else
                {
                    throw new Exception("Wrong types mixed in index groups: " + working);
                }
            }
            // call out bottom after having handled either another imbedded group or a list of index items
       //     pr.print(gT("FLOW_END_FOP"));  // terminate the higher-level group 
        } // end for each group in the list
    } // end traverse and render a group of indexes (recursive)
    /*
    public void endPageSeq(
        PrintWriter pr)
    {
        pr.print("</fo:page-sequence>\n");
    }
    */
	/*
	 * override parent, flow is a little different with print
	 */
    public void startFlow(
        PrintWriter pr,
        String page_number_string) throws Exception
    {
	// left-right is used
        pr.print("<fo:page-sequence  initial-page-number=\"1\" id=\"N2528\" master-reference=\"psmOddEven\">\n");
        pr.print("<!-- page number at bottom --><fo:static-content flow-name=\"xsl-region-after\">\n");
        // inherit        pr.print("<fo:block text-align-last=\"center\" font-family=\"Helvetica\" color=\"gray\" font-size=\"10pt\">\n");
        pr.print("<fo:block text-align-last=\"center\" color=\"gray\" space-before.optimum=\"0\">\n");
        pr.print(page_number_string + " - <fo:page-number/>\n");
        pr.print("</fo:block>\n");
        pr.print("</fo:static-content>\n");
        createStaticHeaders(pr);
        pr.print("<fo:flow flow-name=\"xsl-region-body\">\n");
    } // end startFlow
    
    public void renderIndex(PrintWriter pr, List all, int level) throws Exception
    {
        //    System.out.println(all);
        pr.print(gT("FLOW_END_FOP")); // end current document flow
        endPageSeq(pr);
            
        startFlow(pr,"Index"); // start a new flow with different page number indicator
    /*   NOT SURE IF WE NEED A HEADING JUST FOR ALL INDEXES pr.print(gT("STATE_TITLE_BLOCK_FOP1") +
"_indexes" +
                 gT("STATE_TITLE_BLOCK_FOP2") +
"All Indexes" +
                 gT("STATE_TITLE_BLOCK_FOP_END")); // heading is marked for return, and is generic
                 */      
        
        /*
         * check type of object at the top of this List, given the
         * design it MUST be a Group, as are all in this highest
         * level list. Deeper down, some groups have only
         * entries, others have more groups under them
         */
        Object working = all.get(0); // type is very important
  //      System.out.println("Entering index renderer, object: " + working);
    //    System.out.flush();
        if (working instanceof IndexEntry)
        {
            throw new Exception("Index Structure Wrong: " + working); // NOT HERE
        }
        else
        {
            if (working instanceof IndexGroup)
            {
                renderIndexGroupList(pr,all,1); // may recurse
                // we are done having recursed through all groups
            } // end another group under the main one
            else
            {
                throw new Exception("Index Structure Wrong: " + working);
            }
        } // end not index entry
        /*
         * at this point, we have rendered all groups and entries under them, tree done
         */
//        pr.print(gT("FLOW_END_FOP"));  
    } // end render the indexes into FOP
    
    /*
     * no bookmarks with printed PDF, only printed index
     * pages (page number references only)
     */
    public void renderBookmarks(PrintWriter pr, List all) throws Exception
    {
    }

    /*
     * override parent
     * (original code taken from the interactive pdf create special content)
     * 
     * HOWEVER, we don't want the internal links, we use only 
     * page numbers with the print version
     */
    public void makeTableOfContents(PrintWriter pr,
                                    Map state_index_map,
                                    Map abbrev_map,
                                    Map city_index_map,
                                    Map fac_index_map,
                                    Map general_index_map,
                                    List appendixes,
                                    Map index_flags
                                    ) throws Exception
    {
	/*
	 * TOC is OPTIONAL. If we don't want, there should
	 * be NO output into the flow
	 */
	if (g_options.wantTOC())
	{
		startFlow(pr,"Table of Contents"); // start the TOC flow with page numbers that are different
		//startFlow(pr,"TOC"); // start the TOC flow
		pr.print(gT("STATE_TITLE_BLOCK_FOP1") +
	"_toc" +
			 gT("STATE_TITLE_BLOCK_FOP2") +
			 gT("MARKER_START_STATE_FOP") +
			 "Table of Contents" + // 'the_state' is 'Table of Contents', not 'TOC'
			 //"TOC" + was this, not really useful information
			 gT("MARKER_END_FOP") +
			"Table of Contents" +
			 gT("STATE_TITLE_BLOCK_FOP_END"));
		
		Iterator states = state_index_map.keySet().iterator();
		ExactReferenceComparable the_ref = null;
		List some_items = null;
		String index_item = "";
		/*
		 * first, the introduction
		 */
		pr.print(makeTOCLine(
			 "_intro", 
			"Introduction",""));
        
		while (states.hasNext())
		{
		    index_item = (String)states.next();  // state name
		    // ONLY ONE STATE PER ENTRY
		    some_items = (List)state_index_map.get(index_item);
		    the_ref = new ExactReferenceComparable((ExactReference)some_items.get(0)); // first item only
		    
		    // we are only processing state items
			pr.print(makeTOCLine(
			     "state_" + 
			     BookUtils.eC(the_ref.state_name),   // escape state name
			     index_item,""));

		} // end for each state toc item
        
		Iterator appit = appendixes.iterator();
		String app_name = "";
		String app_title = "";
		
		while (appit.hasNext())
		{
		    app_name = (String)appit.next();
		    app_title = (String)appit.next(); // bad boy!
			pr.print(makeTOCLine(
				app_name,
				app_title,""));
		} // end list all appendixes in TOC
		/*
		 * point to indexes. 
		 */
		// HERE the general index handling should allow for optional!
		if (g_options.wantGeneralIndex())
		{
		    pr.print(makeTOCLine(
			 gT("GENERAL_INDEX_ID") ,
			 gT("GENERAL_INDEX_TITLE"),
			"Index - " ));
		}
		if (index_flags.containsKey( "INDEX_STATES"))
		{
		    pr.print(makeTOCLine(
			     gT("STATE_INDEX_ID"),
			     gT("STATE_INDEX_TITLE"),
			"Index - " ));
		} // end if putting in TOC entry for state index
            
		if (index_flags.containsKey( "INDEX_CITIES"))
		{
		    pr.print(makeTOCLine(
			     gT("CITY_INDEX_ID"),
			     gT("CITY_INDEX_TITLE"),
			"Index - " ));
		} // end write TOC entry for city index
            
		if (index_flags.containsKey( "INDEX_FACILITIES"))
		{
		    pr.print(makeTOCLine(
			     gT("FACILITY_INDEX_ID"),
			     gT("FACILITY_INDEX_TITLE"),
			"Index - " ));
		} // end breadcrumb for facility index
            
		if (index_flags.containsKey( "INDEX_NO_POSTAL_HISTORY"))
		{
		    pr.print(makeTOCLine(
			     gT("FACILITY_NO_INDEX_ID"),
			     gT("FACILITY_NO_INDEX_TITLE"),
			"Index - " ));
		} // end TOC entry for no postal history facilities
        
        /*
         * We used to add a blank page after the TOC,
         * BUT the FOP system is now set up to use a 
         * "left-right" layout. Therefore, we no
         * longer add that page.
         *  
         */
	//pr.print(gT("BLANK_PAGE_FOP")); no longer used
        
	/*
	 * we created some block content above, because TOC
	 * was wanted. we need to terminate the flow, and allow for
	 * next part of the document.
	 */
		pr.print(gT("FLOW_END_FOP") + " <!-- end of flow for toc -->\n");
		endPageSeq(pr);
	} // end if want a TOC
    } // end make table of contents
    
   
	/*
	 * OVERRIDE parent
	 * only property we process right now is column count
	 */
	public String getProperty(String key)
	{
	    if (key.equals("PDF_COLUMN_COUNT"))
		{
			return "2"; // NOT SETTABLE for print (or should it be?)
			// return options.getProperty(key,"2"); // default 1
		}
		else
		{
			return null;
		}
	} // end getproperty

    public ReplacementString getProjectKeyValue(String xx) throws Exception
    {
		throw new Exception("FOP (print) sink does not use project values");
	}

	/*
	 * override parent
	 * 
	 * NO LINKS, as this is a printed version 
	 */
	public String makeTOCLine(String id, String narrative,
		String page_number_pre) throws Exception
	{
		return
			gT("NORMAL_BLOCK_FOP_REST") +
/*
NO LINKS
			gT("LINK_FOP1") +
			id +
			gT("LINK_FOP2") +
			narrative  +
			gT("LINK_END") +
*/
			narrative  +
			"   (" +
			page_number_pre + // can be empty string
			gT("PAGE_NUMBER_LINK") +
			id +
			gT("PAGE_NUMBER_LINK_END") +
			")" +
			gT("NORMAL_BLOCK_FOP_END");
	} // end make simple TOC line
    
} // end special output creator facilities  FOP format for PRINT (not PDF reader)

