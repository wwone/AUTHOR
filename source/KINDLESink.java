import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;

/*
 *
 * last edited 12/1/2018
 *
 * remove dependency on old XMLUtils
 *
 * altered to receive auxiliary metadata, but
 * since the Kindle data is one big HTML-like file,
 * we cannot insert metadata for each section.
 * for now, we ignore this information.
 *
 * KINDLE DOES NOT USE the HTML formatting
 * we have used for indexes, particularly
 * not a 2-column layout using skeleton!!!
 * FIX THIS
 *
 * handles preformatted text
 * 
 * The code here is the sink for simplified HTML
 * (and we DO MEAN SIMPLIFIED!) that will be converted 
 * into Kindle (MOBI) format
 * by tools like "kindlegen" and the Kindle Previewer
 *
 * There are a number of changes we make from ordinary
 * EPUB or HTML, including adding special
 * markers that are unique to Kindle
 *
 *
 * STRUCTURE:
 *
 * Have learned a bit about how to structure the
 * single HTML file for Kindle. We must write
 * partial files as:
 *
 * Title, Front Matter, Preface -- file 1
 *    NOTE no cover page, this is done as
 *    a separate image. I have a formatted cover
 *    page in the JSON, but Kindle does not want it
 *
 * TOC -- file 2
 *  Created at the end of all processing in this Sink
 *  object. This is done because we don't know
 *  all the sections that will be in the TOC.
 *  (note we write the "manifest" skeleton of
 *  the NCX file at the end of processing, but 
 *  this file is separately maintained from this
 *  programmitic creation)
 *
 * Content Matter -- file 3
 *   The body of the book, right to the end
 *
 * ISSUES:
 *
 * Add ability to choose TOC and various indexes
 *
 */
public class KINDLESink extends HTMLSink
{
    /*
     * globals 
     */

    public String[] BOILERPLATE = BookUtils.HTML_BOILERPLATE;  // use HTML-specific boilerplate
    
    public String g_file_extension; // may be .html or .xhtml, and so on
    public String g_file_extensionq; // same as extension, but has added double quote for use in URL's
    /*
     * The filename we use for linkage references is the final
     * name constructed from the components outside
     * this program
     */
    public final static String KINDLE_FILE = "kindleindex"; // this is the single file for output
  
    public String g_kindle_file = null; // will be KINDLE_FILE plus extension
    
    public List g_toc_list = null;
    public List g_state_toc = null;
    
    public String g_tag_terminator;  // needed to finish "some" HTML tags
 
    public final static String IMAGE_LOCATION = "";   // use current directory
    //public final static String IMAGE_LOCATION = "thumbs/";   // use thumbs directory
    //public final static String IMAGE_LOCATION = "pics/";   // use pics directory

    public KINDLESink() // NO PARAM
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
        
        g_file_extension = file_ext; 
        
        g_toc_list = new ArrayList();
        g_state_toc = new ArrayList();
        
        try
        {
            /*
             * create the writer for the final HTML output
             * THIS IS A ONE FILE creation. That is, unless there
             * are issues with HTML code that must be created
             * after others, all output is to a SINGLE
             * file.
             */
            g_kindle_file = KINDLE_FILE + file_ext;
            
            g_pr = new PrintWriter(new File(KINDLE_FILE + "3" + file_ext)); // main body is written here
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
    public KINDLESink(GenericSource sou)
    {   
//        super(sou);
    }
    
    public void startDocument(SpecialContentCreator tit) throws Exception
    {
	g_tit = tit; // make global, used a lot
        /*
         * now that we have the g_options object, we can
         * do some initial housekeeping. This WAS done in the constructor,
         * BUT the Options were not set at that time.
         */
            /*
             * These TOC items are pre-created, so we know who they are
             */
//            g_toc_list.add(new TOCEntry(g_kindle_file + "#cover_page","Cover Page"));
		if (g_options.wantTitlePage())
		{
		    g_toc_list.add(new TOCEntry(g_kindle_file + "#title_page","Title Page"));
		}
		if (g_options.wantFrontMaterialPage())
		{
		    g_toc_list.add(new TOCEntry(g_kindle_file + "#front_page","Front Matter"));
		}
		if (g_options.wantPrefacePage())
		{
		    g_toc_list.add(new TOCEntry(g_kindle_file + "#preface_page","Preface for Kindle Users"));
		}
	// at one time a separate link to the TOC was considered redundant,
	// since we provide a toc.ncx file to the Kindle creator
		if (g_options.wantTOC())
		{
		       g_toc_list.add(new TOCEntry(g_kindle_file + "#TOC","Table of Contents"));
		}
            g_toc_list.add(new TOCEntry(g_kindle_file + "#_introduction","Introduction"));
        /*
         * create the title page
         */
        //System.err.println("in kindle sink startdocument, toc size: " +
          //                 g_toc_list.size());
        g_tit.createTitlePage(g_pr,g_toc_list); // writes to it's own file

//        createPrefixHTML(g_pr); // page break right now
      //  g_pr.println("<a name=\"_introduction\"></a>"); // marker for introduction
    } // end startDocument
    
    public void endDocument(
        TreeMap  state_index_map,
        TreeMap  abbrev_map,
        TreeMap  city_index_map,
        TreeMap  fac_index_map,
        TreeMap  general_index_map
        ) throws Exception
    {
        createPostfixHTML(g_pr,true,
                          state_index_map,
                          abbrev_map,
                          city_index_map,
                          fac_index_map,
                          general_index_map
                          ); // gets all of the indexes at this time
            
        g_pr.flush();  // flush out the HTML output stream
        /*
         * make the TOC item list for insertion by hand into the NCX file
         */
        //System.err.println("in kindle sink enddocument, toc size: " +
          //                 g_toc_list.size());
        
        Iterator ii = g_toc_list.iterator();
        TOCEntry toc = null;
        PrintWriter pr = new PrintWriter(new File("kmanifest.xml"));
        int position = 1;
        while (ii.hasNext())
        {
            toc = (TOCEntry)ii.next();
            pr.println(g_tit.gT("TOC1") +
                       // was "<navPoint id=\"navPoint-" + 
                       position + 
                       g_tit.gT("TOC2") +
                       // was "\" playOrder=\"" + 
                       position + 
                       g_tit.gT("TOC3"));
            // was "\">");
            pr.println(g_tit.gT("TOC4"));
            // was "<navLabel>");
            pr.println(g_tit.gT("TOC5") +
                       // was "<text>" + 
                       toc.toc_title + 
                       g_tit.gT("TOC5_END"));
            // was"</text>");
            pr.println(g_tit.gT("TOC4_END"));
            // was "</navLabel>");
            pr.println(g_tit.gT("TOC6") +
                       // was "<content src=\"" + 
                       toc.link + 
                       g_tit.gT("TOC7"));
            // was "\" />");
            pr.println(g_tit.gT("TOC1_END"));
            // was "</navPoint>");
            position++;
        }
        pr.flush();    
        pr.close();
        /*
         * NOW, make the TOC file. This HTML will be merged into the
         * "middle" of the single final file, between the front
         * matter and the body.
         */
        pr = new PrintWriter(new File(KINDLE_FILE + "2" + g_file_extension)); // main body is written here
	// TOC here may be empty!
	if (g_options.wantTOC())
	{
		ii = g_toc_list.iterator();
		pr.println(g_tit.gT("KINDLE_PAGE_BREAK"));
		// was "<mbp:pagebreak />");
		pr.println(g_tit.gT("TOC_HEADER"));
		// was "<h1 id=\"TOC\">Table of Contents</h1>");
		while (ii.hasNext())
		{
		    toc = (TOCEntry)ii.next();
		    pr.println(g_tit.gT("PARAGRAPH_START"));
		    // was "<p>");
		    pr.println(g_tit.gT("INDEX_TARGET1") +
			       // was "<a href=\"" + 
			       toc.link + 
			       g_tit.gT("INDEX_CRUMB2"));
			       // was "\" >");
			       pr.println(toc.toc_title);
			       pr.println(g_tit.gT("INDEX_CRUMB10_END"));
			       // was "</a></p>");
		}
	} // end if want TOC
	pr.flush();    
	pr.close();
	// file just created MAY BE EMPTY
        /*
         * now that the toc list is good, we
         * want to make the TOC.NCX file. Only
         * the special processor can do this
         */
        g_tit.createTOC(null, // no printwriter 
                          null, // no Map objects
                        g_toc_list, // 3rd param is a List, good
                        null); // last is a Map not used
    } // end end of document processing
    
    public PrintWriter getCurrentWriter()
    {
      /*  if (g_pr != null)
        {
            return g_pr;   // this seems to be the active one
        } 
        UNTIL WE CHANGE OTHERWISE!
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
        // states are currently in their own HTML files, one per state
    } // start states grouping
    
    public void endStates()
    {
        // states are currently in their own HTML files, one per state
    } // end states grouping
    
    public void startMainSection(
				String short_title) throws Exception
    {
        createPrefixHTML(g_pr); // page break now
        g_pr.println(g_tit.gT("INTRODUCTION_NAME"));
// was "<a name=\"_introduction\"></a>"); // marker for introduction
    }  // end start up main section
    
    public void endMainSection()
    {
    } // end terminate main section
    
    public void startAbbreviationSection(String app_name,
                                         String app_title,
					String short_title) throws Exception
    {
//        System.out.println("Abbreviations: " + app_title);
        
        createPrefixHTML(g_pr); // page break now
        g_pr.println(g_tit.gT("APPENDIX_NAME") +
                     // was "<a name=\"__" + 
                     app_name + 
                     g_tit.gT("ANCHOR_END"));
        // was "\"></a>");  
        g_toc_list.add(new TOCEntry(g_kindle_file + 
                                    g_tit.gT("APPENDIX_POINTER") +
                                    // was "#__" + 
                                    app_name,
                                    app_title));        
    }
    
    public void endAbbreviationSection(TreeMap abbrev_map,
                                       TreeMap state_index_map,
                                       TreeMap city_index_map,
                                       TreeMap fac_index_map,
                                       TreeMap general_index_map
                                       ) throws Exception
    {
        createAbbreviationIndex(g_pr,abbrev_map);
    } // end abbreviation section
    
    public void startAppendixSection(String appendix_name,
                                     String appendix_title,
				String short_title) throws Exception
    {
//        System.out.println("Appendix: " + appendix_title);
        /*
        * allow this appendix section to be found in the TOC
        * use two underlines before the appendix name to keep
        * uniqueness
        */
        createPrefixHTML(g_pr); // page break now
        
        g_pr.println(g_tit.gT("APPENDIX_NAME") +
                     // was "<a name=\"__" + 
                     appendix_name + 
                     g_tit.gT("ANCHOR_END"));
        // was "\"></a>");  
        g_toc_list.add(new TOCEntry(g_kindle_file + 
                                    g_tit.gT("APPENDIX_POINTER") +
                                    // was "#__" + 
                                    appendix_name,
                                    appendix_title));        
    }
    
    public void endAppendixSection(TreeMap state_index_map,
                                   TreeMap abbrev_map,
                                   TreeMap city_index_map,
                                   TreeMap fac_index_map,
                                   TreeMap general_index_map
                                   ) throws Exception
    {
    } // end appendix section
    
    public void startGeneralSection(String general_name,
                                     String general_title,
				String short_title,
			AuxiliaryInformation aux) throws Exception
    {
System.err.println("Starting general section: " + general_name + ", " +
general_title);
        createPrefixHTML(g_pr); // page break now
        g_pr.println(g_tit.gT("APPENDIX_NAME") +
                     // was "<a name=\"__" + 
                     general_name + 
                     g_tit.gT("ANCHOR_END"));
        // was "\"></a>");  
	g_pr.println("<h2>" + general_title + "</h2>");
        
        g_toc_list.add(new TOCEntry(g_kindle_file + 
                                    g_tit.gT("APPENDIX_POINTER") +
                                    // was "#__" +
                                    general_name,
                                    general_title));
        
        //System.out.println("General: " + general_title);
    }

	public void setOptions(Options op)
	{
		g_options = op;
	}
    
    
    public void endGeneralSection(TreeMap state_index_map,
                                   TreeMap abbrev_map,
                                   TreeMap city_index_map,
                                   TreeMap fac_index_map,
                                   TreeMap general_index_map
                                  ) throws Exception
    {
    } // end general section

    public void create_anchor(String anchor_name,PrintWriter pr) throws Exception
    {
        if (anchor_name != null)
        {
            pr.print(g_tit.gT("SIMPLE_ANCHOR_START") +
                   BookUtils.eC(anchor_name) + 
                   g_tit.gT("ANCHOR_END"));
        }
    }
    
    public void create_index_entries(List index_array,PrintWriter pr) throws Exception
    {
        if (index_array != null)
        {
            Iterator it = index_array.iterator();
            IndexRef ir = null;
            while (it.hasNext())
            {
                ir = (IndexRef)it.next();
                g_pr.println(g_tit.gT("GENERAL_ANCHOR_START2") +
                             // was "<a id=\"general_" + 
                             BookUtils.eC(ir.name) + "_" +
                             ir.getRefNumber()  +  
                             g_tit.gT("ANCHOR_END"));
                // was "\"></a>");   // put in <a> anchor MUST be separate for IE
                
            } // end for each index entry
        } // if embedded index(es) in a state,city,fac tag
    }
    
    /*
     * SINGLE file product, so a state is NOT a new file
     */
    public void startAState(String the_abbrev, 
                            String the_state,
                            String anchor_name,
                            List index_array,
                            TreeMap state_index_map,
                            TreeMap current_city_index_map,
                            TreeMap current_fac_index_map,
				String short_title) throws Exception
    {
        g_pr.print(g_tit.gT("KINDLE_SECTION_START"));
// was "<mbp:section>");
        createPrefixHTML(g_pr); // page break now
        //
        // if state has anchor, put it here before heading
        //
        create_anchor(anchor_name,g_pr);
        create_index_entries(index_array,g_pr);
        g_pr.print(g_tit.gT("STATE_HEADER_START") +
                   // was "<h2 id=\"state_" + 
                   BookUtils.eC(the_state) + 
                   g_tit.gT("HEADER_ID2") +
                   // was "\" >" + 
                   "State: " +
                   BookUtils.eT(the_state) + 
                   g_tit.gT("CITY_HEADER_END"));
        // was "</h2>\n");
        g_toc_list.add(new TOCEntry(g_kindle_file + 
                                    g_tit.gT("STATE_POINTER") +
                                    // was "#state_" +
                                    BookUtils.eC(the_state),BookUtils.eT(the_state) ));
        g_state_toc.add(new TOCEntry(g_kindle_file + 
                                     g_tit.gT("STATE_POINTER") +
                                     // was "#state_" +
                                     BookUtils.eC(the_state),BookUtils.eT(the_state) ));
    } // end startastate


    
    public void createStatePrefixHTML(PrintWriter pr,
                                      String state_name
                                      ) throws Exception
    {
        g_tit.createStaticHeaders(pr);
    }
    

    public void makeTableOfContents(PrintWriter pr)
    {
        
    } // end make table of contents

    
    /*
     * invoked for the last state seen.
     * a new state handler closes the previous, but
     * this happens at the end without a new state
     */
    public void endAState(TreeMap state_map,
                          TreeMap current_city_index_map,
                          TreeMap current_fac_index_map) throws Exception
    {
        g_pr.print(g_tit.gT("KINDLE_SECTION_END"));
// was "</mbp:section>");
    }

    public void createStatePostfixHTML(PrintWriter pr,
                                       String current_state,
                                       TreeMap state_index_map,
                                       TreeMap current_city_index_map,
                                       TreeMap current_fac_index_map) throws Exception
    {
    }
    
    
    public void startACity(String the_abbrev, 
                           String the_state,
                           String the_city,
                           String anchor_name,
                           List index_array) throws Exception
    {
        //
        // if city has anchor, put it here before heading
        //
        create_anchor(anchor_name,g_pr);
        create_index_entries(index_array,g_pr);
        
        g_pr.print(g_tit.gT("STATE_HEADER_START") +
                   // was "<h3 id=\"state_" + 
                   BookUtils.eC(the_state) + 
                   g_tit.gT("CITY_ANCHOR_MARKER") +
                   // was "_city_" + 
                   BookUtils.eC(the_city) + 
                   g_tit.gT("HEADER_ID2") +
                   // was "\" >" + 
                   "City: " +
                   BookUtils.eT(the_city) + 
                   g_tit.gT("CITY_HEADER_END"));
        // was "</h3>\n");
    } // end starta city
    
    public void endACity()
    {
    }
    
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
        //
        // if facility has anchor, put it here before heading
        //
        create_anchor(anchor_name,g_pr);
        create_index_entries(index_array,g_pr);
        g_pr.print(
            g_tit.gT("FACILITY_HEADER_START") +
            // was "<h4  id=\"state_" + 
            BookUtils.eC(the_state) + 
            g_tit.gT("CITY_ANCHOR_MARKER") +
            // was "_city_" + 
            BookUtils.eC(the_city) + 
            g_tit.gT("FACILITY_ANCHOR_MARKER") +
            // was "_fac_" + 
            BookUtils.eC(the_facility) + 
            g_tit.gT("HEADER_ID2") +
            // was \"\"  >" + 
            BookUtils.eT(the_facility) + 
            g_tit.gT("FACILITY_HEADER_END"));
        // was "</h4>\n");
        
        /*
         * code for creating HTML for the open dates, close dates, etc
         *
         * these lines are bunched together under the
         * "reference" css class. If shown as individual <p>
         * paragraphs, it takes up too much room
         */
        ArrayList all_refs = new ArrayList(5);
        if (source_page != null)
        {
            all_refs.add(g_tit.gT("SOURCE_PAGE_HEADER_START") +
                         // was "Source Page: " + 
                         source_page);
        }
        if (open_date != null)
        {
            all_refs.add(g_tit.gT("OPEN_DATE_HEADER_START") +
                         // was "Open: " + 
                         open_date);
        }
        if (close_date != null)
        {
            all_refs.add(g_tit.gT("CLOSE_DATE_HEADER_START") +
                         // was "Closed: " + 
                         close_date);
        }
        if (personnel_count != null)
        {
            all_refs.add(g_tit.gT("TROOP_COUNT_HEADER_START") +
                         // was "Maximum Troops: " + 
                         personnel_count);
        }
        if (hospital_admissions != null)
        {
            all_refs.add(g_tit.gT("HOSPITAL_ADMISSION_HEADER_START") +
                         // was "Hosp Admissions: " + 
                         hospital_admissions);
        }
        int ref_cnt = all_refs.size();
        if (ref_cnt != 0)
        {
   //         System.out.println("refs: " + all_refs);
            g_pr.print(g_tit.gT("INDENT_PARAGRAPH"));  // start a paragraph and break for each line
            // was "<p style=\"text-indent:0\">"); 
            for (int inner  = 0 ; inner < ref_cnt ; inner++)
            {
                g_pr.print(all_refs.get(inner));   // line contents
                // if NOT last line, add a break
                if (inner != (ref_cnt - 1))
                {
                    g_pr.print(g_tit.gT("LINE_BREAK"));
// was "<br />");  // break after each line
                }
            } // end for each ref item
            g_pr.print(g_tit.gT("PARAGRAPH_END"));
// was "</p>\n"); // terminate the "refs" lines
        } // end if some troop counts, etc lines needed
    } // end starta facility

    public void endAFacility(String the_state,
                             String the_city) throws Exception
    {
        /*
         * put a footer after each facility with links to the document, state, and city (and index)
         * put newlines between footer links
         */
        g_pr.print(g_tit.gT("FACILITY_FOOTER1") +
                   // was "<p ><a href=\"#TOC\">TOC</a>\n<a href=\"#state_" + 
                   BookUtils.eC(the_state) + 
                   g_tit.gT("INDEX_CRUMB2") +
                   // ws "\">" +
                   BookUtils.eT(the_state) + 
                   g_tit.gT("INDEX_CRUMB_END"));
        // was "</a>\n");
        g_pr.print(g_tit.gT("FACILITY_FOOTER2") +
                   // was "<a href=\"#state_" + 
                   BookUtils.eC(the_state) + 
                   g_tit.gT("CITY_ANCHOR_MARKER") +
                   // was "_city_" + 
                   BookUtils.eC(the_city) + 
                   g_tit.gT("HEADER_ID2") +
                   // was "\">" +
                   BookUtils.eT(the_city) + 
                   g_tit.gT("SIMPLE_ANCHOR_END"));
        // was "</a>\n");
        g_pr.print(g_tit.gT("INDEX_LINK_START") +
                   g_tit.gT("INDEX_LINK_END") +
                   g_tit.gT("PARAGRAPH_END"));
        // was "<a href=\"#index\">Index</a></p>\n"); // index for this state, then end of line
    } // end end a facility
    
    public void setSpecialTerminator(String st) // a kind of a state marker, often used in HTML output
    {
        g_tag_terminator = st; // specify it from outside
    }
    
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
        
        pr.print(g_tit.gT("PARAGRAPH_START"));
        // was "<p >");   // \n"); // starts new paragraph
        if (allow_span)
        {
            //            g_tag_terminator = "\n"; // no paragraph termination, someone else must do this
            print_terminator = false; // don't end paragraph at this point
        }
        //        else
        //      {
        g_tag_terminator = g_tit.gT("PARAGRAPH_END");  // if spanning, this terminator will be needed by the final code
        // was "</p>\n"; 
        //  }
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
            // was "</p>");
        }
        else
        {
            pr.println(); // just terminate line for readibility
        }
        // if spanning, let the following constructions print the paragraph terminator
//        pr.print(g_tag_terminator);  // will be end para, or just a newline, depending on "span" flag
    } // end start boilerplate text
    
    public void startText(String text,
		String anchor_content) throws Exception // start of sequence with inserted link, emphasis, etc
    {
        PrintWriter pr = getCurrentWriter();

        if (anchor_content != null)
        {
            pr.print(g_tit.gT("PARAGRAPH_ANCHOR_START1") +
                     // was "<p id=\"" + 
                     anchor_content +
                     g_tit.gT("TAG_ID_END"));
            // was "\" >"); // \n"); // starts new paragraph
        }
        else
        {
            pr.print(g_tit.gT("PARAGRAPH_START"));
            // was "<p >"); // \n"); // starts new paragraph
        }
        pr.print(BookUtils.eT(text));
        g_tag_terminator = g_tit.gT("PARAGRAPH_END"); 
        // was "</p>\n";   // would terminate this tag, someone else has to handle
    }
    
    public void startPREText(String text,
                          String anchor_content) throws Exception // start of sequence with inserted link, emphasis, etc
    {
        // main code seems to handle correctly
        startText(text,anchor_content);
    }
    
    public void endText(String text)  // last text in a sequence
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(BookUtils.eT(text));
        pr.print(g_tag_terminator);  // whatever was needed to terminate the sequence of text
    }
    
    public void setIndexLocation(List index_items) throws Exception
    {
        if (index_items == null)
        {
            return; // nothing to do
        }
        PrintWriter pr = getCurrentWriter();
        create_index_entries(index_items,pr);
    }
    
    public void setAnchor(String name) throws Exception // destination of a "go to"
    {
        //    System.out.println("Anchor: " + name);
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("SIMPLE_ANCHOR_START") +
                 // was "<a id=\""+ 
                 BookUtils.eC(name) + 
                 g_tit.gT("ANCHOR_END"));
        // was "\"></a>\n");  // must separate
    }
    
    public void insertSeparator(String anchor_content) throws Exception
    {
        // this is a separator
        PrintWriter pr = getCurrentWriter();
        if (anchor_content != null)
        {
            pr.print(g_tit.gT("PARAGRAPH_SEPARATOR_START1") +
                     // was "<hr id=\"" + 
                     anchor_content +
                     g_tit.gT("PARAGRAPH_SEPARATOR_END"));
            // was                   "\"  />\n");
        }
        else
        {
            pr.print(g_tit.gT("SEPARATOR_PARAGRAPH"));
            // was "<hr />\n");
        }
        
        g_tag_terminator = "\n";
    }
    
    public void endList() throws Exception // ends bulleted list
    {
        // this is the end of a list
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("END_LIST"));
        // was "</ul>\n");
        g_tag_terminator = "\n";
    }
    
    public void startList(String size) throws Exception  // start bulleted list
    {
	// ignore size in Kindle
        PrintWriter pr = getCurrentWriter();
        // this is the start of a list
        pr.print(g_tit.gT("START_LIST"));
        // was "<ul >\n");
        g_tag_terminator = "\n";
    }

// for list items that contain inner sequences (quote, link, etc)
    public void insertListItemStart(String text,
		String anchor_content) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        // start of item in list (usually "li")
        if (anchor_content != null)
        {
            pr.print(g_tit.gT("LIST_ITEM_START1") +
                     // was "<li id=\"" + 
                     anchor_content + 
                     g_tit.gT("TOC2"));
            // was "\">");
        }
        else
        {
            pr.print(g_tit.gT("LIST_ITEM_START2"));
            // was "<li>");
        }
        pr.print(BookUtils.eT(text));
        g_tag_terminator = "\n";  // no terminator tag yet
    }
    
    public void insertListItemEnd(String text) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        // end of list item
        pr.print(BookUtils.eT(text) + 
                 g_tit.gT("LIST_ITEM_END"));
// was "</li>\n");  // terminate the list item
    }
            
    public void insertListItem(String text,
		String anchor_content) throws Exception  // add bullet item
    {
        PrintWriter pr = getCurrentWriter();
        // this is a single item in a list
        if (anchor_content != null)
        {
            pr.print(g_tit.gT("LIST_ITEM_START1") +
                     // was "<li id=\"" + 
                     anchor_content + 
                     g_tit.gT("HEADER_ID2"));
            // was "\">");
        }
        else
        {
            pr.print(g_tit.gT("LIST_ITEM_START2"));
            // was "<li>");
        }
        
        pr.print( BookUtils.eT(text) + 
                  g_tit.gT("LIST_ITEM_END"));
        // was "</li>\n");
    }

    /*
     * override the HTML handler, we have our own
     * necessities of manifest content
     *
     * BUT this won't work at this stage of
     * the process. These manifest items are captured
     * in pre-processing the Author (or XML) file,
     * and actual internal Kindle links are not known
     *
     * After startAState, StartAGeneralSection, etc
     * have run (2nd pass of processing) do we know
     * these values. The g_toc_list is updated on the
     * fly by those methods.
     *
     * Soooo, we can only make a proper set of manifest items
     * at the end of processing. This is a "kindle-only" problem
     * and has chicken-egg issues.
     */
    public void addToManifest(String name, String title,
                              int manifest_flag)
    {
        // nothing for now
    }
    public void addToManifestdiscard(String name, String title,
	int manifest_flag)
	{
		switch (manifest_flag)
		{
			case MANIFEST_STATE:
			case MANIFEST_GENERAL:
			case MANIFEST_APPENDIX:
		{
		    TOCEntry toc = new TOCEntry(g_kindle_file + "#" +
			name,title);
                    if (title.equals("Introduction"))
                    {
                        // do NOT do the Introduction, it is hard-wired
                        System.err.println("DID NOT AddtoManifest, name: " + name +
                                           ", title: " + title +
                                           ", flag: " + manifest_flag);
                        break; // FORGET IT
                    }
		    g_toc_list.add(toc);
		System.err.println("AddtoManifest, name: " + name +
                           ", title: " + title +
                           ", flag: " + manifest_flag);
		System.err.println("toc: " + toc);
			break;
		} // end case ones we want
		} // end switch
    }
    /*
     * for Kindle, we advance the heading by 1 type number
     */
    public void createHeading(int type, String text,
		String anchor_content) throws Exception
    {
        String heading_type = String.valueOf(type + 1);
        PrintWriter pr = getCurrentWriter();
        if (anchor_content != null)
        {
            pr.print(g_tit.gT("HEADER_START") +
                     // was "<h" + 
                     heading_type + 
                     g_tit.gT("ANCHOR_START") +
                     // was " id=\"" + 
                     anchor_content + 
                     g_tit.gT("HEADER_ID2"));
            // was "\">");
        }
        else
        {
            pr.print(
                g_tit.gT("HEADER_START") +
                // was "<h" + 
                heading_type + 
                g_tit.gT("TAG_END"));
            // was ">");
        }
        pr.print(BookUtils.eT(text) + 
                 g_tit.gT("HEADER_END") +
                 // was "</h" + 
                 heading_type + 
                 g_tit.gT("TAG_END"));
        // was ">\n");
    }
    
    public void insertQuotedText(String text) throws Exception  // text in "middle" of a sequence with italics as a quote
    {
        PrintWriter pr = getCurrentWriter();
        /*
         * in spite of documentation to the contrary on the web,
         * it appears that many browsers DO NOT do anything
         * special with the "q" tag for quote...
         */
        pr.print(g_tit.gT("QUOTE_START") +
                 // was "<q>" + 
                 BookUtils.eT(text) + 
                 g_tit.gT("QUOTE_END"));
        // was "</q>");  // terminate quote text, no newline
    }
    
    public void insertCitedText(String text) throws Exception  // text in "middle" of a sequence with italics as a cite
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("CITE_START") +
                 // was "<q>" + 
                 BookUtils.eT(text) + 
                 g_tit.gT("CITE_END"));
//        pr.print("<cite>" + BookUtils.eT(text) + "</cite>");  // terminate quote text, no newline
    }
    
    public void insertBlockQuote(String text) throws Exception  // text in a separate paragraph, not in the "middle"
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("BLOCKQUOTE_START") +
                 // was "<q>" + 
                 BookUtils.eT(text) + 
                 g_tit.gT("BLOCKQUOTE_END"));
//        pr.print("\n<blockquote>" + BookUtils.eT(text) + "</blockquote>\n");  // a separate paragraph
    }
    
    public void insertEmphasizedText(String text) throws Exception // text in "middle" of a sequence which is bolded
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("EMPHASIZED_START") +
                 // was "<q>" + 
                 BookUtils.eT(text) + 
                 g_tit.gT("EMPHASIZED_END"));
//        pr.print("<b>" + BookUtils.eT(text) + "</b>");  // terminate emphasized text, no newline
    }

    public void insertIntermediateText(String text) // text in the "middle" of a sequence that is ordinary
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(BookUtils.eT(text) + "\n");  // nothing much, just newline (hope it looks OK)
    }
    
    public void insertLink(String href,
                           String text) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("INDEX_TARGET1") +
                 // was "<a href=\"" + 
                 BookUtils.eT(href) + 
                 g_tit.gT("INDEX_CRUMB2"));
        // was "\">");   // put in <a> anchor
        //
        // text that follows will be before the </a>
        //
        pr.print(BookUtils.eT(text) + 
                 g_tit.gT("INDEX_CRUMB_END"));
        // was "</a>");  // terminate HTML link, no newline
    }
    
    
    public void insertSimpleText(String content) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("PARAGRAPH_START") +
                 // was "<p >" + 
                 BookUtils.eT(content) + 
                 g_tit.gT("PARAGRAPH_END"));
        // was "</p>\n");
    }
    
    public void insertSeeAlso(
        String filename,
        String link,
        String content,
        String middle_text,
        String final_text) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("INDEX_CRUMB1") +
                 // was "<a href=\"" + 
                 // was "#" +
                 BookUtils.eC(link) + 
                 g_tit.gT("INDEX_CRUMB2"));
        // ws "\">");
        //        filename + 
        pr.print(BookUtils.eT(content) + 
                 g_tit.gT("INDEX_CRUMB_END"));
        // was "</a>");  
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
// was "</p>");
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
	if (remote)
	{
		throw new Exception("Kindle cannot use remote images! " +
			full_image_location);
	}
        PrintWriter pr = getCurrentWriter();
        /*
         * process image first, then text
         */
        if (full_image_location != null)
        {
            // create inline AND popup
            if (caption != null)
            {
                /*
                 * both image location and caption are given
                 * THIS IS THE NORM! Really should not have
                 * anything that does not have this information
                 */
                pr.print(g_tit.gT("IMAGE_ANCHOR_START"));
                // was "<p style=\"text-align:center;\" ><br /><img ");
                if (anchor_content != null)
                {
                    pr.print(
                        g_tit.gT("ANCHOR_START") +
                        // was " id=\"" +
                        anchor_content + 
                        g_tit.gT("TAG_ID_MIDDLE"));
                    // was "\" ");
                }
                pr.print(
                    g_tit.gT("IMAGE_LOC1") +
                    // was " alt=\"[" + 
                    BookUtils.eT(full_image_location) + 
                    g_tit.gT("IMAGE_SOURCE_START") +
                    // was "]\"  src=\"" +
                    IMAGE_LOCATION +    // may be empty or pics/ or thumbs/
                    BookUtils.eT(full_image_location) + 
                    g_tit.gT("IMAGE_LOC3"));
                // was "\"/>\n"); 
                makeNewLine(pr);
                makeNewLine(pr);    // add caption after inline image
                pr.print(g_tit.gT("IMAGE_CAPTION1") +
                         // was "<i>" + 
                         BookUtils.eT(addLineBreaks(caption)) + 
                         g_tit.gT("IMAGE_CAPTION2"));
                // was "</i>\n");   // new line first
            } // end caption specified
            else
            {
                throw new Exception("NO CAPTION: " + full_image_location);
             /*   System.err.println("NO CAPTION ON IMAGE: " + full_image_location);
                // popup window without caption
                if (anchor_content != null)
                {
                    pr.print("<p ><br /><a id=\"" +
                             anchor_content + "\"");
                }
                else
                {
                    pr.print("<p ><br /><a ");
                }
                pr.print("<img " +  // no title or caption
                         "alt=\"[" + 
                         BookUtils.eT(full_image_location) + "]\" " +
                         " src=\"" + IMAGE_LOCATION + // may be empty, or pics/ or thumbs/
                         BookUtils.eT(full_image_location) + "\"/></a>\n"); 
                // no caption follows in the inline HTML
            */
            } // end no caption
        } // end full image location provided
        else
        {
            throw new Exception("IMAGE LOCATION NULL: " + thumb_image_location);
         /*   // full image location is null, what gives?
            // no popup window, everything inline
            if (anchor_content != null)
            {
                pr.print("<p ><br /><img id=\"" +
                         anchor_content + "\"");
            }
            else
            {
                pr.print("<p ><br /><img ");
            }
            pr.print(" alt=\"[" + 
                     BookUtils.eT(full_image_location) + 
"]\" style=\"color:black;border:solid;border-width:2px\"" +
                     " src=\"thumbs/" + 
                     BookUtils.eT(thumb_image_location) + "\"/>\n"); 
            
            if (caption != null)
            {
           */     /*
                 * there is a caption, put it in after the image, letting the css handle the formatting
                 * CAPTION MAY CONTAIN LINE BREAKS!!
                 */
             /*   makeNewLine(pr);
                makeNewLine(pr);
                pr.print("<i>" + BookUtils.eT(addLineBreaks(caption)) + "</i>\n");   // new line first
            }
            */
            } // end full image location null
        pr.print(g_tit.gT("PARAGRAPH_END"));
// was "</p>\n");   // finish up the paragraph for the image
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
        String newer = x.replaceAll("::",
                                    g_tit.gT("SIMPLE_LINE_BREAK"));
// was "<br />");   // replace every double colon with HTML newline
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
            res.append("'" + xx[inner].replaceAll("'"," ") + "'"); // SORRY no single quotes
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
// was "<br />\n");   // new line
    }
    
    public void createPrefixHTML(PrintWriter pr) throws Exception
    {
        g_tit.createStaticHeaders(pr); // only page break at this time
    }
        
    public void createPostfixHTML(PrintWriter pr, 
                                  boolean make_index,
                                  TreeMap state_index_map,
                                  TreeMap abbrev_map,
                                  TreeMap city_index_map,
                                  TreeMap fac_index_map,
                                  TreeMap general_index_map
                                  ) throws Exception
    {
        
        pr.print(g_tit.gT("SEPARATOR_PARAGRAPH"));
     // was   pr.print("<hr />\n");
        
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
         * NOTE historically, was included a "target" in the index, because
         * content for HTML was spread around various locations. For Kindle, we
         * have a simplified HTML, only ONE file. Thus, all links are non-target,
         * having only a "#link" location within the same HTML file. TARGET
         * values are ignored with Kindle
         */
        pr.print(" <!-- start postfix -->");  // no div
        
	if (g_options.wantAnyIndex())
        {
            List the_root = null; // root of the indexgroup items
            IndexGroup current_group = null; // will be the one we are working on right now
            
            Iterator inner2 = null;
            Iterator inner = null;
            boolean first_time;
            String index_item = "";
            List some_items = null;
            ExactReferenceComparable the_ref = null;
            String a_title = "";
            FacilityReference the_fac = null;
            ExactReference ref2 = null;
            ExactFacilityReferenceComparable the_ref2 = null;
            String initial_letter = "";   // used for making subheadings to break up long index
            String current_initial_letter = "";  
            Iterator facs_it = null;
            FacilityReference fac_ref = null;
            TreeMap facility_links = null;   
            
            
            the_root = new ArrayList(10); // will contain IndexGroups
            List facility_root = new ArrayList(); // facilities ONLY (will make other files)
            // GENERAL INDEX FIRST
            //
	if (g_options.wantGeneralIndex())
            {
                current_group = BookUtils.createGeneralIndex(g_tit,general_index_map);
                the_root.add(current_group); // add to list
                
                g_toc_list.add(new TOCEntry(g_kindle_file + "#" +
                                            g_tit.gT("GENERAL_INDEX_ID"),
                                            // was _general_index",
                                            g_tit.gT("GENERAL_INDEX_TITLE")));
// was "General Index"));
              /*  current_group = new IndexGroup(
                    g_tit.gT("GENERAL_INDEX_ID"),
                    g_tit.gT("GENERAL_INDEX_TITLE"),
                    g_tit.gT("GENERAL_INDEX_TITLE"),
                    false, // no crumbs
                    g_tit.gT("GENERAL_INDEX_ID"),"General");
                the_root.add(current_group); // add to list
                
                // NOW populate the index group
                
                int colon_location = 0;
                
                IndexRefComparable current_index_item  = null;
                GeneralIndexReference index_ref = null;
                inner = general_index_map.keySet().iterator();
                while (inner.hasNext())
                {
                    index_item = (String)inner.next();
                    inner2 = ((List)general_index_map.get(index_item)).iterator();
                    while (inner2.hasNext())
                    {
                        index_ref = (GeneralIndexReference)inner2.next();
                        
                         * construct the text for the general index item. Strange stuff:
                         * city can be null
                         * "state" can be "appendixz" where "z" is letter a-z
                         
                        StringBuffer bb = new StringBuffer(index_item + " (");
                        if (index_ref.city_name == null)
                        { // dont bother with city, its null
                        }
                        else
                        {
                            if (index_ref.state_abbrev.startsWith("appendix"))
                            { // dont use the appendix as a city name, see later
                            }
                            else
                            {
                                bb.append(index_ref.city_name + ", ");
                            }
                        }
                        if (index_ref.state_abbrev.startsWith("appendix"))
                        {
                            bb.append("Appendix " + index_ref.state_abbrev.substring(8).toUpperCase());
                        }
                        else
                        {
                            bb.append(index_ref.state_abbrev);
                        }
                        bb.append(")");
                        current_group.children.add(new IndexEntry(
                            "general_" + BookUtils.eC(index_item) + 
                            "_" + index_ref.ref_number, // ID contains escaped name and ref number
                            bb.toString(),  // previously built
                     //       index_item + " (" + index_ref.city_name + ", " + index_ref.state_abbrev + ")",  // text name
                            index_ref.state_abbrev // becomes target file IGNORED
                            ));
                    } // end for each reference
                } // end for each general index item
            */
            }
	if (g_options.wantStateIndex())
            {
                // STATE INDEX
                //
                current_group = BookUtils.createStateIndex(
                    g_tit,state_index_map);
                the_root.add(current_group); // add to list
                g_toc_list.add(new TOCEntry(g_kindle_file + "#" +
                                            g_tit.gT("STATE_INDEX_ID"),
                                            // was _state_index",
                                            g_tit.gT("STATE_INDEX_TITLE")));
                               // was "State Listing"));
            /*    current_group = new IndexGroup(
                    g_tit.gT("STATE_INDEX_ID"),
                    g_tit.gT("STATE_INDEX_TITLE"),
                    g_tit.gT("STATE_INDEX_TITLE"),
                    false, // no crumbs
                    g_tit.gT("STATE_INDEX_ID"),"States");
                the_root.add(current_group); // add to list
                
                inner = state_index_map.keySet().iterator();
                
                while (inner.hasNext())
                {
                    index_item = (String)inner.next();  // state name
                    // ONLY ONE STATE PER ENTRY
                    some_items = (List)state_index_map.get(index_item);
                    the_ref = new ExactReferenceComparable((ExactReference)some_items.get(0)); // first item only
                    
                    // we are only processing state items
                    current_group.children.add(new IndexEntry(
                        "state_" + 
                        BookUtils.eC(the_ref.state_name),  // escape state name
                        index_item,  // text name
                        the_ref.state_abbrev // becomes target file IGNORED
                        ));
                } // end for each state index item
            */
            }
	if (g_options.wantCityIndex())
            {
                current_group = BookUtils.createOverallCityIndex(
                    g_tit,
                    city_index_map); // all entries
                the_root.add(current_group); // add top to the list
                g_toc_list.add(new TOCEntry(g_kindle_file + "#" +
                                            g_tit.gT("CITY_INDEX_ID"),
                                            // was _city_index",
                                            g_tit.gT("CITY_INDEX_TITLE")));
                               // was "City Index"));
/*                current_group = new IndexGroup(
                    g_tit.gT("CITY_INDEX_ID"),
                    g_tit.gT("CITY_INDEX_TITLE"),
                    g_tit.gT("CITY_INDEX_TITLE"),
                    true, // we will treat these items as breadcrumbs in the renderer
                    g_tit.gT("CITY_INDEX_ID"),"Cities");  // link for return to top
                the_root.add(current_group); // add top to the list
                IndexGroup letter_group = null; // one more level are the lettered groups
                
                // CITY INDEX, grouped by initial letter
                
                first_time = true;
                
                
                String city_initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
                String city_current_initial_letter = "ZZZZZ";  
                
                 * in order to make a set of bookmarks with the alphabet letters (easier navigation
                 * to alphabet headings), we must gather them up for later placement
                 
                TreeMap city_links = new TreeMap();        
                
                inner = city_index_map.keySet().iterator();
                
                while (inner.hasNext())
                {
                    index_item = (String)inner.next();  // city name
                    city_initial_letter = index_item.substring(0,1);
                    if (!city_initial_letter.equals(city_current_initial_letter))
                    {
                        
                         * New letter, we make a new bookmark grouping
                         
                        //                        city_links.put(city_initial_letter,"Cities '" + city_initial_letter + "'"); // index name "Cities A" etc
                        city_links.put(city_initial_letter,"'" + city_initial_letter + "'"); // index name "'A'" etc
                        city_current_initial_letter = city_initial_letter;
                    }
                } // end for each item in city map
                
                
                 * start over again. We will create each city bookmark grouping separately
                 
                inner = city_index_map.keySet().iterator();
                city_current_initial_letter = "ZZZZ";
                
                while (inner.hasNext())
                {
                    index_item = (String)inner.next();  // city name
                    city_initial_letter = index_item.substring(0,1);
                    if (!city_initial_letter.equals(city_current_initial_letter))
                    {
                        
                         * NEW INITIAL letter! Start new bookmark group
                         
                        if (first_time)
                        {
                            first_time = false;
                        }
                        else
                        {
                        }
                        letter_group = new IndexGroup(
                            "_city_" + city_initial_letter,
                            (String)city_links.get(city_initial_letter) + " -- ", // short name is letter only and separator
                            "Cities -- " + (String)city_links.get(city_initial_letter),
                            false, // no crumbs
                            "_city_" + city_initial_letter,
                            "Cities " + city_initial_letter
                            ); // jump back is to top of letter
                        current_group.children.add(letter_group); // main group gets the initial letter
                        city_current_initial_letter = city_initial_letter;
                    } // end if different initial letter seen
                    
                    // CAN BE SEVERAL CITIES PER ENTRY
                    some_items = (List)city_index_map.get(index_item);
                    inner2 = some_items.iterator();
                    while (inner2.hasNext())
                    {
                        
                        the_ref = new ExactReferenceComparable((ExactReference)inner2.next()); 
                        
                        // we are only processing city items
                        letter_group.children.add(new IndexEntry("state_" +
                                                                 BookUtils.eC(the_ref.state_name) +  // escape state name
                                                                 "_city_" +
                                                                 BookUtils.eC(the_ref.city_name),   // escape city name
                                                                 index_item +
                                                                 "  (" + the_ref.state_abbrev + ")", 
                                                                 the_ref.state_abbrev // becomes target file IGNORED
                                                                 ));
                        
                    } // end for each city with same name
                } // end for each city index item
*/
            }
	if (g_options.wantFacilityIndex())
            {
                current_group = BookUtils.createOverallFacilityIndex(
                    g_tit,
                    fac_index_map); // all entries
                the_root.add(current_group); // add top to the list
                g_toc_list.add(new TOCEntry(g_kindle_file + "#" +
                                            g_tit.gT("FACILITY_INDEX_ID"),
                                            // was _facility_index",
                                            g_tit.gT("FACILITY_INDEX_TITLE")));
                               // was "Facility Index"));
/*                current_group = new IndexGroup(
                    g_tit.gT("FACILITY_INDEX_ID"),
                    g_tit.gT("FACILITY_INDEX_TITLE"),
                    g_tit.gT("FACILITY_INDEX_TITLE"),
                    true, // we will treat these items as breadcrumbs in the renderer
                    g_tit.gT("FACILITY_INDEX_ID"),"Facilities"  // link for return to top
                    );

                the_root.add(current_group); // add top to the list
                IndexGroup letter_group = null; // one more level are the lettered groups
                
                // FACILITY INDEXES (One for each initial letter)
                
                
                first_time = true;
                
                
                initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
                current_initial_letter = "ZZZZZ";  
                
                facs_it = fac_index_map.keySet().iterator();
                
                fac_ref = null;
                
                 * in order to make a set of bookmarks with the alphabet letters (easier navigation
                 * to alphabet headings), we must gather them up for later placement
                 
                facility_links = new TreeMap();        
                while (facs_it.hasNext())
                {
                    fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
                    initial_letter = fac_ref.facility_name.substring(0,1);
                    if (!initial_letter.equals(current_initial_letter))
                    {
                        
                         * New letter, we make a new bookmark grouping
                         
                        facility_links.put(initial_letter,"'" + initial_letter + "'"); // index name "'A'" etc
                        //                        facility_links.put(initial_letter,"Facilities '" + initial_letter + "'"); // index name "Facilities A" etc
                        current_initial_letter = initial_letter;
                    }
                } // end for each item in facility map
                
                
                 * start over again. We will create each facility bookmark grouping separately
                 
                facs_it  = fac_index_map.keySet().iterator();
                current_initial_letter = "ZZZZZ";  
                
                while (facs_it.hasNext())
                {
                    fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
                    initial_letter = fac_ref.facility_name.substring(0,1);
                    if (!initial_letter.equals(current_initial_letter))
                    {
                        
                         * NEW INITIAL letter! Start new bookmark group
                         
                        if (first_time)
                        {
                            first_time = false;
                        }
                        else
                        {
                        }
                        letter_group = new IndexGroup(
                            "_facility_" + initial_letter,
                            (String)facility_links.get(initial_letter) + " -- ", // short name is letter only and separtor
                            "Facilities -- " + (String)facility_links.get(initial_letter),
                            false, // no crumb
                            "_facility_" + initial_letter,
                            "Facility " + initial_letter);
                        current_group.children.add(letter_group); // main group gets the initial letter
                        
                        current_initial_letter = initial_letter;
                    } // end if different initial letter seen
                    
                    inner2 = ((List)fac_index_map.get(fac_ref)).iterator();
                    while (inner2.hasNext())
                    {
                        ref2 = (ExactReference)inner2.next();
                        
                         * must build a reference that has facility in it, to
                         * use as a key to the page location to go to
                         
                        the_ref2 = new ExactFacilityReferenceComparable(
                            ref2.state_abbrev,ref2.state_name,ref2.city_name,
                            BookUtils.createFacilityName(fac_ref,BookUtils.FOR_TEXT,false));
                        
                        // we are only processing facility items
                        
                        
                        a_title = BookUtils.createFacilityName(fac_ref,
                                                               BookUtils.FOR_HTML,  // must escape
                                                               false) + 
", " + ref2.state_abbrev;
                        letter_group.children.add(new IndexEntry("state_" +
                                                                 BookUtils.eC(the_ref2.state_name) +  // escape state name
                                                                 "_city_" +
                                                                 BookUtils.eC(the_ref2.city_name) +   // escape city name
                                                                 "_fac_" +
                                                                 BookUtils.eC(the_ref2.fac_name),   // escape facility name
                                                                 a_title,
                                                                 the_ref2.state_abbrev // becomes target file IGNORED
                                                                 ));
                    } // end for each facility with same name
                } // end for each facility index item
                facility_root.add(current_group); // add one item ONLY to this index listing
                */
            }
	if (g_options.wantNoPostalHistoryIndex())
            {
                current_group = BookUtils.createOverallNOFacilityIndex(
                    g_tit,
                    fac_index_map); // all entries
                the_root.add(current_group); // add top to the list
                g_toc_list.add(new TOCEntry(g_kindle_file + "#" +
                                            g_tit.gT("FACILITY_NO_INDEX_ID"),
                                            // was _facilityno_index",
                                            g_tit.gT("FACILITY_NO_INDEX_TITLE")));
                               // was "Index of Facilities with No Postal History"));
/*                current_group = new IndexGroup(
                    g_tit.gT("FACILITY_NO_INDEX_ID"),
                    g_tit.gT("FACILITY_NO_INDEX_TITLE"),
                    g_tit.gT("FACILITY_NO_INDEX_TITLE"),
                    true, // we will treat these items as breadcrumbs in the renderer
                    g_tit.gT("FACILITY_NO_INDEX_ID"),"Facilities No "  // link for return to top
                    );
                the_root.add(current_group); // add top to the list
                IndexGroup letter_group = null; // one more level are the lettered groups
                
                // FACILITY (NO POSTAL HISTORY) INDEXES (One for each initial letter)
        
                first_time = true;
                
                the_fac = null;
                ref2 = null;
                the_ref2 = null;
                
                initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
                current_initial_letter = "ZZZZZ";  
                
                facs_it = fac_index_map.keySet().iterator();
                
                fac_ref = null;
                
                 * in order to make a set of bookmarks with the alphabet letters (easier navigation
                 * to alphabet headings), we must gather them up for later placement
                 
                facility_links = new TreeMap();        
                while (facs_it.hasNext())
                {
                    fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
                    if (BookUtils.someNoPostalHistory((List)fac_index_map.get(fac_ref)))
                    {
                        // at least one item has no postal history, so consider adding those that are desirable
                        
                        initial_letter = fac_ref.facility_name.substring(0,1);
                        if (!initial_letter.equals(current_initial_letter))
                        {
                            
                             * New letter, we make a new bookmark grouping
                             
                            facility_links.put(initial_letter,"'" + initial_letter + "'"); // index name "Facilities (NO) A" etc
                            //                            facility_links.put(initial_letter,"Facilities (NO) '" + initial_letter + "'"); // index name "Facilities (NO) A" etc
                            current_initial_letter = initial_letter;
                        }
                    } // end if at least one internal entry is a NO Postal History
                } // end for each item in facility map
                
                
                
                 * start over again. We will create each facility bookmark grouping separately
                 
                facs_it  = fac_index_map.keySet().iterator();
                current_initial_letter = "ZZZZZ";  
                
                while (facs_it.hasNext())
                {
                    fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
                    if (BookUtils.someNoPostalHistory((List)fac_index_map.get(fac_ref)))
                    {
                        // at least one item has no postal history, so consider adding those that are desirable
                        
                        
                        initial_letter = fac_ref.facility_name.substring(0,1);
                        if (!initial_letter.equals(current_initial_letter))
                        {
                            
                             * NEW INITIAL letter! Start new bookmark group
                             
                            if (first_time)
                            {
                                first_time = false;
                            }
                            else
                            {
                            }
                            letter_group = new IndexGroup(
                                "_facilityno_" + (String)facility_links.get(initial_letter),
                                (String)facility_links.get(initial_letter) + " -- ", // short name is letter only and separator
                                "Facilities (NO) -- " + (String)facility_links.get(initial_letter),
                                false, // no breadcrumb
                                "_facilityno_" + (String)facility_links.get(initial_letter),
                                "Facility NO " + (String)facility_links.get(initial_letter));
                            current_group.children.add(letter_group); // main group gets the initial letter
                            
                            current_initial_letter = initial_letter;
                        } // end if different initial letter seen
                        
                        inner2 = ((List)fac_index_map.get(fac_ref)).iterator();
                        while (inner2.hasNext())
                        {
                            ref2 = (ExactReference)inner2.next();
                            if (ref2.nopost)
                            {
                                // ONLY include inner items that have no postal history
                                
                                 * must build a reference that has facility in it, to
                                 * use as a key to the page location to go to
                                 
                                the_ref2 = new ExactFacilityReferenceComparable(
                                    ref2.state_abbrev,ref2.state_name,ref2.city_name,
                                    BookUtils.createFacilityName(fac_ref,BookUtils.FOR_TEXT,false));
                                
                                // we are only processing facility items
                                
                                
                                a_title = BookUtils.createFacilityName(fac_ref,
                                                                       BookUtils.FOR_HTML,  // must escape
                                                                       false) + 
", " + ref2.state_abbrev;
                                letter_group.children.add(new IndexEntry("state_" +
                                                                         BookUtils.eC(the_ref2.state_name) +  // escape state name
                                                                         "_city_" +
                                                                         BookUtils.eC(the_ref2.city_name) +   // escape city name
                                                                         "_fac_" +
                                                                         BookUtils.eC(the_ref2.fac_name),   // escape facility name
                                                                         a_title,
                                                                         the_ref2.state_abbrev // becomes target file IGNORED
                                                                         ));
                                
                            } // end if inner item has no postal history
                        } // end for each facility (NO) with same name
                    } // if at least one inner item has no postal history
                } // end for each facility (NO) index item
                */
            } // end if want facility (NO) index
            // the_root will be the entire thingie
            //        System.out.println(the_root); // this is what we pass to the renderer
            if (make_index)
            {
                g_tit.renderIndex(pr,the_root,IndexRenderer.COMPLETE_INDEX);
            } // end if any indexes wanted at all
        } // end if any indexes wanted at all
        pr.println(g_tit.gT("ENDING_INDEX_BOILERPLATE"));  // end of body and html
    } // end create postfix HTML
    

    public void createAbbreviationIndex(PrintWriter pr,
                                        TreeMap abbrev_map)
    {
        /*
         * make index listing of abbreviations
         */
        pr.print("<a id=\"abbreviation_index\"></a>\n");  // have to have separate items
        Iterator abbrev_it = null;
        String the_abbrev = null;
        String abbrev_def = null;
        String initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
        String current_initial_letter = "ZZZZZ";  
        /*
         * in order to make a heading with the alphabet letters (easier navigation
         * to alphabet headings), we must gather them up for later placement
         */
        TreeMap abbreviation_links = new TreeMap();        
        
        AbbreviationKey the_key = null;
        
        abbrev_it = abbrev_map.keySet().iterator();
        while (abbrev_it.hasNext())
        {
            the_key = (AbbreviationKey)abbrev_it.next();
            the_abbrev = the_key.comparison_key;   // lower case, no parens
            initial_letter = the_abbrev.substring(0,1);
            if (!initial_letter.equals(current_initial_letter))
            {
                // new letter
                abbreviation_links.put(initial_letter,"abbreviation_" + initial_letter);
                current_initial_letter = initial_letter;
            }
        } // end for each abbreviation in the index
        /*
         * now, print a header with links to all of the found initials
         */
        pr.println("<p>");
        Iterator initials_it = abbreviation_links.keySet().iterator();
        while (initials_it.hasNext())
        {
            current_initial_letter = (String)initials_it.next(); 
            pr.println("<a href=\"#" + abbreviation_links.get(current_initial_letter) +
                       "\"> .. " + current_initial_letter.toUpperCase() + " .. </a>");
        }
        pr.println("</p>"); // end the special link list
        /*
         * pass again, printing each abbreviation
         */
        abbrev_it = abbrev_map.keySet().iterator();
        current_initial_letter = "ZZZZZ";  
        ArrayList all_abbs = null; // will be filled and emptied for each intial letter
        
        
        while (abbrev_it.hasNext())
        {
            the_key = (AbbreviationKey)abbrev_it.next();
            the_abbrev = the_key.the_abbreviation;
            initial_letter = the_key.comparison_key.substring(0,1);
            if (!initial_letter.equals(current_initial_letter))
            {
                if (!current_initial_letter.equals("ZZZZZ"))
                {
                    // not first time, dump previous letter entries
                    if (all_abbs != null)
                    {
                        //  use 1-col layout for Kindle
                        makeAColumn(pr,all_abbs);
                    }
                    all_abbs = new ArrayList(100); // start again
                    pr.print("<p><a href=\"#abbreviation_index\">[top]</a></p>");
                }
                else
                {
                    // first time
                    all_abbs = new ArrayList(100); // start first time
                }
                // make heading
                pr.print("<h2 id=\"" + abbreviation_links.get(initial_letter) +
                         "\" > -- " + initial_letter.toUpperCase() + " -- </h2>");
                current_initial_letter = initial_letter;
            } // end if initial letter changed
            
            /*
             * Note: there is only one abbrevition per treemap item. We grab the first regardless
             * NO NO THIS IS NOT TRUE! Because of the sorting BUG BUG that is
             * non-case-sensitive, there are items that match
             */
            // HERE HERE, we need to put in the defintion, like we do with FOP
            // NO POPUPS!
            abbrev_def = (String)((List)abbrev_map.get(the_key)).get(0);
            /*
             * HTML is a link that pops up the definition
             */
            // make popup window with caption
            all_abbs.add(
                BookUtils.eT(the_abbrev) + " -- " +
                BookUtils.eT(abbrev_def));
/*            all_abbs.add("<a href=\"#\" title=\"  Abbreviation: " +
                         BookUtils.eT(the_abbrev) + ",  Definition: " + 
                         BookUtils.eT(abbrev_def) +"\">" +
                         BookUtils.eT(the_abbrev) + "</a>");
*/
            //"\');\" 
//            javascript:pop_up_window(\'" +
  //                                   BookUtils.eT(BookUtils.escapeSingleQuotes(the_abbrev)) + "','" + 
    //                                 BookUtils.eT(BookUtils.escapeSingleQuotes(abbrev_def)) + 
        } // end for each abbreviation in the index
        /*
         * all abbreviation items exhausted. We have some items
         * from the last letter change. must dump them out
         */
        if (all_abbs != null)
        {
            // use 1-column layout for Kindle!
            makeAColumn(pr,all_abbs);
        }
        
        pr.print("<p><a href=\"#abbreviation_index\">[top]</a></p>");
    } //end create abbreviation index listing
    
       public  void makeAColumn(PrintWriter pr, List items)
    {
           pr.println("<p>");
        Iterator ii = items.iterator(); // simple single-column list
        while (ii.hasNext())
        {
            pr.println(ii.next() + "<br />");
        }
        pr.println("</p>");
       } // end make a column
       
       /*
    SUPPLANTED by public container of same name
    public class TOCEntry
    {
        public String link;
        public String toc_title;
        
        public TOCEntry(String l, String t)
        {
            link = l;
            toc_title = t;
        }
    }
    */
    
} // end  kindle html sink
