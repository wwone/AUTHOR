import java.io.PrintWriter;
import java.io.InputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


// for JSON processing

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/*
 * updated 5/29/2018
 * 
 * Use simple "right" page layout, rather than fancy odd-even
 * (printed version will have to use odd-even)
 * 
 * Use Jackson for JSON processing 
 * 
 * Project-Agnostic, Format-Specific  created 4/22/2017
 */

public  class CreateSpecialContentFOPPDF extends SpecialContentCreator
{
    /*
     * CURRENT NOTES:
     *
     * o) the bookmarks on the left side should start with the
     *    initial letter, because PDF viewers cut off the bookmarks 
     *    after only about 10 letters. much depends on the PDF viewer.
     *
     *
     * o) add NO POSTAL history listing to the end of the NOF.
     *    SAME for Print PDF
     *
     * o) KEYWORDS do not seem to get through to the metadata. 
     *    I can always post-insert them, but what the??
     *
     *    Looking at the OPF for EPUB, the entry seems to be dc:subject
     *    I see that listed in the OPF standard, keywords live there, BUT
     *    FOP Metadata writeup suggests keywords in a "pdf:Keyword" namespace
     *    no additional info. I don't know how to specify, have to search
     *    discussion group and/or help for FOP at Apache. (multiple dc:subject
     *    entries did not really help).
     *
     *    SAME ISSUE with Print PDF
     *
     *
     *
     * o) Experiment with single-column is SUCCESSFUL, make it the default,
     *    but allow setting for 2-column, the JSON is pretty simple. This does
     *    NOT apply to printed PDF. (1-column was over 1000 pages)
     *    LAYOUT_MASTER_FOPnnn are the boilerplate items
     *
     * The front material pattern is:
     *
     *  1) Front Cover (just an image)
     *
     *  2) Title Page (simple name, author)
     *
     *  3) Front material (ISBN, edition, dates, etc)
     *
     *  4) Preface (format-specific text on its own
     *      page
     */
    
    public List static_header_object1 = null;
    public List static_header_object2 = null;
    public List page_head_object = null;
    public List title_page_object = null;
    public List cover_page_object = null;
    public List preface_text_object = null;
    
        String[] special_keys = {
//    	"PROJECT_FRONT_MATTER" // must split into single lines
		"XX_TT_SS" // do nothing special, let title page handler do it
    	    	    };

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
     * repository of options that drive this object
     * created in the getMetaData method AFTER instantiation
     */
    Properties options = null;
    
    public CreateSpecialContentFOPPDF() throws Exception
    {
        /*
         * read in the data that is used by this object
         */
        Map<String,Object> userData = BookUtils.readJSON(
		this.getClass().getName(), false);
        
        // userData is a Map containing the named arrays
        
            populate_key_values ((List)userData.get("boilerplate")); // let base class handle the boilerplate
            static_header_object1 = (List)userData.get("static_header1");
//System.err.println("static_header_object1: " + static_header_object1);
            static_header_object2 = (List)userData.get("static_header2");
            page_head_object = (List)userData.get("page_head");
            title_page_object = (List)userData.get("title_page");
            cover_page_object = (List)userData.get("cover_page");
            preface_text_object = (List)userData.get("preface_text");
            /*
             * JSON objects all appear to be just fine, we will unpeel
             * them when they are needed for output
             */
    } // end instantiation
    
    public void createTitlePage(Object out, Object notused) throws Exception
    {
        PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK

	// this PDF style, NO COVER PAGE (this may not be the best approach)

	/*
	 * If no title,cover, or preface pages are wanted we DO NOT
	 * enclose them in a page-sequence!
	 * 
	 * ALL of these are OPTIONAL 
	 */
	if ( (g_options.wantPrefacePage()) ||
	 (g_options.wantCoverPage()) ||
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
         * step 1: cover page (split it out)
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
         */

        Object someobject = null;
        Iterator ii = null;

	if (g_options.wantCoverPage())
	{
		BookUtils.createAPage(pr,cover_page_object); // cover only
	}

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
    
    
    public void createMetadata(Object out) throws Exception
    {
        PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK
        pr.println(gT("METADATA_FOP1")); // start of metadata
        Object someobject = null;
        Iterator ii = page_head_object.iterator();
        while (ii.hasNext())
        {	
            someobject =  ii.next();
            if (someobject instanceof String)
            {
                pr.print(someobject);
                continue; // done for now
            }
            //  now if there is a fall-through, the objects are somebody we don't know about
            throw new Exception("Problems with JSON inside page_head: " + someobject.getClass().getName());
            
        } // end write the content to the stream
        pr.println(gT("METADATA_FOP_END")); // end of metadata
        
    } // end create metadata
    
    public void createStaticHeaders(Object out, Object notused,
		AuxiliaryInformation aux) throws Exception
    {
		createStaticHeaders(out,notused); // no pass through of aux
	}
    public void createStaticHeaders(Object out, Object notused) throws Exception
    {
        PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK
        
	/*
	 * static stuff is broken into 2 parts, those BEFORE
	 * the page top links, and those after 
	 * 
	 * we create the links HERE, because the user may not want them 
	 */
	BookUtils.createAPage(pr,static_header_object1); // front stuff
	// make links, if desired
	if (g_options.wantTOC())
	{
		pr.println(gT("TOC_LINK_FOP"));
	}
	if (g_options.wantAnyIndex())
	{
		pr.println(gT("INDEX_LINK_FOP"));
	}
/*
        "<fo:basic-link internal-destination=\"_toc\" border-bottom-color=\"#c6deff\" border-bottom-style=\"solid\" border-bottom-width=\"medium\">   [ TOC </fo:basic-link>\n",
        "<fo:basic-link internal-destination=\"_indexes\" border-bottom-color=\"#c6deff\" border-bottom-style=\"solid\" border-bottom-width=\"medium\">   Index ]</fo:basic-link>\n",
 */
	BookUtils.createAPage(pr,static_header_object2); // end stuff
        
    } // end create static headers
    
    public void renderIndex(PrintWriter pr, List all, int level) throws Exception
    {
        //    System.out.println(all);
        pr.print(gT("FLOW_END_FOP")); // end current document flow
        endPageSeq(pr);
            
        startFlow(pr,"Index"); // start a new flow with different page number indicator
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
                /*
                 * BREADCRUMBS needed for the interactive PDF
                 * ONLY if there is more than one index type
                 */
		if (g_options.wantGeneralIndexONLY())
		{
			// do NOT make a heading/breadcrumb page!
                         // BUT NOTE that we will need an ID="_indexes" later
                         // it's gonna be ugly
		}
		else
		{
			// more than one index type wanted, make breadcrumb page
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
		 * first block has the special ID of "_indexes"
		 * this is the MASTER location of the top of all
		 * indexes, and is pointed to by such items as
		 * the small link at the top of each page, and
		 * by the index link in the TOC
		 */
                pr.print(gT("STATE_TITLE_BLOCK_FOP1") +
                         "_indexes" + 
                         gT("STATE_TITLE_BLOCK_FOP2") +
                         "Document Indexes" +
                         gT("STATE_TITLE_BLOCK_FOP_END")); 
                while (ingrp.hasNext())
                {
                    inner_group = (IndexGroup)ingrp.next(); // MUST WORK or throw exception for bad structure
                    pr.print(gT("NORMAL_BLOCK_FOP_REST")); // start text block for all breadcrumbs
                
                    pr.print(
                        gT("LINK_FOP1") +
                        inner_group.id +
                        gT("LINK_FOP2") +
                        inner_group.short_title +
                        gT("LINK_END")); //  + " -- "); //  + NORMAL_BLOCK_FOP_END);
                    pr.print(gT("NORMAL_BLOCK_FOP_END")); // terminate breadcrumbs block
                } // end if putting in bread crumb for a particular top-level group
		} // end if breadcrumb page wanted
            
                /*
                 * NOW, we create the index, based on this structure
                 */
                renderIndexGroupList(pr,all,1); // probably will recurse
            } // end if the right kind of group
            else
            {
                throw new Exception("Index Structure Wrong: " + working);
            }
        } // end not index entry
    } // end render the indexes into FOP
    
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
                          gT("LINK_FOP1") +
                         the_entry.id +    // id jump
                     gT("LINK_FOP2") +  // makes a blue-underlined link
                     the_entry.long_title +
                        gT("LINK_END") +
                   "   (" +
                   gT("PAGE_NUMBER_LINK") +
                  the_entry.id +
                   gT("PAGE_NUMBER_LINK_END") +                         
")" +
                     gT("NORMAL_BLOCK_FOP_END"));
        }
    } // end render only a list of index items

    /*
     * for interactive PDF we add breadcrumbs
     */
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
                pr.print(gT("STATE_TITLE_NONBREAKING_BLOCK_FOP1") +
                         inner_group.id +
                         gT("STATE_TITLE_BLOCK_FOP2") +
                         inner_group.long_title +   // sub heading is the longer name with explanatory front, such as "facilities"
// short title does NOT include explanatory front, such as "cities"    inner_group.short_title +   // sub heading is the shorter name
                         gT("STATE_TITLE_BLOCK_FOP_END")); // separate header
            } // end level is 2 or deeper
            else
            {
                /* first level index, cause a page break, make heading */
                /* 
                 * HOWEVER, there are problems with general index-only 
                 * situations. Since there is no breadcrumb page,
                 * we are missing the necessary id=_indexes. Nasty.
                 * 
                 * Soooo, we will make two blocks, one page-break, 
                 * empty with id=_indexes. Second block will be
                 * general title, no  page-break, with id=_general_index.
                 */ 
		if ( (g_options.wantGeneralIndexONLY()) &&
			(inner_group.id.equals("_general_index")) )
		{
			// #1: empty page break block id = "_indexes"
			pr.print(gT("STATE_TITLE_BLOCK_FOP1") +
				 "_indexes" +
				 gT("STATE_TITLE_BLOCK_FOP2") +
				 gT("STATE_TITLE_BLOCK_FOP_END")); // empty header
			// #2: desired heading with correct id, NO break
			pr.print(gT("STATE_TITLE_NONBREAKING_BLOCK_FOP1") +
				 inner_group.id + // _general_index
				 gT("STATE_TITLE_BLOCK_FOP2") +
				 inner_group.long_title +  // General Index
				 gT("STATE_TITLE_BLOCK_FOP_END")); // separate header
		} // end SPECIAL CASE headings for general index
		else
		{
			pr.print(gT("STATE_TITLE_BLOCK_FOP1") +
				 inner_group.id +
				 gT("STATE_TITLE_BLOCK_FOP2") +
				 inner_group.long_title + 
				 //                     inner_group.long_title + " [" + level + "] " +  // debugging for level number
				 gT("STATE_TITLE_BLOCK_FOP_END")); // separate header
		    } // end not special case
	    } // end first-level index
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
    
    public void endPageSeq(
        PrintWriter pr)
    {
        pr.print("</fo:page-sequence>\n");
    }

    public void startFlow(
        PrintWriter pr,
        String page_number_string) throws Exception
    {
        pr.print("<fo:page-sequence  initial-page-number=\"1\" id=\"N2528\" master-reference=\"right\">\n"); // use right only
        // pr.print("<fo:page-sequence  initial-page-number=\"1\" id=\"N2528\" master-reference=\"psmOddEven\">\n"); use odd-even layout system
        pr.print("<!-- page number at bottom --><fo:static-content flow-name=\"xsl-region-after\">\n");
        // inherit        pr.print("<fo:block text-align-last=\"center\" font-family=\"Helvetica\" color=\"gray\" font-size=\"10pt\">\n");
        pr.print("<fo:block text-align-last=\"center\" color=\"gray\" space-before.optimum=\"0\">\n");
        pr.print(page_number_string + " - <fo:page-number/>\n");
        pr.print("</fo:block>\n");
        pr.print("</fo:static-content>\n");
        createStaticHeaders(pr);
        pr.print("<fo:flow flow-name=\"xsl-region-body\">\n");
    } // end startFlow
    
    
    public void renderBookmarkIndexList(PrintWriter pr, List all) throws Exception
    {
        IndexEntry the_entry = null;
        /*
         * ALL items in list are Index contents (no higher up container objects)
         */
        // debugging System.out.println("Handling index list, size: " + all.size() + ", first item: " +
                    // debugging        all.get(0));
        // debugging System.out.flush();
        Iterator inds = all.iterator();
        while (inds.hasNext())
        {
            the_entry = (IndexEntry)inds.next(); // HAS TO BE THIS TYPE, otherwise bad design
            pr.print(gT("BOOKMARKS_LINK_FOP") +
                     the_entry.id +
                     gT("BOOKMARKS_END_FOP"));
            pr.print(gT("BOOKMARKS_TITLE_FOP") +
                     the_entry.long_title +
                     gT("BOOKMARKS_TITLE_END_FOP"));
            pr.print(gT("BOOKMARKS_BOOKMARK_END_FOP")); // end a single bookmark
        }
    } // end render only a list of index items

    public void renderBookmarkGroupList(PrintWriter pr, List all) throws Exception
    {
        IndexGroup inner_group = null;
        Object working = null;
        Iterator ingrp = all.iterator();
        while (ingrp.hasNext())
        {
            inner_group = (IndexGroup)ingrp.next(); // MUST WORK or throw exception for bad structure
            // debugging System.out.println("Handling Group: " + inner_group.long_title);
 // debugging            System.out.flush();
            pr.println(gT("BOOKMARKS_DUMMY_FOP"));
            pr.println(gT("BOOKMARKS_TITLE_FOP1") + 
                       inner_group.short_title + gT("BOOKMARKS_TITLE_FOP2")); // unterminated at this point
//            inner_group.long_title + gT("BOOKMARKS_TITLE_FOP2")); // unterminated at this point
            /*
             * now, check to see what kind of objects are the children
             */
            working = inner_group.children.get(0);
            if (working instanceof IndexEntry)
            {
                renderBookmarkIndexList(pr,inner_group.children);
                // done, now go to next item in group list
            }
            else
            {
                if (working instanceof IndexGroup)
                {
                    // have to recurse
                    renderBookmarkGroupList(pr,inner_group.children); // will recurse
                }
                else
                {
                    throw new Exception("Wrong types mixed in index groups: " + working);
                }
            }
            // call out bottom after having handled either another imbedded group or a list of index items
            pr.print(gT("BOOKMARKS_BOOKMARK_END_FOP"));  // terminate the higher-level group bookmark
        } // end for each group in the list
   //     pr.print(gT("BOOKMARKS_BOOKMARK_END_FOP")); // terminate higher level bookmark
    } // end traverse and render a group of bookmarks (recursive)
    
    public void renderBookmarks(PrintWriter pr, List all) throws Exception
    {
    //    System.out.println(all);
        pr.println(gT("BOOKMARKS_FOP1")); // start bookmark tree
        /*
         * check type of object at the top of this List, given the
         * design it MUST be a Group, as are all in this highest
         * level list. Deeper down, some groups have only
         * entries, others have more groups under them
         */
        Object working = all.get(0); // type is very important
        // debugging System.out.println("Entering bookmark renderer, object: " + working);
 // debugging        System.out.flush();
        if (working instanceof IndexEntry)
        {
            throw new Exception("Index Structure Wrong: " + working); // NOT HERE
        }
        else
        {
            if (working instanceof IndexGroup)
            {
                renderBookmarkGroupList(pr,all); // may recurse
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
        pr.print(gT("BOOKMARKS_TREE_END_FOP"));  
    } // end render the bookmarks into FOP
    
    public void createTOC(PrintWriter pr,
                          Map all_maps,
                          List appendixes,
                          Map index_flags) throws Exception
    {
        /*
         * unwrap the maps and pass to some code that is very
         * specific to this book and format. Exceptions thrown
         * from here will be prefixed with "TOC key missing:".
         */
        makeTableOfContents(pr,
                            get_a_map("STATE",all_maps,"TOC"),
                            get_a_map("ABBREVIATIONS",all_maps,"TOC"),
                            get_a_map("CITY",all_maps,"TOC"),
                            get_a_map("FACILITY",all_maps,"TOC"),
                            get_a_map("GENERAL",all_maps,"TOC"),
                            appendixes,
                            index_flags);
    }
    

    /*
     * code taken from the fop pdf sink
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
	 * TOC is OPTIONAL
	 */
	if (g_options.wantTOC())
	{
		startFlow(pr,"TOC"); // start the TOC flow
		pr.print(gT("STATE_TITLE_BLOCK_FOP1") +
	"_toc" +
			 gT("STATE_TITLE_BLOCK_FOP2") +
			 gT("MARKER_START_STATE_FOP") +
			 "TOC" +
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
         * KLUGE: the TOC is really only one page long. However, the
         * layout for PDF is left-right. We want to maintain that
         * layout, so we need to add an empty page. We'll make
         * another page with (this page left intentionally blank) on it.
         */
// NOT AT THIS TIME        pr.print(gT("BLANK_PAGE_FOP"));
        
	/*
	 * we created some block content above, because TOC
	 * was wanted. we need to terminate the flow, and allow for
	 * next part of the document.
	 */
		pr.print(gT("FLOW_END_FOP") + " <!-- end of flow for toc -->\n");
		endPageSeq(pr);
	} // end if want a TOC
    } // end make table of contents

// HERE HERE, we need to make introduction NOT a fixed, separate section
//	public boolean wantIntroductionByDefault()

	public String makeTOCLine(String id, String narrative,
		String page_number_pre) throws Exception
	{
		return
			gT("NORMAL_BLOCK_FOP_REST") +
			gT("LINK_FOP1") +
			id +
			gT("LINK_FOP2") +
			narrative  +
			gT("LINK_END") +
			"   (" +
			page_number_pre + // can be empty string
			gT("PAGE_NUMBER_LINK") +
			id +
			gT("PAGE_NUMBER_LINK_END") +
			")" +
			gT("NORMAL_BLOCK_FOP_END");
	} // end make simple TOC line


	// new method uses helper in base class
	public void modifyMetaData() throws Exception
	{
		// create array of List s to be modified
		List [] to_process_list = new List[]
		{
		 static_header_object1,
		 static_header_object2,
		 preface_text_object,
		 cover_page_object,
		 page_head_object
		};
/*
		following replace won't work, as the object
		is quite complex. We have code to perform this
		in the createTitlePage method
 		
		 stringReplacer(title_page_object,project_keys); // replace in title page
*/
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
				//result.append("<h1>"); // front is a heading WHAT TO DO?
			    	String xx[] = rval.rep.split(":"); // 1 (one) colon delimiter
        			for (int inner  = 0 ; inner < xx.length ; inner++)
        			{
        				result.append(xx[inner] +
        					"\n\n" ); // skip two lines
        				if (inner < 0)   // NOT USED first line only
        				{
        					result.append("</h1>" +
						"<p style=\"text-align: center;\">" +
						"<br/>");

        				} // end first line only
				}  // end loop through all strings to be treated as separate lines         
			//	result.append("</p>"); HOW TO END?
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
		 * NOW, read json file for PDF-specific! OPTIONS we will use
		 */
		options = BookUtils.getPropertiesFromJSON("options.json",
		"format_options"); 

		// now that we processed the options, return the metadata info
		return result;
	} // end getmetadata (overrides parent class)

	/*
	 * get Options information from the project json file, as well
	 */
    public Object getMetaDataxxxxx(String json_file) throws Exception
    {
    		Object result = getProjectSpecificMetaData(json_file); // use helper in base class
		/*
		 * NOW, read json file for OPTIONS we will use
		 */
        String options_file = "options.json";
        System.out.println("Getting JSON from: " + options_file);
        File input = new File(options_file);
        ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
        Map<String,Object> userData = mapper.readValue(input,Map.class);
        
        // userData is a Map containing the named arrays
        
            /*
             * a List of Strings that specify
             * options for various output. They
             * are pairs, keyword, then value
             */
            List optionsx = (List)userData.get("format_options"); 
	    options = new Properties();
		/*
		 * options are keyword then value
		 */
		List arr = optionsx; // cast
		int count= arr.size();
		for (int inner = 0 ; inner < count ; inner+=2)
		{
			String key = (String)arr.get(inner);
			String value = (String)arr.get(inner+1);
			    options.setProperty(key,value); // strings
		} // end loop on string pairs

    		return result;
    } // end getmetadata (overrides parent class)

    
    public void renderNOFChecklist(PrintWriter pr, Map nof_by_city) throws Exception
    {
    }

    public ReplacementString getProjectKeyValue(String xx) throws Exception
    {
		throw new Exception("FOP (PDF) sink does not use project values");
	}
   
	/*
	 * only property we process right now is column count
	 * 
	 * we tried to change orientation (portrait vs landscape). THIS IS
	 * DONE in the configuration file for the FOP command,
	 * completely outside this Java program. 
	 */
	public String getProperty(String key)
	{
	    if (key.equals("PDF_COLUMN_COUNT"))
		{
			return options.getProperty(key,"1"); // default 1
		}
		else
		{
			return null;
		}
	} // end getproperty
    
} // end special content creator facilities  FOP format for PDF output only (not print)

