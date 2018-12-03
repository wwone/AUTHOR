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
 *
 * updated 12/1/2018
 *
 * remove dependency on old XMLUtils
 *
 * Can REVEAL work with remote images? If so, we have
 * stubbed in the "remote" flag.
 *
 * altered to accept auxiliary metadata, but since this is
 * a single big HTML file, all metdata is at the top. We
 * will ignore the individual metadata supplied.
 *
 * MOVED 2/7 to the mainline code, needs a little cleanup
 *
 * This Sink produces HTML in a format that works
 * with "reveal.js", a system that allows creation
 * of Presentations in a web format. They work and look
 * just like PowerPoint, but are only HTML, with a lovely
 * structure and navigation capability. For the Military
 * Postal History Society, I have made some of these, called
 * "small bites", and the results are remarkable. The work
 * of the REVEAL team is mind-boggling.
 *
 * It is EXPECTED that this system will NOT be used
 * for the Facilities book, but rather for short PowerPoint-like
 * presentations.
 *
 */
public class REVEALSink extends GenericSink
{
    /*
     * globals 
     */

    public String[] BOILERPLATE = BookUtils.HTML_BOILERPLATE;  // use HTML-specific boilerplate
    
    public String g_file_extension; // may be .html or .xhtml, and so on
    public String g_file_extensionq; // same as extension, but has added double quote for use in URL's
    
    public String g_current_state; // used for postfix processing of a state
	public TreeMap nof_by_city = null; // may or may not be built
    
        public List g_toc_list = new ArrayList(); // initialize before anything else happens!
        public List g_reveal_toc_list = new ArrayList(); 

        public List g_non_state_list = null;
        
    public String g_tag_terminator;  // needed to finish "some" HTML tags
 
	public int g_html_format = HTMLContentCreator.FORMAT_POEM;
    
    public REVEALSink() // NO PARAM
	{
	}
    public void init(GenericSource sou,
                    TreeMap state_index_map,
                    TreeMap abbrev_map,
                    TreeMap city_index_map,
                    TreeMap fac_index_map,
                    TreeMap general_index_map,
                    String file_ext)
    { 
        init(sou); // parent
        
        g_file_extension = file_ext; // may be .html, or .xhtml, etc
        g_file_extensionq = g_file_extension + "\"";  // quote at end for use in <a> links
        

        g_state_pr = null;  // no active state file

        
        try
        {
            /*
             * create the writer for the final HTML output
             * during testing, this is stdout
             */
            g_pr = new PrintWriter(new File("reveal" + g_file_extension)); // write to this file only
            //        g_pr = new PrintWriter(System.out);
            /*
             * NO LONGER set up to read the XML, using DOM4J
             */
            g_adap = null;

        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    } // end init
    
    /*
     * simple constructor, just passes through
     */
    public REVEALSink(GenericSource sou)
    {   
//        super(sou);
    }
    
    public void startDocument(SpecialContentCreator tit) throws Exception
    {
	g_tit = tit; // make global, used a lot
        /*
         * create the title page
        g_tit.createTitlePage(null); // writes to it's own file
         */
        /*
         * write start of HTML
         */
        createPrefixHTML(g_pr);
    }
    
	/*
	 * implement for HTML, because we have a TOC at
	 * the top of every HTML file
	 *
	 * IGNORE images passed to the manifest, as
	 * they are not used by the HTML sink
	 */
	public void addToManifest(String name, String title,
	int manifest_flag)
	{
		/*
         * add boilerplate items to non-state-list
         * that will appear at the top of each page
         */
         if (g_non_state_list == null)
         {
	         	// first time
     	     g_non_state_list = new ArrayList(); // initialize before anything else happens!

          	g_non_state_list.add(new TOCEntry(
		    		"index.html#index",
		    		"All Document Indexes"));        
		/*
		 * when we are creating a non-facility
		 * book/document, we don't need a number
		 * of features provided to that style
		 * of product.
		 *
		 * So, do not put in unnecessary or confusing
		 * links in the TOC at the top of each
		 * page.
		 *
		 * For example:
		 *
		 * ONLY put in a local index TOC item at top
		 * if states are being processed
	 	 */
		if (g_options.wantStateIndex())
		{
			g_non_state_list.add(new TOCEntry(
					"#index",
					"This Section Indexes"));    
		}
		/*
		 * ONLY put in a TOC item at top of
		 * page, if facility index is desired
		 */
		if (g_options.wantFacilityIndex())
		{
			g_non_state_list.add(new TOCEntry(
					"facility_index.html",
					"Facility Index"));    
			g_non_state_list.add(new TOCEntry(
					"javascript:pop_up_controlling_window('facility_index_popup.html');",
					"Facility Index Popup"));    
			}
		} // end if facility index wanted
		if (manifest_flag == MANIFEST_IMAGE)
		{
			return; // we are not putting images in any TOC or manifest file
		}
		if (manifest_flag != MANIFEST_STATE)
		{
			// NO states here
           	g_non_state_list.add(new TOCEntry(
		    name + ".html",
		    title));        
		}
		// not image, add to the primary list 
           g_toc_list.add(new TOCEntry(
		    name + ".html",
		    title));        
	} // end addtomanifest

    public void endDocument(
        TreeMap  state_index_map,
        TreeMap  abbrev_map,
        TreeMap  city_index_map,
        TreeMap  fac_index_map,
        TreeMap  general_index_map
        ) throws Exception
    {
	/*
	 * we don't append much with reveal.js
        createPostfixHTML(g_pr,true,
                          state_index_map,
                          abbrev_map,
                          city_index_map,
                          fac_index_map,
                          general_index_map
                          ); // gets all of the indexes at this time
*/

	/*
	 * For first cut, make instruction page
	 * then, make TOC page (dummy)
	 */
	g_pr.println(g_tit.gT("INSTRUCTION_PAGE"));
            
	makeREVEALTOC(); // make TOC

        g_pr.print(g_tit.gT("SKELETON_PAGE_CLOSE"));

        g_pr.flush();  // flush out the HTML output stream
        

    } // end of enddocument method
    
	/*
	 * make a TOC section, using accumulated toc entries
	 */
	public void makeREVEALTOC() throws Exception
	{
		TOCEntry entry = null;
		// write front matter
		g_pr.println(g_tit.gT("TOC_FRONT"));
		/*
		 * for readability! we must compress like-named entries
		 */
		String previous = "";
		ArrayList result = new ArrayList();
           	Iterator ii = g_reveal_toc_list.iterator();
		while (ii.hasNext())
		{
			entry = (TOCEntry)ii.next();
			if (entry.toc_title.equals(previous))
			{
				// dont use
			}
			else
			{
				result.add(entry); //use this one
				previous = entry.toc_title;
			}
		} // end looking for dupes
		ii = result.iterator(); // wanted items
		while (ii.hasNext())
		{
			entry = (TOCEntry)ii.next();
			g_pr.println("<a href=\"" + entry.link +
				"\">" + entry.toc_title +
				"</a><br/>");
		}
		g_pr.println(g_tit.gT("TOC_END")); // complete toc
	} // end make Reveal.js TOC slide

    public PrintWriter getCurrentWriter()
    {
/*
        if (g_state_pr != null)
        {
            return g_state_pr;   // this seems to be the active one
        }
*/
        return g_pr; // the main web page
    }
    
    /*
     * allows an external object to get the sink resource in
     * use, such as the active printwriter (for HTML, etc)
     */
    public Object getSinkResource()
    {
        return getCurrentWriter(); // in our case we return the current printwriter
    }
    
    public void startStates()
    {
	//throw new Exception("REVEAL does not handle state sections");
        // states are currently in their own HTML files, one per state
    } // start states grouping
    
    public void endStates()
    {
	//throw new Exception("REVEAL does not handle state sections");
        // states are currently in their own HTML files, one per state
    } // end states grouping
    
	/*
	 * main section does not really exist with reveal.js
	 */
    public void startMainSection(
        String short_title) throws Exception
    {
        /*
         * introduction content
         *
         * reuse the "state" writer reference
        g_state_pr = new PrintWriter(new File("introduction" + g_file_extension));
        createPrefixHTML(g_state_pr);
         */
                
        //        g_state_pr.print("<div> <!-- main section -->\n");
    }  // end start up main section
    
	/*
	 * for reveal.js all is one file, so this is the end of the document???
	 */
    public void endMainSection() throws Exception
    {
        /*
         * finish main section
         */
        //g_state_pr.print(g_tit.gT("TOC_LINK1"));
        /*
         * the main section is simply text, usually the INTRODUCTION
         *
         * we need to terminate this page correctly
        g_state_pr.print(g_tit.gT("END_SECTION"));
        g_state_pr.print(g_tit.gT("END_BODY"));
        g_state_pr.flush();
        g_state_pr.close();
        g_state_pr = null;  // so it can be seen later
         */
        //
        // state writer can now be reused
    } // end terminate main section
    
    public void startAbbreviationSection(String app_name,
                                         String app_title,
                                         String short_title) throws Exception
    {
	throw new Exception("REVEAL does not handle abbreviation sections");
    }
    
    public void endAbbreviationSection(TreeMap abbrev_map,
                                       TreeMap state_index_map,
                                       TreeMap city_index_map,
                                       TreeMap fac_index_map,
                                       TreeMap general_index_map
                                       ) throws Exception
    {
	throw new Exception("REVEAL does not handle abbreviation sections");
    } // end abbreviation section
    

    public void createAbbreviationIndex(PrintWriter pr,
                                        TreeMap abbrev_map)
    {
	//throw new Exception("REVEAL does not handle abbreviation sections");
   } 
    public void startAppendixSection(String appendix_name,
                                     String appendix_title,
                                     String short_title) throws Exception
    {
	throw new Exception("REVEAL does not handle appendixes");
    }
    
    public void endAppendixSection(TreeMap state_index_map,
                                   TreeMap abbrev_map,
                                   TreeMap city_index_map,
                                   TreeMap fac_index_map,
                                   TreeMap general_index_map
                                   ) throws Exception
    {
	throw new Exception("REVEAL does not handle appendixes");
    } // end appendix section
    
	/*
	 * we use the fields this way:
	 * 1) general_name is the internal ID of the section start
	 * 2) short_title (2nd entry in AUTHOR) is link, if any, to
	 *    background image (else page defaults to style)
	 */
    public void startGeneralSection(String general_name,
                                    String general_title,
                                    String short_title,
				AuxiliaryInformation aux) throws Exception
    {
/* debug
	System.err.println("startGeneral: " + general_name +
	", title: " + general_title + ", short: " +
	short_title);
 */
	// store for later, when we make TOC
           g_reveal_toc_list.add(new TOCEntry(
		"#/" + general_name,  // URL (inside this html file)
		general_title)); // link text
        /*
         * general section is separate <section> within
         * reveal.js. SAME FILE
         */
	if (BookUtils.returnContentsOrNull(short_title) == null)
	{
		g_pr.println("<section id=\"" + general_name + "\">");
	}
	else
	{
		// background image
		g_pr.println("<section id=\"" + general_name + 
			"\" data-background-image=\"" +
			short_title + "\">");
	}
	g_pr.println(g_tit.gT("SECTION_FRONT"));
    } // end start general section (all sections for reveal.js)

public void setOptions(Options op)
{
	g_options = op;
}
    

public void setSpecialTerminator(String x)
{
}

    
    public void endGeneralSection(TreeMap state_index_map,
                                  TreeMap abbrev_map,
                                  TreeMap city_index_map,
                                  TreeMap fac_index_map,
                                  TreeMap general_index_map
                                  ) throws Exception
    {
        /*
         * done with section
         */
	g_pr.println("</section>");
    } // end general section
    
    public void startAState(String the_abbrev, 
                            String the_state,
                            String anchor_name,
                            List index_array,
                            TreeMap state_index_map,
                            TreeMap current_city_index_map,
                            TreeMap current_fac_index_map,
                            String short_title) throws Exception
    {
throw new Exception("REVEAL system does not handle states");
    } // end startastate

    /*
     * NOTE the following static methods really ought to be in the
     * SPecial Content creator, since they are very HTML-specific.
     * WE ARE WORKING THAT NOW
     */
    
    /*
     * print out a full page width "skeleton" grid starting div
     * HAVE THIS DONE IN STATIC that is accessible to all
    public  static  void fullWidthSkeleton(PrintWriter pr)
    {
        *
         * ISSUE, the "prefix" code already sets up a full width column!! WHAT TO DO??
         *
    }
    */

	/*
	 * bridge to the static version ONLY if we are 
	 * using SKEL(eton)
	 */
    public    void fullWidth(PrintWriter pr)
    {
		if (g_html_format==HTMLContentCreator.FORMAT_SKELETON)
		{
			HTMLContentCreator.fullWidthSkeleton(pr); // use static version
		}
		// ELSE, we do nothing, POEM does not require special handling
    	}

    /*
     * bridge to static method
     */
    public   void columnOneSkeletonx(PrintWriter pr)
    {
    		HTMLContentCreator.columnOneSkeleton(pr);
    }

    /*
     * bridge to static method
     */
    public  static  void columnTwoSkeletonx(PrintWriter pr)
    {
    		HTMLContentCreator.columnTwoSkeleton(pr);
    }

    /*
     * bridge to static method
     */
    public   void finishSkeletonx(PrintWriter pr)
    {
	// ONLY SKEL(eton) needs termination tags
		if (g_html_format == HTMLContentCreator.FORMAT_SKELETON)
		{
			HTMLContentCreator.finishSkeleton(pr);
		}
		// fall through for POEM
    }
    
    public void createStatePrefixHTML(PrintWriter pr,
                                      String state_name
                                      ) throws Exception
    {
	throw new Exception("REVEAL does not handle state sections");
       }
    


    /*
	* bridge to static method
	*/
    public void make2Columnsx(PrintWriter pr, List items)
    {
    		HTMLContentCreator.make2Columns(pr,items,
			g_html_format); // tell it what kind of HTML
	}
    
    /*
     * invoked for the last state seen.
     * a new state handler closes the previous, but
     * this happens at the end without a new state
     */
    public void endAState(TreeMap state_map,
                          TreeMap current_city_index_map,
                          TreeMap current_fac_index_map) throws Exception
    {
	throw new Exception("REVEAL does not handle state sections");
    }

    
    /*
     * We will try to make the indexes for each state simpler than the
     * full-blown indexes on the master index pages
     */
    public void createStatePostfixHTML(PrintWriter pr,
                                       String current_state,
                                       TreeMap state_index_map,
                                       TreeMap current_city_index_map,
                                       TreeMap current_fac_index_map) throws Exception
    {
	throw new Exception("REVEAL does not handle state sections");
    } // end postfix writing for each state entry

/*
 * SPECIAL CHARACTERS?
 */
    public void create_anchor(String anchor_name,PrintWriter pr) throws Exception
    {
        if (anchor_name != null)
        {
            pr.print(g_tit.gT("SIMPLE_ANCHOR_START") +
                         BookUtils.eC(anchor_name) + 
                         g_tit.gT("ANCHOR_END"));
        } // if anchor marker wanted
    }
    
/*
 * SPECIAL CHARACTERS?
 */
    public void create_index_entries(List index_array, PrintWriter pr) throws Exception
    {
        if (index_array != null)
        {
            Iterator it = index_array.iterator();
            IndexRef ir = null;
            while (it.hasNext())
            {
                ir = (IndexRef)it.next();
                pr.println(g_tit.gT("GENERAL_ANCHOR_START2") +
                           BookUtils.eC(ir.name) + "_" +
                           ir.getRefNumber() +  
                           g_tit.gT("ANCHOR_END"));
            } // end for each index entry
        } // if embedded index(es) 
    } // end create index entries
    
/*
 * SPECIAL CHARACTERS?
 */
    public void startACity(String the_abbrev, 
                           String the_state,
                           String the_city,
                           String anchor_name,
                           List index_array) throws Exception
    {
throw new Exception("REVEAL system does not handle cities");
    } // end start a city

    public void endACity()
    {
//throw new Exception("REVEAL system does not handle cities");
    }
    
/*
 * SPECIAL CHARACTERS?
 */
    public void startAFacility(String the_abbrev, 
                               String the_state,
                               String the_city,
                               String the_facility,
                               String source_page,
                               String open_date,
                               String personnel_count,
                               String hospital_admissions,
                               String close_date,
                               boolean no_postal_history_flag,
                               String anchor_name,
                               List index_array) throws Exception
    {
throw new Exception("REVEAL system does not handle facilities");
    } // end starta facility

	public void insertRefIfNotNull(String item,
	List contents, String prefix)
	{
       if (item != null)
        {
            contents.add(prefix + item);
        }
        // no action if entry is null
     } // end insert ref if not null
/*
 * SPECIAL CHARACTERS?
 */
    public void endAFacility(String the_state,
                             String the_city) throws Exception
    {
throw new Exception("REVEAL system does not handle facilities");
    } // end end a facility
   
    public void startBoilerplate(int type,
                                 String inner_text,
                                 String the_preceeding_text,
                                 boolean allow_span) throws Exception
    {
        /*
         * boilerplate text, but each type of sink formats this
         * text differently.
         * the text source, however has to reside in the primary driver program
         */
        PrintWriter pr = getCurrentWriter();

        boolean print_terminator = true;  // end paragraph when we are done here
        
        pr.print(g_tit.gT("PARAGRAPH_START2"));        // starts new paragraph
        if (allow_span)
        {
            print_terminator = false; // don't end paragraph at this point
        }
        g_tag_terminator = g_tit.gT("PARAGRAPH_END");   // if spanning, this terminator will be needed by the final code
        if (the_preceeding_text == null)
        {
            pr.print(BOILERPLATE[type]); //  + " \n"); // starts sentence
        }
        else
        {
            pr.print(BookUtils.eT(the_preceeding_text)); // whatever was entered
            pr.print(BOILERPLATE[type]); // continues sentence
        }
        pr.print(BookUtils.eT(inner_text));
        if (print_terminator)
        {
            pr.println(g_tit.gT("PARAGRAPH_END"));
        }
        else
        {
            pr.println(); // just terminate line for readability
        }
        // if spanning, let the following constructions print the paragraph terminator
    } // end start boilerplate text
/*
 * SPECIAL CHARACTERS?
 */
    
    public void startText(String text,
                          String anchor_content) throws Exception // start of sequence with inserted link, emphasis, etc
    {
        PrintWriter pr = getCurrentWriter();

        if (anchor_content != null)
        {
            pr.print(g_tit.gT("PARAGRAPH_ANCHOR_START2") +
                     anchor_content +
                     g_tit.gT("TAG_ID_END"));
        }
        else
        {
            pr.print(g_tit.gT("PARAGRAPH_START2"));            // starts new paragraph
        }
        pr.print(BookUtils.eT(text));
        g_tag_terminator = g_tit.gT("PARAGRAPH_END");  // would terminate this tag, someone else has to handle
    }
    
    public void startPREText(String text,
                          String anchor_content) throws Exception // start of sequence with inserted link, emphasis, etc
    {
        // main code seems to handle just fine
        startText(text,anchor_content);
    }
    
/*
 * SPECIAL CHARACTERS?
 */
    public void endText(String text)  // last text in a sequence
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(BookUtils.eT(text));
        pr.print(g_tag_terminator);  // whatever was needed to terminate the sequence of text
    }
    
    public void setIndexLocation(List index_items) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        create_index_entries(index_items,pr);
    }
        
/*
 * SPECIAL CHARACTERS?
 */
    public void setAnchor(String name) throws Exception // destination of a "go to"
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("SIMPLE_ANCHOR_START") +
                 BookUtils.eC(name) + 
                 g_tit.gT("ANCHOR_END"));
    }
    
/*
 * SPECIAL CHARACTERS?
 */
    public void insertSeparator(String anchor_content) throws Exception
    {
        // this is a separator
        PrintWriter pr = getCurrentWriter();
        if (anchor_content != null)
        {
            pr.print(g_tit.gT("PARAGRAPH_SEPARATOR_START1") +
                     anchor_content +
                     g_tit.gT("PARAGRAPH_SEPARATOR_START2"));
        }
        else
        {
            pr.print(g_tit.gT("SEPARATOR_PARAGRAPH"));
        }
        
        g_tag_terminator = "\n";
    }
    
    public void endList() throws Exception // ends bulleted list
    {
        // this is the end of a list
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("LIST_END"));
        g_tag_terminator = "\n";
    }
    
    public void startList(String size) throws Exception  // start bulleted list
    {
        PrintWriter pr = getCurrentWriter();
	g_list_font_size = BookUtils.returnContentsOrNull(size); // used later
        // this is the start of a list
	if (g_list_font_size != null)
	{
	System.err.println("REVEAL List Start, Size: " + g_list_font_size); // debug
		pr.print(g_tit.gT("LIST_START_FONT1") +
			g_list_font_size +
			g_tit.gT("LIST_START_FONT2"));
	}
	else
	{
		// ordinary list, no font size override
		pr.print(g_tit.gT("LIST_START"));
	}
        g_tag_terminator = "\n";
    } // end start list

    // for list items that contain inner sequences (quote, link, etc)
/*
 * SPECIAL CHARACTERS?
 */
    public void insertListItemStart(String text,
                                    String anchor_content) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        // start of item in list (usually "li")
// HERE set font size???
        if (anchor_content != null)
        {
            pr.print(g_tit.gT("LIST_ITEM_START1") +
                     anchor_content + 
                     g_tit.gT("TAG_ID_END"));
        }
        else
        {
            pr.print(g_tit.gT("LIST_ITEM_START2"));
        }
        pr.print(BookUtils.eT(text));
        g_tag_terminator = "\n";  // no terminator tag yet
    }
    
    public void insertListItemEnd(String text) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        // end of list item
        pr.print(BookUtils.eT(text) +
                 g_tit.gT("LIST_ITEM_END")); // terminate the list item
    }
            
/*
 * SPECIAL CHARACTERS?
 */
    public void insertListItem(String text,
                               String anchor_content) throws Exception // add bullet item
    {
        PrintWriter pr = getCurrentWriter();
        // this is a single item in a list
        if (anchor_content != null)
        {
            pr.print(g_tit.gT("LIST_ITEM_START1") +
                     anchor_content + 
                     g_tit.gT("TAG_ID_END"));
        }
        else
        {
            pr.print(g_tit.gT("LIST_ITEM_START2"));
        }
        
        pr.print( BookUtils.eT(text) + 
                  g_tit.gT("LIST_ITEM_END"));
    }

/*
 * SPECIAL CHARACTERS?
 */
    public void createHeading(int type, String text,
                              String anchor_content) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        if (anchor_content != null)
        {
            pr.print(
                     g_tit.gT("HEADER_START") +
                     String.valueOf(type) + 
                     g_tit.gT("ANCHOR_START") +
                     anchor_content + 
                     g_tit.gT("HEADER_ID2"));

        } // end add anchor content
        else
        {
        		// no anchor content, just print header
	        pr.print(    g_tit.gT("HEADER_START") +
                String.valueOf(type) + 
                g_tit.gT("TAG_END"));
        }
        pr.print(BookUtils.eT(text) + 
                 g_tit.gT("HEADER_END") +
                 String.valueOf(type) + 
                 g_tit.gT("TAG_END"));
    } // end create heading
    
/*
 * SPECIAL CHARACTERS?
 */
    public void insertQuotedText(String text) throws Exception  // text in "middle" of a sequence with italics as a quote
    {
        PrintWriter pr = getCurrentWriter();
        /*
         * in spite of documentation to the contrary on the web,
         * it appears that many browsers DO NOT do anything
         * special with the "q" tag for quote...
         */
        pr.print(g_tit.gT("QUOTE_START") +
                 BookUtils.eT(text) + 
                 g_tit.gT("QUOTE_END"));        // terminate quote text, no newline
    }
    
/*
 * SPECIAL CHARACTERS?
 */
    public void insertCitedText(String text) throws Exception  // text in "middle" of a sequence with italics as a cite
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("CITE_START") +
                 BookUtils.eT(text) + 
                 g_tit.gT("CITE_END")); // terminate quote text, no newline
    }
    
/*
 * SPECIAL CHARACTERS?
 */
    public void insertBlockQuote(String text) throws Exception  // text in a separate paragraph, not in the "middle"
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("BLOCKQUOTE_START") +
                 BookUtils.eT(text) + 
                 g_tit.gT("BLOCKQUOTE_END"));        // a separate paragraph
    }
    
/*
 * SPECIAL CHARACTERS?
 */
    public void insertEmphasizedText(String text) throws Exception  // text in "middle" of a sequence which is bolded
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("EMPHASIZED_START") +
                 BookUtils.eT(text) + 
                 g_tit.gT("EMPHASIZED_END"));        // terminate emphasized text, no newline
    }

/*
 * SPECIAL CHARACTERS?
 */
    public void insertIntermediateText(String text) // text in the "middle" of a sequence that is ordinary
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(BookUtils.eT(text) + "\n");  // nothing much, just newline (hope it looks OK)
    }
    
    public void insertIntermediateBreak() // break in the "middle" of a sequence that is ordinary
    {
        PrintWriter pr = getCurrentWriter();
        pr.print("<br/>\n");  // using br tag and see if it works (parameterize?)
    }

/*
 * SPECIAL CHARACTERS?
 */
    public void insertLink(String href,
                           String text) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("INDEX_TARGET1") +
                 BookUtils.eT(href) + 
                 g_tit.gT("INDEX_CRUMB2"));
        //
        // text that follows will be before the </a>
        //
        pr.print(BookUtils.eT(text) + 
                 g_tit.gT("INDEX_CRUMB_END"));
    }
    
    
/*
 * SPECIAL CHARACTERS?
 */
    public void insertSimpleText(String content) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("PARAGRAPH_START2") +
                 BookUtils.eT(content) + 
                 g_tit.gT("PARAGRAPH_END"));
    }
    
/*
 * SPECIAL CHARACTERS?
 */
    public void insertSeeAlso(
        String filename,
        String link,
        String content,
        String middle_text,
        String final_text) throws Exception
    {
        /*
         * this is a HTML maker. We may be passed a see_also link
         * that has no filename. This will default to index.html
         *
         * other formats are all one file, so filename means very little
         */
        if (filename == null)
        {
            filename = "reveal.html"; // KLUGE default
            System.out.println("WARN: see_also inserted index.html filename for: " +
                               link + ", " + content);
        }
        PrintWriter pr = getCurrentWriter();
        //pr.print(g_tit.gT("INDEX_TARGET1") +
        pr.print(g_tit.gT("INDEX_SEEALSO_TARGET1") + // special formatting
                 filename + "#" +
                 BookUtils.eC(link) + 
                 g_tit.gT("INDEX_CRUMB2"));
        pr.print(BookUtils.eT(content) + 
                 g_tit.gT("INDEX_CRUMB_END"));
        /*
         * to save typing, we violate the pattern and
         * allow specification of final text (after the link),
         * middle text (after link, but more text follows),
         * BUT NOT BOTH
         * 
         * can have neither
         */
        
        if (final_text != null)
        {
            /*
             * the last thing we printed was the <a> tag. The
             * inner text will be the anchor text,
             * and the following text will finish with
             * a paragraph ending tag
             */
            pr.println(BookUtils.eT(final_text) + 
                       g_tit.gT("PARAGRAPH_END"));
            //
            // this sequence is now OVER
            //
            return; // Either we have final text, middle text, or none
        } // end if final text
        if (middle_text != null)
        {
            /*
             * the last thing we printed was the <a> tag. The
             * inner text will be the anchor text,
             * and the following text will be printed
             * with nothing following
             */
            pr.println(BookUtils.eT(middle_text));
            //
            // this sequence MUST BE CONTINUED, there is no </p> terminator YET!
            //
        } // end if middle text instead of final text
    } // end insert see also
    
    public  void preProcessInlineImage(
		boolean remote,
		String thumb_image_location,
                                       String full_image_location,
                                       String pdf_scale,
                                       String html_width,
                                       String pdf_use,
                                       String caption)
    {
        // nothing for now
    }
    
    
/*
 * SPECIAL CHARACTERS?
 */
    public  void processInlineImage(
		boolean remote,
		String thumb_image_location,
                                    String full_image_location,
                                    String pdf_scale,
                                    String html_width,
                                    String pdf_use,
                                    String caption,
                                    String anchor_content) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        /*
         * process image first, then text
         */
        if (full_image_location != null)
        {
            // create inline AND popup
            if (caption != null)
            {
                // popup window with caption
                if (anchor_content != null)
                {
                    pr.print(g_tit.gT("INLINE_IMAGE_START1") +
                             anchor_content + 
                             g_tit.gT("INLINE_IMAGE_START2"));
                }
                else
                {
                	throw new Exception("Images MUST have anchor names! Image: "+
				full_image_location);
                }
                pr.print(g_tit.gT("INLINE_IMAGE_START4") +
                         BookUtils.eT(full_image_location) + 
                         g_tit.gT("INLINE_IMAGE_START5") +
                         BookUtils.eT(breakCaption(caption)) +   // build multiple captions and NO single quotes!! 
                         g_tit.gT("INLINE_IMAGE_START6") +
                         BookUtils.eT(filterForHTMLTitle(caption)) +   // set up for use in HTML title for page
                         g_tit.gT("INLINE_IMAGE_START7") +
                         g_tit.gT("INLINE_IMAGE_ALT") +
                         BookUtils.eT(full_image_location) + 
                         g_tit.gT("INLINE_IMAGE_STYLE") +
                         BookUtils.eT(thumb_image_location) + 
                         g_tit.gT("INLINE_IMAGE_END"));
			// start caption
			pr.println(g_tit.gT("IMAGE_CAPTION_START"));
                pr.print(BookUtils.eT(addLineBreaks(caption)) + "\n"); // split if necessary 
                pr.print(g_tit.gT("AFTER_CAPTION")); // special line after caption  
            }
            else
            {
                	throw new Exception("Images MUST have caption! Image: "+
				full_image_location);
            }
        } // end full image specified
        else
        {
		throw new Exception("Image specified without full image location! Image: "+
				thumb_image_location);
        }
        pr.print(g_tit.gT("PARAGRAPH_END"));        // finish up the paragraph for the image
    } // end process an inline image


    public void noteReferenceDuplicate(String key,
                                       String content,
                                       String context) // for debugging, this gets stored somewhere
    {
        // put into main index.html file
        
        //        g_pr.println("<!-- duplicate " + key + " " + content + "\n   from " + context + " -->");
        
    }
    

    /**
     * receive a String and return it with HTML line breaks present for any
     * '::' characters
     */
    public String addLineBreaks(String x) throws Exception
    {
        // replace every double colon with HTML newline
        String newer = x.replaceAll("::",
                                    g_tit.gT("LINE_BREAK2"));
        return newer;
    }
    
    
    public String breakCaption(String x)
    {
        StringBuffer res = new StringBuffer(100);
        /*
         * walk through all the breaks in the caption, and put each
         * into a Javascript array. That array will be put into function calls
         * to pop up the images.
         *
         * comma assumed before, so put one after
         *
         * SORRY: due to many problems passing single quotes
         *   through Javascript, we must suppress them
         */
        res.append("new Array("); // start of array declaration
        String xx[] = x.split(":{2}"); // 2 (only) colon delimiters
        for (int inner  = 0 ; inner < xx.length ; inner++)
        {
            res.append("'" + BookUtils.escapeSingleQuotes(xx[inner]) + "'"); // escape single quotes
//            res.append("'" + xx[inner].replaceAll("'"," ") + "'"); // SORRY no single quotes
            if (inner == xx.length - 1)
            {
                // last one, no comma
                //
            }
            else
            {
                // not last, put comma on end
                res.append(",");
            }
        }
        res.append("),"); // end the array declaration, comma for next param in function call
        return res.toString();
    }
    

    /**
     * receive a String and return it so that break requests (::) are
     * removed
     * also remove any single quotes (sorry about that)
     */
    public String filterForHTMLTitle(String x)
    {
        String newer = x.replaceAll("::"," -- ");   // replace every double colon with some simple text
        String newer2 = newer.replaceAll("'"," ");   // replace every single quote with a space
        return newer2;
    }
    
    public void makeNewLine(PrintWriter pr) throws Exception
    {
        pr.print(g_tit.gT("LINE_BREAK"));
    }
    
    public void createPrefixHTML(PrintWriter pr) throws Exception
    {
        g_tit.createStaticHeaders(pr,g_non_state_list); // pass TOC items from first pass 
            
    }
   
/*
 * for now, we are NOT using this
 */
    public void createPostfixHTMLnotused(PrintWriter pr, 
                                  boolean make_index,
                                  TreeMap state_index_map,
                                  TreeMap abbrev_map,
                                  TreeMap city_index_map,
                                  TreeMap fac_index_map,
                                  TreeMap general_index_map
                                  ) throws Exception
    {
        fullWidth(pr); 
        pr.print(g_tit.gT("POSTFIX_CONTENT")); // more links, pointer to toc
                finishSkeletonx(pr);

        /*
         * we can make several types of outlines here by using the treemaps
         * that were passed to us
         *
         * destination names:
         * 1) general: == general index
         * 2) state: with no city  == state list
         * 3) state:name:city  with no fac == city list
         * 4) facilities (break out by initial letter)
         * 5) facilities with no postal history (break out by initial letter)
         *
         * NOTE that we have to include a "target" in the index, because
         * content is spread around various files, such is also
         * done with HTML
         */
        pr.print(" <!-- start postfix -->\n");  // no div
        
	/*
	 * reveal.js not using indexes (or are we?)
	 */
        
        fullWidth(pr);    
        pr.print(g_tit.gT("HIDDEN_MESSAGE")); // currently EMPTY!!
// don't think we need these any more
        makeNewLine(pr);
        makeNewLine(pr);
        makeNewLine(pr);
        makeNewLine(pr);
        /*
         * BUG, use the special CSS display of the validator, not the image
         *   (else image is local and not on the web)
	 * [probably no longer applies]
         */
// don't think we need this        pr.print(g_tit.gT("PARAGRAPH_END"));
        finishSkeletonx(pr);
        pr.print(g_tit.gT("SKELETON_PAGE_CLOSE"));
    } // end create postfix HTML


} // end  REVEAL sink
