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
 * last edited 12/26/2018
 *
 * correct H2 header for General section to have an ID for
 * jumping purposes (TOC)
 *
 * remove dependency on old XMLUtils
 *
 * added ability to receive auxiliary metadata, but for a single
 * HTML file, all meta goes at the top. So we really cannot and
 * will not process this information.
 *
 * TODO:
 *
 * 1) indexes, if any wanted, go at the end of the entire document
 *    there should be a link at the top of the page
 *
 * 2) TOC, if wanted, goes at the end of the entire document, just
 *    before any indexes. There should be a link at the top of the page
 *
 * use options.json options area to drive this creation:
 *
 * 1) We will use a flag in the options.json file to
 *    determine HTML format, such as POEM
 *
 * 2) We will use a flag in the options.json file to
 *    determine whether we create a single HTML file, or one for
 *    each SECTION (or state if facilities book). WORKING
 *
 * 3) ALSO, we will use a flag in the options.json file to
 *    determine whether we create the FRONT material files. These
 *    include: cover page, title page, preface, front page, etc.
 *
 * add NOFacility index that is sorted by
 * state and city. Really cannot use the
 * existing NOF list as a checklist
 *
 * The code here is the sink for HTML
 * creation of a SINGLE page for an entire project.
 *
 * WARNING: this layout will probably not work well
 * for the big facilities BOOK, due to its size, and complexity.
 * it is really intended for newsletters and articles
 *
 *
 */
public class HTMLSingleSink extends HTMLSink
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
//        public List g_non_state_list = null;
        
    public String g_tag_terminator;  // needed to finish "some" HTML tags
 

	// default if nothing else
	public int g_html_format = HTMLContentCreator.FORMAT_POEM;
    
    public HTMLSingleSink() // NO PARAM
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
        
        try
        {
            /*
             * create the writer for the final HTML output
             * during testing, this is stdout
             */
            g_pr = new PrintWriter(new File("single_page" + g_file_extension)); // write to this file only
		g_state_pr = g_pr; // ALL OUTPUT to main stream
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
    } // end constructor
    
    /*
     * simple constructor, just passes through
     */
    public HTMLSingleSink(GenericSource sou)
    {   
    //    super(sou);
    }
    
    

	/*
	 * override parent, all output to main stream
	 */
    public void endDocument(
        TreeMap  state_index_map,
        TreeMap  abbrev_map,
        TreeMap  city_index_map,
        TreeMap  fac_index_map,
        TreeMap  general_index_map
        ) throws Exception
    {
/*
 * redundant??
        createPostfixHTML(g_pr,true,
                          state_index_map,
                          abbrev_map,
                          city_index_map,
                          fac_index_map,
                          general_index_map
                          ); // gets all of the indexes at this time
*/

	/*
	 * BEFORE we terminate all HTML output for this single page
	 * we need to create any indexes wanted. 
	 */
        createPostfixHTMLSingle(g_pr,true, // flag on for indexes
                          state_index_map,
                          abbrev_map,
                          city_index_map,
                          fac_index_map,
                          general_index_map
                          ); // gets all of the indexes at this time
	/*
	 * terminate all HTML output for this single page
	 * most of the section terminations so far have been dummy
	 */
        g_state_pr.print(g_tit.gT("END_BODY_FINAL"));
            
            /*
             * create a "manifest" file that can be used to correctly update the JSON
             * file used to create the table of contents for the HTML pages
             */
             
        g_pr.flush();  // flush out the HTML output stream
        g_pr.close();  // finish MAIN HTML output stream
        
	/*
	 * more for debugging, but we will write out
	 * the TOC manifest information.
	 */
        int the_size = g_toc_list.size();
        TOCEntry toc = null;
        PrintWriter pr = new PrintWriter(new File("htmlmanifest.json"));
       
        pr.println("[");  // start JSON list
        for (int position = 0 ; position < the_size ; position++)
//        while (ii.hasNext())
        {
            toc = (TOCEntry)g_toc_list.get(position);
System.err.println("TOC:" + toc); // debugging
                       
	   	pr.print("\"<a href=\\\"" +   toc.link);
   		pr.print("\\\">" + toc.toc_title + "</a>\""); // no comma yet
   		if (position != (the_size - 1))
   		{
   			pr.println(","); // add comma for all but last
   		}
          
        } // end for each item in the TOC list
        pr.println("]"); // end the JSON list
        pr.flush();    
        pr.close(); // END SPECIAL manifest output

    } // end of enddocument method
    
	/*
	 * override parent
	 */
    public PrintWriter getCurrentWriter()
    {
/*
        if (g_state_pr != null)
        {
            return g_state_pr;   // this seems to be the active one
        }
*/
        return g_pr; // ALWAYS using the primary output stream
    }
    
	/*
	 * override parent, use main output stream
	 */
    public void startMainSection(
        String short_title) throws Exception
    {
        /*
         * introduction content
         *
         * reuse the "state" writer reference NOT
         */
        g_state_pr = g_pr; // keep on main stream
        createPrefixHTML(g_state_pr,null);
                
        //        g_state_pr.print("<div> <!-- main section -->\n");
    }  // end start up main section
    
	/*
	 * override parent, keep output on main stream
	 */
    public void endMainSection() throws Exception
    {
        /*
         * finish main section
         */
//        g_state_pr.print(g_tit.gT("TOC_LINK1")); for now, no TOC
	//g_state_pr.println("<p><a href=\"#top\">Top</a></p>"); // get to top of page
        /*
         * the main section is simply text, usually the INTRODUCTION
         *
         * we need to terminate this page correctly
         */
        //g_state_pr.print(g_tit.gT("END_SECTION")); seems to be Skeleton code??
        //g_state_pr.print(g_tit.gT("END_BODY_FINAL"));
        //g_state_pr = g_pr; // keep main stream
        //
    } // end terminate main section
    
	/*
	 * override parent, keep on main output stream
	 */
    public void startAbbreviationSection(String app_name,
                                         String app_title,
                                         String short_title) throws Exception
    {
        /*
         * special abbreviations appendix, 
         * treated differently than a generic "appendix"
         */
        String appendix_name = app_name;
        /*
         * reusing the state writer NOT, since no state is printed
         * at the same time as the appendix
         */
        g_state_pr = g_pr; // keep on main output stream
        createPrefixHTML(g_state_pr,null);
        fullWidth(g_state_pr);
        
	/* should be done during first pass, not here
                g_toc_list.add(new TOCEntry(
		    app_name + ".html",
                                    app_title));        
	 */

    } // end start abbreviation section
    
	/*
	 * override parent, keep output on main stream
	 */
    public void endAbbreviationSection(TreeMap abbrev_map,
                                       TreeMap state_index_map,
                                       TreeMap city_index_map,
                                       TreeMap fac_index_map,
                                       TreeMap general_index_map
                                       ) throws Exception
    {
        /*
         * done with div
         */
        finishSkeletonx(g_state_pr);
// no longer needed        g_state_pr.print("</div> <!-- abbreviation section -->\n");
        createAbbreviationIndex(g_state_pr,abbrev_map);
        createPostfixHTML(g_state_pr,false,
                          state_index_map,
                          abbrev_map,
                          city_index_map,
                          fac_index_map,
                          general_index_map
                          );
        g_state_pr = g_pr; // keep on main stream
    } // end abbreviation section
    
    
	/*
	 * override parent, keep appendix output on same main stream
	 */
    public void startAppendixSection(String appendix_name,
                                     String appendix_title,
                                     String short_title) throws Exception
    {
        /*
         * appendix, goes on its own file, but
         * content is processed much like normal
         * content, with all <p> in this section
         */
        g_state_pr = g_pr; // keep on same stream
        createPrefixHTML(g_state_pr,null);
        fullWidth(g_state_pr);
        //   g_state_pr.print("<div> <!-- appendix section: " + appendix_name + " -->\n");
        // appendix_pr.print("<!-- start of appendix (simple content) section -->\n");
    } // end startappendix
    
	/*
	 * override parent KEEP OUTPUT ON MAIN STREAM
	 */
    public void endAppendixSection(TreeMap state_index_map,
                                   TreeMap abbrev_map,
                                   TreeMap city_index_map,
                                   TreeMap fac_index_map,
                                   TreeMap general_index_map
                                   ) throws Exception
    {
        /*
         * done with div
         */
        finishSkeletonx(g_state_pr);
        //        g_state_pr.print("</div> <!-- appendix section -->\n");
        createPostfixHTML(g_state_pr,false,
                          state_index_map,
                          abbrev_map,
                          city_index_map,
                          fac_index_map,
                          general_index_map
                          );
        g_state_pr = g_pr; // keep on same output stream
    } // end appendix section
    
	/*
	 * general section is in the main HTML stream, too
	 */
    public void startGeneralSection(String general_name,
                                    String general_title,
                                    String short_title,
				AuxiliaryInformation aux) throws Exception
    {
        /*
         * general section, goes on its own file (NOT), but
         * content is processed much like normal
         * content, with all <p> in this section
         */
        g_state_pr = g_pr; // use MAIN STREAM
        createPrefixHTML(g_state_pr,null);
        fullWidth(g_state_pr);
        //   g_state_pr.print("<div> <!-- appendix section: " + appendix_name + " -->\n");
        // appendix_pr.print("<!-- start of appendix (simple content) section -->\n");
	/* 
	 * should be done during first pass, not here
              g_toc_list.add(new TOCEntry(
		  general_name + ".html",
                                    general_title));
	 */
		// print header for this SECTION
	        g_state_pr.print(g_tit.gT("HEADER_START") +
		"2" + // type 2 header seems right
                g_tit.gT("ANCHOR_START") +
		"_" + general_name +  // preceding _ for uniqueness
                g_tit.gT("HEADER_ID2")); // make header2 with an ID for TOC jumping
		g_state_pr.print(BookUtils.eT(general_title) + 
                 g_tit.gT("HEADER_END") +
		"2" + // type 2 header seems right
                 g_tit.gT("TAG_END"));
    } // end startgeneral
    
	/*
	 * override parent
	 */
    public void endGeneralSection(TreeMap state_index_map,
                                  TreeMap abbrev_map,
                                  TreeMap city_index_map,
                                  TreeMap fac_index_map,
                                  TreeMap general_index_map
                                  ) throws Exception
    {
        /*
         * done with div
         */
        finishSkeletonx(g_state_pr);
        //        g_state_pr.print("</div> <!-- appendix section -->\n");
        createPostfixHTML(g_state_pr,false,
                          state_index_map,
                          abbrev_map,
                          city_index_map,
                          fac_index_map,
                          general_index_map
                          );
        g_state_pr = g_pr; // KEEP AS MAIN OUTPUT
    } // end general section
    
	/*
	 * override parent
	 * 
	 * we KEEP OUTPUT on the same main output file 
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
        /*
         * if already processing a state, finish it up
         */
        if (g_state_pr == null)
	{
		throw new Exception("startastate: output stream null");
	}
	else
        {
            // one exists, close it
            // DEBUG             g_pr.println("<!-- " + current_city_index_map + " -->");
            createStatePostfixHTML(g_state_pr,
                                   g_current_state,
                                   state_index_map,
                                   current_city_index_map,
                                   current_fac_index_map);
		// DO NOT CLOSE
        }
        g_state_pr = g_pr; // KEEP AS MAIN output
        g_current_state = the_state; // hold for later postfix HTML content
            
        createStatePrefixHTML(g_state_pr,the_state);
            
        /*
         * this is a new state, so a new div
         */
        //        g_state_pr.print("<div> <!-- state " + the_abbrev + " -->\n");
        // TESTING using the URL encoder to make good anchor names
        //
        // if state has anchor, put it here before heading
        //
        create_anchor(anchor_name,g_state_pr);
        create_index_entries(index_array,g_state_pr);
        fullWidth(g_state_pr);
        g_state_pr.print(g_tit.gT("STATE_HEADING_START") +
                         BookUtils.eC(the_state) + 
                         g_tit.gT("HEADER_ID2") +
                         BookUtils.eT(the_state) + 
                         g_tit.gT("HEADER1_END"));

    } // end startastate
    
    /*
     * override parent. state postfix is simple, we DON'T use
     * indexes when all is one HTML file
     */
    public void createStatePostfixHTML(PrintWriter pr,
                                       String current_state,
                                       TreeMap state_index_map,
                                       TreeMap current_city_index_map,
                                       TreeMap current_fac_index_map) throws Exception
    {
    	pr.println("<!-- entering create state postfix html -->\n");
        fullWidth(pr);
        
        g_tit.endPageSeq(pr);
    } // end postfix writing for each state entry

    /*
     * override parent. We MUST KEEP writing to the main html file
     * 
     * invoked for the last state seen.
     * a new state handler closes the previous, but
     * this happens at the end without a new state
     */
    public void endAState(TreeMap state_map,
                          TreeMap current_city_index_map,
                          TreeMap current_fac_index_map) throws Exception
    {
        if (g_state_pr == null)
	{
		throw new Exception("endastate: file stream null");
	}
	else
        {
            /*
             * to this point, all content of the state section
             * has been wrapped in a full width skeleton <div>
             *
             * end it
             */
            finishSkeletonx(g_state_pr);
            
            // one exists, close it
            // DEBUG             g_pr.println("<!-- " + current_city_index_map + " -->");
// FOLLOWING NEEDED?
            createStatePostfixHTML(g_state_pr,
                                   g_current_state,
                                   state_map,
                                   current_city_index_map,
                                   current_fac_index_map);
            g_state_pr = g_pr; // NO CHANGE
        } // if print is not null (had better be main output file)
    } // end endastate

	/*
	 * override parent, no indexes
	 * these are done AT THE VERY END, in a separate method
	 */
    public void createPostfixHTML(PrintWriter pr, 
                                  boolean make_all_indexes,
                                  TreeMap state_index_map,
                                  TreeMap abbrev_map,
                                  TreeMap city_index_map,
                                  TreeMap fac_index_map,
                                  TreeMap general_index_map
                                  ) throws Exception
    {
        fullWidth(pr); 
                finishSkeletonx(pr);

        pr.print(" <!-- start postfix -->\n");  // no div
        
        fullWidth(pr);    
        makeNewLine(pr);
pr.println("<p><a href=\"#top\">Top</a></p>"); // get to top of page
        finishSkeletonx(pr);
        pr.print(g_tit.gT("SKELETON_PAGE_CLOSE"));
    } // end create postfix HTML

	/*
	 * override parent
	 */
    public void startDocument(SpecialContentCreator tit) throws Exception
    {
	/*
	 * FIRST THINGS FIRST:
	 * 
	 * We can populate some fixed items. 
	 * 
	 * These TOC items are pre-created, so we know who they are
	 *
	 * ALL OTHER TOC links are created during the first pass
	 */
	// index is special case, so we don't use the common code
	if (g_options.wantAnyIndex())
	{
		g_toc_list.add(new TOCEntry(
		"#index","Document Indexes"));
	}
	// rest use common code, it adds the ".html" to the link
	if (g_options.wantCoverPage())
	{
		addToManifest("cover_page","Cover Page",MANIFEST_GENERAL);
	}
	if (g_options.wantTitlePage())
	{
		addToManifest("title_page","Title Page",MANIFEST_GENERAL);
	}
	if (g_options.wantFrontMaterialPage())
	{
		addToManifest("front_page","Front Matter",MANIFEST_GENERAL);
	}
	if (g_options.wantPrefacePage())
	{
		addToManifest("preface_page","Preface for HTML (Web) Users"
		,MANIFEST_GENERAL);
	}
	// done during first pass            addToManifest("introduction.html","Introduction");

	g_tit = tit; // make global, used a lot
        /*
         * first use, grab the format, which only the 
	 * SpecialContentCreator knows
         */
	String aform = g_tit.getProperty("HTML_FORMAT");
	if (aform.equalsIgnoreCase("poem"))
	{
		g_html_format = HTMLContentCreator.FORMAT_POEM;
	}
	if (aform.equalsIgnoreCase("skel"))
	{
		g_html_format = HTMLContentCreator.FORMAT_SKELETON;
	}
	// none of the above, problems
        /*
         * create the title page
         */
        g_tit.createTitlePage(g_pr,null); // writes to the MAIN file
        /*
         * write start of HTML
         */
        createPrefixHTML(g_pr,null);
    } // end startDocument

	/*
	 * override parent
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
         * we are making ONE one file, so filename means nothing here
         */
        if (filename == null)
        {
            filename = "index.html"; // KLUGE default
            System.out.println("WARN: see_also inserted index.html filename for: " +
                               link + ", " + content);
        }
        PrintWriter pr = getCurrentWriter();
        //pr.print(g_tit.gT("INDEX_TARGET1") +
        pr.print(g_tit.gT("INDEX_SEEALSO_TARGET1") + // special formatting
                 "#" + // NO FILENAME
                 //filename + "#" +
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

	/*
	 * split out the Index creation function from the end HTML
	 * creation process. We suppress "end HTML" processing for
	 * nearly all sections, states, and appendixes.
	 */
    public void createPostfixHTMLSingle(PrintWriter pr, 
                                  boolean make_all_indexes,
                                  TreeMap state_index_map,
                                  TreeMap abbrev_map,
                                  TreeMap city_index_map,
                                  TreeMap fac_index_map,
                                  TreeMap general_index_map
                                  ) throws Exception
    {
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
        
	if (g_options.wantAnyIndex())
        {
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
            ExactFacilityReferenceComparableByCity the_ref2 = null;
            String initial_letter = "";   // used for making subheadings to break up long index
            String current_initial_letter = "";  
            Iterator facs_it = null;
            FacilityReference fac_ref = null;
            TreeMap facility_links = null;   
            
            List the_root = new ArrayList(10); // will contain all desired IndexGroups
            List facility_root = new ArrayList(); // facilities index ONLY (program makes additional files)
	/*
	 * Before anything is printed, we must populate
	 * the index content objects. Then, we decide which
	 * index is sent to the renderer.
	 * 
	 * There are various ways in HTML we have indexes: 
	 * 1) if facility index wanted, we have a separate
	 *    web page
	 * 2) if facility index wanted, we have a separate pop-up
	 *    web page
	 * 3) if at end of a project, we want all indexes together
	 * 
	 * (4 -- indexes at bottom  of each state page) 
	 */
            // GENERAL INDEX FIRST
            //
	if (g_options.wantGeneralIndex())
            {
//                current_group = createGeneralIndex(g_tit,general_index_map); // use local code
                current_group = BookUtils.createGeneralIndex(g_tit,general_index_map);
                the_root.add(current_group); // add to list
                
            }
	if (g_options.wantStateIndex())
            {
                // STATE INDEX
                //
                current_group = BookUtils.createStateIndex(
                    g_tit,state_index_map);
                the_root.add(current_group); // add to list
            }
	if (g_options.wantCityIndex())
            {
                current_group = BookUtils.createOverallCityIndex(
                    g_tit,
                    city_index_map); // all entries
                the_root.add(current_group); // add top to the list
            }
	if (g_options.wantFacilityIndex())
            {
                current_group = BookUtils.createOverallFacilityIndex(
                    g_tit,
                    fac_index_map); // all entries
                the_root.add(current_group); // add top to the list
                
                facility_root.add(current_group); // add one item ONLY to this index listing
	}

	if (g_options.wantNoPostalHistoryIndex())
            {
                current_group = BookUtils.createOverallNOFacilityIndex(
                    g_tit,
                    fac_index_map); // all entries
                the_root.add(current_group); // add top to the list
		/*
		 * CREATE only a listing (not index) 
		 * of all NOF items, but ordered by state and city
		 * NOTE NOTE, the single HTML does not ever do this
		 * the individual HTML creator will do it, if we want
                makeNOFChecklist(
                    g_tit,
                    fac_index_map); // will be in a different order
		 */
//System.out.println("NOF Listing: " + nof_by_city); // debugging
                
            } // end if want facility (NO) index
            // the_root will be the entire thingie
            //        System.out.println(the_root); // this is what we pass to the renderer
            if (make_all_indexes)
            {
		/*
		 * "all" (requested) indexes are written at the end
		 * of the document! (for single HTML, no state pages!)
		 */
                g_tit.renderIndex(pr,the_root,IndexRenderer.COMPLETE_INDEX);
		if (nof_by_city != null)
		{
			/*
			 * something was created
			debugging pr.println("<p>NOF: " + nof_by_city.size() +
				"</p>");
			 */
			g_tit.renderNOFChecklist(pr,nof_by_city);
		}
            } // end if printing all (desired) indexes, usually at end
        } // end if any indexes wanted at all

	/*
	 * UNCLEAR whether we have terminated the index area
	 * correctly. ALSO there may be links to the indexes
	 * that are needed. In particular there is a local link
	 * "#index" that is referred to by lots of places
	 */
        
    } // end create index postfix HTML for single page output
    
	/*
	 * override parent
	 * 
	 * LINKS are different when used in a single page environment 
	 */
	public void addToManifest(String name, String title,
	int manifest_flag)
	{
	/*
         * add boilerplate items to non-state-list
         * that will appear at the top of each page
         * 
         * These are present REGARDLESS of how many states/sections/appendixes 
         * exist in the project.
         */
         if (g_non_state_list == null)
         {
		// first time
System.out.println("Initializing Manifest, saw: " + name + ", " + title);
		g_non_state_list = new ArrayList(); // initialize before anything else happens!
		// add static items

		if (g_options.wantAnyIndex())
		{
			// ONLY if some index will be created
/*
 * NOTE we may not want to put anything here for single page system
 */
			g_non_state_list.add(new TOCEntry(
			"#index",
			"All Document Indexes"));        
		}
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
/*
not appropriate for single page
			g_non_state_list.add(new TOCEntry(
					"#index",
					"This Section Indexes"));    
 */
		}
		/*
		 * ONLY put in a TOC item at top of
		 * page, if facility index is desired
		 */
		if (g_options.wantFacilityIndex())
		{
/*
not appropriate for single page system
			g_non_state_list.add(new TOCEntry(
					"facility_index.html",
					"Facility Index"));    
			g_non_state_list.add(new TOCEntry(
					"javascript:pop_up_controlling_window('facility_index_popup.html');",
					"Facility Index Popup"));    
 */
		} // end if facility index wanted
	} // end first time, add static items
	System.out.println("g_non_state_list1: " + g_non_state_list);
	if (manifest_flag == MANIFEST_IMAGE)
	{
		System.out.println("NO manifest processing for this type");
		return; // we are not putting images in any TOC or manifest file
	}
	if (manifest_flag != MANIFEST_STATE)
	{
		// NO states here
	System.out.println("Adding to Manifest: " + name + ", " + title);
		g_non_state_list.add(new TOCEntry(
		    "#_" + name,
		    //name + ".html",
		    title));        
	}
	// not image, add to the primary list 
	System.out.println("Adding to Manifest: " + name + ", " + title);
	g_toc_list.add(new TOCEntry(
//		    name + ".html",
		    "#" + name,
		    title));        
	System.out.println("g_non_state_list2: " + g_non_state_list);
	} // end addtomanifest

	/*
	 * override code from BookUtils
	 */
    public IndexGroup createGeneralIndexlocal(
        SpecialContentCreator tit,
        Map general_index_map
        ) throws Exception
    {
        IndexGroup current_group = new IndexGroup(
            tit.gT("GENERAL_INDEX_ID"),
            tit.gT("GENERAL_INDEX_TITLE"),
            tit.gT("GENERAL_INDEX_TITLE"),
            false, // no crumbs
            tit.gT("GENERAL_INDEX_ID"),"General");
        // the_root.add(current_group); // caller will add to any cumulative list
        
        // NOW populate the index group
        
        int colon_location = 0;
        
        IndexRefComparable current_index_item  = null;
        GeneralIndexReference index_ref = null;
        Iterator inner = general_index_map.keySet().iterator();
        Iterator inner2 = null;
        String index_item = "";
        while (inner.hasNext())
        {
            index_item = (String)inner.next();
            inner2 = ((List)general_index_map.get(index_item)).iterator();
            while (inner2.hasNext())
            {
                index_ref = (GeneralIndexReference)inner2.next();
                /*
                 * construct the text for the general index item. Strange stuff:
                 * city can be null
                 * "state" can be "appendixz" where "z" is letter a-z
                 */
                StringBuffer bb = new StringBuffer(index_item + " (");
//System.out.println("adding general index item: " + index_ref);
                //if (index_ref.city_name == null)
                if (BookUtils.isEmpty(index_ref.city_name))
                { // dont bother with city, its null or empty
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
                    bb.toString(), // full name with city and state
                    //     index_item,  // text name
                    index_ref.state_abbrev // becomes target file
                    ));
            } // end for each reference
        } // end for each general index item
        return current_group;
    } // end create general index

} // end  html single sink
