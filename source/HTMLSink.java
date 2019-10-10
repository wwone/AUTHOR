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
 * last edited 10/9/2019
 *
 * add Table handling. While it could depend on the format (VAN, SKEL, POEM), we
 * will stick with simple tables with NO internal formatting
 *
 * add vanilla format for HTML
 *
 * remove dependency on old XMLUtils
 *
 * using "remote" flag for images
 *
 * TODO: allow "options.json" to change the size of the outline
 * for images. Right now it is hard-coded here. Option value could
 * be complete CSS, such as  "border:solid .1em color;" 
 * rather than just one border parameter. 
 *
 * TODO: using the AuxiliaryInformation provided,
 * populate the META tags of the resultant
 * HTML file for this section. Those tags already
 * have some boilerplate for the project, but these
 * add more meaning. Code working 6/5.  As of
 * 6/11, we WARN the user and "correct" when the
 * additional metadata is not formatted correctly.
 *
 * This text is encapsulated in an object by the 
 * AUTHOR reading software. 
 *
 * 5/26/2018 REWROTE the process Inline Image. Also
 * making major changes 6/10/2018, see below:
 *
 *
 * allow image address to be "http" something, so we can use
 *   external image sources (DONE) the coding was much
 *   simpler than that above, which seems to have been
 *   broken....
 *
 * working on selective appearance of desired indexes
 *
 * Add ability to set font size inside a bulleted list DONE
 *
 * BIG CHANGE (DONE): use options.json options area to drive this creation:
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
 * preformatted text handler added
 *
 * DONE: remove ALL references to 960 grid,
 * and use SKELETON. this means NO "class=" tags.
 * NOW use either SKEL(eton) of POEM for HTML formatting
 *
 * Attempt to fix issues with non-standard characters,
 * such as tilde n, accented a or o, leading and trailing
 * quotes, etc, etc. These are a pain in the a*****s. When
 * the input XML is read, they become Unicode characters,
 * and Java just passes them through to animals like this
 * Sink. Thus, the letters appear in the output in their
 * native form, not escaped for HTML or other products.
 *
 * Most of the work is done in BookUtils, in the "eT()" method,
 * but that method has to be invoked where we expect
 * high-level Latin characters...
 *
 * ALSO BookUtils the eC() method has to be changed, as
 * the escape system is slightly different (Kindle does not
 * like what HTML browsers are OK with)
 *
 * ADD "manifest" that can be used to create the HTML table
 * of contents. Save some manual work. It will create info
 * that can go into the JSON for the HTML code.
 *
 * The code here is the sink for HTML
 * creation.
 *
 *
 *
 * ISSUES:
 *
 * o) Allow for intermediate breaks, which is kinda
 *    a way for preformatted text
 *
 * *) We don't seem to have the "no postal history" listing
 *    that we made for the PDF. ADD IT!
 *
 * *) The FACILITY links don't seem to work in certain
 *    browsers. Are they wrong?  (which is the same
 *    problem discovered with the PDF version). 
 *    SEEMS to be with the phone browser(s), since Chrome and 
 *    Firefox on computer work fine with the URL's.
 *    I've noticed that some work and some don't with the
 *    same browser, including Firefox on Nexus 7 tablet. Hmm...
 *
 * *) single quotes in captions for images are NOT
 *    getting passed correctly into the Javascript for
 *    the popup images. Try Virginia Quartermaster Br
 *    image...
 *    SEE WA.html for problems with breaks inside
 *      the image captions. We use single quotes ', but
 *      strictly parsing does not allow the <br/> inside
 *      those quotes. See above....
 *
 * TODO:
 *
 * *) No postal history listing (seems to be around, check)
 *
 */
public class HTMLSink extends GenericSink
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
        public List g_non_state_list = null;
        
    public String g_tag_terminator;  // needed to finish "some" HTML tags
 

	// default if nothing else
	public int g_html_format = HTMLContentCreator.FORMAT_POEM;
    
    public HTMLSink() // NO PARAM constructor
	{
	}

    public void init(GenericSource sou,
                    TreeMap state_index_map,
                    TreeMap abbrev_map,
                    TreeMap city_index_map,
                    TreeMap fac_index_map,
                    TreeMap general_index_map,
                    String file_ext) throws Exception
    { 
        init(sou); // parent method
        
        g_file_extension = file_ext; // may be .html, or .xhtml, etc
        g_file_extensionq = g_file_extension + "\"";  // quote at end for use in <a> links
        

        g_state_pr = null;  // no active state file

        
        try
        {
            /*
             * create the writer for the final HTML output
             * during testing, this is stdout
             */
            g_pr = new PrintWriter(new File("index" + g_file_extension)); // write to this file only
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
    public HTMLSink(GenericSource sou)
    {   
        //super(sou);
    }
    
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
		"index.html#index","Document Indexes"));
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
	if (aform.equalsIgnoreCase("van"))
	{
		g_html_format = HTMLContentCreator.FORMAT_VANILLA;
	}
	// none of the above, problems
        /*
         * create the title page
         */
        g_tit.createTitlePage(null); // writes to it's own file
        /*
         * write start of HTML
         */
        createPrefixHTML(g_pr,null);
    } // end startDocument
    
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
			g_non_state_list.add(new TOCEntry(
			"index.html#index",
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
		} // end if facility index wanted
	} // end first time, add static items
	System.out.println("g_non_state_list1: " + g_non_state_list);
	if (manifest_flag == MANIFEST_IMAGE)
	{
		System.out.println("NO manifest processing");
		return; // we are not putting images in any TOC or manifest file
	}
	if (manifest_flag != MANIFEST_STATE)
	{
		// NO states here
		//System.out.println("Adding to Manifest: " + name + ", " + title);
		g_non_state_list.add(new TOCEntry(
		    name + ".html",
		    title));        
	}
	// not image, add to the primary list 
	//System.out.println("Adding to Manifest: " + name + ", " + title);
	g_toc_list.add(new TOCEntry(
		    name + ".html",
		    title));        
	System.out.println("g_non_state_list2: " + g_non_state_list);
	} // end addtomanifest

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
            
            /*
             * create a "manifest" file that can be used to correctly update the JSON
             * file used to create the table of contents for the HTML pages
             */
             
        g_pr.flush();  // flush out the HTML output stream
        
	/*
	 * more for debugging, but we will write out
	 * the TOC manifest information.
	 *
	 * actually, the TOC is written at the top of each
	 * HTML page
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
        pr.close();

    } // end of enddocument method
    
    public PrintWriter getCurrentWriter()
    {
        if (g_state_pr != null)
        {
            return g_state_pr;   // this seems to be the active one
        }
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
        /*
         * introduction content
         *
         * reuse the "state" writer reference
         */
        g_state_pr = new PrintWriter(new File("introduction" + g_file_extension));
        createPrefixHTML(g_state_pr,null);
                
        //        g_state_pr.print("<div> <!-- main section -->\n");
    }  // end start up main section
    
    public void endMainSection() throws Exception
    {
        /*
         * finish main section
         */
        g_state_pr.print(g_tit.gT("TOC_LINK1"));
        /*
         * the main section is simply text, usually the INTRODUCTION
         *
         * we need to terminate this page correctly
         */
        g_state_pr.print(g_tit.gT("END_SECTION"));
        g_state_pr.print(g_tit.gT("END_BODY"));
        g_state_pr.flush();
        g_state_pr.close();
        g_state_pr = null;  // so it can be seen later
        //
        // state writer can now be reused
    } // end terminate main section
    
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
         * reusing the state writer, since no state is printed
         * at the same time as the appendix
         */
        g_state_pr = new PrintWriter(new File(appendix_name + g_file_extension));
        createPrefixHTML(g_state_pr,null);
        fullWidth(g_state_pr);
        
	/* should be done during first pass, not here
                g_toc_list.add(new TOCEntry(
		    app_name + ".html",
                                    app_title));        
	 */

    }
    
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
        g_state_pr.flush();
        g_state_pr.close(); // done with special abbreviation appendix file
        g_state_pr = null; // so can be reused
    } // end abbreviation section
    

    public void createAbbreviationIndex(PrintWriter pr,
                                        TreeMap abbrev_map)
    {
        /*
         * make index listing of abbreviations
         */
        pr.print("<!-- start of abbreviation listing -->\n");
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
        fullWidth(pr);
        pr.println("<p>");
        Iterator initials_it = abbreviation_links.keySet().iterator();
        while (initials_it.hasNext())
        {
            current_initial_letter = (String)initials_it.next(); 
            pr.println("<a href=\"#" + abbreviation_links.get(current_initial_letter) +
                       "\"> .. " + current_initial_letter.toUpperCase() + " .. </a>");
        }
        pr.println("</p>"); // end the special link list
        finishSkeletonx(pr);
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
                        make2Columnsx(pr,all_abbs);
                    }
                    all_abbs = new ArrayList(100); // start again
                    fullWidth(pr);
                    pr.print("<p><a href=\"#abbreviation_index\">[top]</a></p>");
                }
                else
                {
                    // first time
                    fullWidth(pr);
                    all_abbs = new ArrayList(100); // start first time
                }
                // make heading
                pr.print("<h1 id=\"" + abbreviation_links.get(initial_letter) +
                         "\" > -- " + initial_letter.toUpperCase() + " -- </h1>");
                current_initial_letter = initial_letter;
                finishSkeletonx(pr);
            } // end if initial letter changed
            
            /*
             * Note: there is only one abbrevition per treemap item. We grab the first regardless
             * NO NO THIS IS NOT TRUE! Because of the sorting BUG BUG that is
             * non-case-sensitive, there are items that match
             */
            abbrev_def = (String)((List)abbrev_map.get(the_key)).get(0);
            /*
             * HTML is a link that pops up the definition
             */
            // make popup window with caption
            all_abbs.add("<a href=\"javascript:pop_up_window(\'" +
                         BookUtils.eT(BookUtils.escapeSingleQuotes(the_abbrev)) + "','" + 
                         BookUtils.eT(BookUtils.escapeSingleQuotes(abbrev_def)) + 
"\');\" title=\"  Abbreviation: " +
                         BookUtils.eT(the_abbrev) + ",  Definition: " + 
                         BookUtils.eT(abbrev_def) +"\">" +
                         BookUtils.eT(the_abbrev) + "</a>");
        } // end for each abbreviation in the index
        /*
         * all abbreviation items exhausted. We have some items
         * from the last letter change. must dump them out
         */
        if (all_abbs != null)
        {
            make2Columnsx(pr,all_abbs);
        }
        fullWidth(pr);
        
        pr.print("<p><a href=\"#abbreviation_index\">[top]</a></p>");
        finishSkeletonx(pr);
    } //end create abbreviation index listing
    
    public void startAppendixSection(String appendix_name,
                                     String appendix_title,
                                     String short_title) throws Exception
    {
        /*
         * appendix, goes on its own file, but
         * content is processed much like normal
         * content, with all <p> in this section
         */
        g_state_pr = new PrintWriter(new File(appendix_name + g_file_extension));
        createPrefixHTML(g_state_pr,null);
        fullWidth(g_state_pr);
        //   g_state_pr.print("<div> <!-- appendix section: " + appendix_name + " -->\n");
        // appendix_pr.print("<!-- start of appendix (simple content) section -->\n");
    }
    
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
        g_state_pr.flush();
        g_state_pr.close(); // done with appendix file
        g_state_pr = null;  // so can be reused
    } // end appendix section
    
    public void startGeneralSection(String general_name,
                                    String general_title,
                                    String short_title,
				AuxiliaryInformation aux) throws Exception
    {
        /*
         * general section, goes on its own file, but
         * content is processed much like normal
         * content, with all <p> in this section
         */
        /*
         * before processing, check the AuxiliaryMetadata
         * object for correct format: (1) end of description, last
         * non-blank must be "." (that is, a dot)
         * (2) keywords, last non-blank must be "," (that is, a comma)
         * If wrong, we will FIX and WARN
         */
	if (aux instanceof AuxiliaryMetadata)
	{
		AuxiliaryMetadata maux = (AuxiliaryMetadata)aux;
		if ( (maux.description.trim().endsWith("."))  // if after trim is dot
			&& (maux.description.endsWith(" ")) // and at least one space untrimmed
			)
		{
			// good!
		}
		else
		{
			// last nonblank is not dot, and/or last char is not space
			System.err.println("WARNING: additional description metadata must end with '. '! Contents: --" + 
				maux.description + "--");
			maux.description += ". "; // fix it
		}
		if ( (maux.keywords.trim().endsWith(","))  // if after trim is comma
			&& (maux.keywords.endsWith(" ")) // and at least one space untrimmed
			)
		{
			// good!
		}
		else
		{
			// last nonblank is not comma, and/or last char is not space
			System.err.println("WARNING: additional keywords metadata must end with '. '! Contents: --" + 
				maux.keywords + "--");
			maux.keywords += ", "; // fix it
		}
		aux = maux; // store over (may have been altered above)
	}
	g_state_pr = new PrintWriter(new File(general_name + g_file_extension));
        createPrefixHTML(g_state_pr,aux);
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
                g_tit.gT("TAG_END"));
		g_state_pr.print(BookUtils.eT(general_title) + 
                 g_tit.gT("HEADER_END") +
		"2" + // type 2 header seems right
                 g_tit.gT("TAG_END"));
    } // end start general section

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
        g_state_pr.flush();
        g_state_pr.close(); // done with appendix file
        g_state_pr = null;  // so can be reused
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
        /*
         * if already processing a state, finish it up
         */
        if (g_state_pr != null)
        {
            // one exists, close it
            // DEBUG             g_pr.println("<!-- " + current_city_index_map + " -->");
            createStatePostfixHTML(g_state_pr,
                                   g_current_state,
                                   state_index_map,
                                   current_city_index_map,
                                   current_fac_index_map);
            g_state_pr.flush();
            g_state_pr.close();
        }
        g_state_pr = new PrintWriter(new File(the_abbrev + g_file_extension));
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
	
        g_tit.createStaticHeaders(pr,
		g_non_state_list,null); // using global non-state list        
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
        if (g_state_pr != null)
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
            createStatePostfixHTML(g_state_pr,
                                   g_current_state,
                                   state_map,
                                   current_city_index_map,
                                   current_fac_index_map);
            g_state_pr.flush();
            g_state_pr.close();
            g_state_pr = null;
        }
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
    	pr.println("<!-- entering create state postfix html -->\n");
        fullWidth(pr);
        
        pr.print(g_tit.gT("SIMPLE_ANCHOR_START") +
"index" +
                 g_tit.gT("ANCHOR_END"));
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
	if (g_options.wantCityIndex())
            {
                current_group = BookUtils.createStateCityIndex(
                    g_tit,
                    current_city_index_map); // this state only
                if (current_city_index_map.size() > 0)
                {
                    the_root.add(current_group); // add top to the list ONLY if something present
                }
            }
	if (g_options.wantFacilityIndex())
            {
                current_group = BookUtils.createStateFacilityIndex(
                    g_tit,
                    current_fac_index_map); // this state only
                if (current_fac_index_map.size() > 0)
                {
                    the_root.add(current_group); // add top to the list ONLY if something there
                }
            } // end if want facilities index
	if (g_options.wantStateIndex())
            {
                // STATE INDEX
                //
                current_group = BookUtils.createStateIndex(
                    g_tit,state_index_map);
                the_root.add(current_group); // add to list
            } // end make state index
            g_tit.renderIndex(pr,the_root,IndexRenderer.SIMPLE_INDEX);
        } // end, if any indexes wanted at all
        g_tit.endPageSeq(pr);
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
        /*
         * each city used to get  a new div (not any more)
         */
//        g_state_pr.print("<!-- city " + the_city + " -->\n");
//        g_state_pr.print("<div> <!-- city " + the_city + " -->\n");
        //
        // if city has anchor, put it here before heading
        //
        create_anchor(anchor_name,g_state_pr);
        create_index_entries(index_array,g_state_pr);
        
        
        g_state_pr.print(g_tit.gT("CITY_HEADER_START") +
                         BookUtils.eC(the_state) + 
                         g_tit.gT("CITY_ANCHOR_MARKER") +
                         BookUtils.eC(the_city) + 
                         g_tit.gT("HEADER_ID2") +
                         BookUtils.eT(the_city) + 
                         g_tit.gT("CITY_HEADER_END"));
    } // end start a city

    public void endACity()
    {
    	   // well, at least we terminate the div, but why is it there? REMOVED
//        g_state_pr.print("</div> <!-- city -->\n");
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
        //
        // if facility has anchor, put it here before heading
        //
        create_anchor(anchor_name,g_state_pr);
        create_index_entries(index_array,g_state_pr);
        
        
        g_state_pr.print(g_tit.gT("FACILITY_HEADER_START") +
                         BookUtils.eC(the_state) + 
                         g_tit.gT("CITY_ANCHOR_MARKER") +
                         BookUtils.eC(the_city) + 
                         g_tit.gT("FACILITY_ANCHOR_MARKER") +
                         BookUtils.eC(the_facility) + 
                         g_tit.gT("TAG_ID_END") +
                         // underline the facility name
                         g_tit.gT("UNDERLINE_START") +
                         BookUtils.eT(the_facility) + 
                         g_tit.gT("UNDERLINE_END") +
                         g_tit.gT("FACILITY_HEADER_END"));
//                         g_tit.gT("CITY_HEADER_END"));
        
        /*
         * code for creating HTML for the open dates, close dates, etc
         *
         * these lines are bunched together under the
         * "reference" css class. If shown as individual <p>
         * paragraphs, it takes up too much room
         */
        ArrayList all_refs = new ArrayList(5);
        insertRefIfNotNull(source_page,all_refs,
                    "Source Page: ");
        insertRefIfNotNull(open_date,all_refs,
                    "Open: ");
        insertRefIfNotNull(close_date,all_refs,
                    "Closed: ");
        insertRefIfNotNull(personnel_count,all_refs,
                    "Maximum Troops: ");
        insertRefIfNotNull(hospital_admissions,all_refs,
                    "Hosp. Admissions: ");
                    
 
        int ref_cnt = all_refs.size();
        if (ref_cnt != 0)
        {
            //         System.out.println("refs: " + all_refs);
            g_state_pr.print(g_tit.gT("REFERENCE_PARAGRAPH_START"));
            for (int inner  = 0 ; inner < ref_cnt ; inner++)
            {
                g_state_pr.print(all_refs.get(inner));   // line contents
                // if NOT last line, add a break
                if (inner != (ref_cnt - 1))
                {
                    makeNewLine(g_state_pr);  // break after each line
                }
            } // end for each ref item
            g_state_pr.print(g_tit.gT("PARAGRAPH_END")); // terminate the "refs" lines
        } // end if some troop counts, etc lines needed
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
        /*
         * put a footer after each facility with links to the document, state, and city (and index)
         * put newlines between footer links
         */
        g_state_pr.print(g_tit.gT("FACILITY_FOOTER1") +
                         BookUtils.eC(the_state) + 
                         g_tit.gT("INDEX_CRUMB2") +
                         BookUtils.eT(the_state) + 
                         g_tit.gT("INDEX_CRUMB_END"));
        g_state_pr.print(g_tit.gT("FACILITY_FOOTER2") +
                         BookUtils.eC(the_state) + 
                         g_tit.gT("CITY_ANCHOR_MARKER") +
                         BookUtils.eC(the_city) + 
                         g_tit.gT("HEADER_ID2") +
                         BookUtils.eT(the_city) + 
                         g_tit.gT("SIMPLE_ANCHOR_END"));
        g_state_pr.print(g_tit.gT("FACILITY_FOOTER_END")); // index for this state, then end of line
        
    } // end end a facility
   
    public void setSpecialTerminator(String st) 
    {
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
    
	/*
	 * Start a HTML unnumbered list <ul>
	 *
	 * we now allow for a font size change (in percent)
	 */
    public void startList(String size) throws Exception  // start bulleted list
    {
        PrintWriter pr = getCurrentWriter();
        // this is the start of a list
	g_list_font_size = BookUtils.returnContentsOrNull(size); // use in top <ul>
	if (g_list_font_size != null)
	{
	System.err.println("HTML List Start, Size: " + g_list_font_size); // debug
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
    }

    // for list items that contain inner sequences (quote, link, etc)
/*
 * SPECIAL CHARACTERS?
 * NO CHANGE due to font settings (that is in the head <ul> tag)
 */
    public void insertListItemStart(String text,
                                    String anchor_content) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        // start of item in list (usually "li")
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
    } // insert List Item (<li xxx) starting HTML tag
    
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
        //pr.print(g_tit.gT("INDEX_TARGET1") +
        pr.print(g_tit.gT("INDEX_SEEALSO_TARGET1") + // in POEM, at least, we handle displayed value differently
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
            filename = ""; // KLUGE default
            //filename = "index.html"; // KLUGE default
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
//System.out.println("Remote: " + remote + ", " + full_image_location); //debug
        PrintWriter pr = getCurrentWriter();
        /*
         * process image first, then caption text
         * 
         * IN THIS IMPLEMENTATION images are aligned in their
         * own vertical layout, there is NO wrapping of text
         * over, under, around or through the image
         */
        if (full_image_location == null)
        {
		throw new Exception("Image location is null! " +
			caption + ", anchor: " + anchor_content);
	}
        if (caption == null)
        {
		throw new Exception("Image caption is null! " +
			full_image_location + ", anchor: " + anchor_content);
	}
	/*
	 * Make HTML for an image. This is rather messy and
	 * tricky HTML. At one time, I tried to parameterize
	 * the many HTML tags and contents. This has become
	 * impossible to understand and maintain. New needs have
	 * made such work impossible. Sooooo, the HTML being
	 * created here is programmatic, not parameterized.
	 * 
	 * New feature: ALLOW FOR remote image, where the full 
	 * location is a URL, such as "http" address.
	 * rather than a local location. 
	 * If flag is false, use a local location. 
	 */
		/*
		 * If remote image, we do NOT use popup system,
		 * we inline the image as given (it is the AUTHOR
		 * responsibility to get a URL of the right sized
		 * image.)
		 */
                if (remote)
                {
			/*
			 * MUCH work on the following code 6/2018
			 */
			/*
			 * image is a BLOCK by itself, no wrap. We center it,
			 * and add a border. do not allow any ancestor "float"
			 * to control it, we float at "none"
			 * All this hoohah is needed due to heavy CSS use
			 */
// HERE we will insert the image border described in the "options.json" file
			pr.println("<img style=\"display: block; margin-left: auto; margin-right: auto;margin-top:1em;float:none;color:black;border:solid .1em black;\" ");
			// anchor is ALWAYS present for remote
			// add id= to the tag
			pr.println("id=\"" +
				 anchor_content + 
				"\" ");
			/*
			 * continuing on, we add the src=
			 * and close the img tag
			 */
			pr.println(" src=\"" +
                     BookUtils.eT(full_image_location) +  // http://thisnthat
			"\" alt=\"[" +
                         BookUtils.eT(thumb_image_location) +
                         "]\"   />" );
			/*
			 * used to have a clear:left, but since
			 * the above img tag is a block, we do not
			 * need to have a clear
			 * margins pinched a bit to center the caption
			 * more nicely
			 */
			pr.println("<p style=\"text-align:center;margin-left:15%;margin-right:15%;font-style: italic;\">" +
			BookUtils.eT(addLineBreaks(caption)) +
			"</p>"); // end caption text block
		} // end if external image
		else
		{
		/*
		 * else local reference, we process an image in previous "normal" way
		 * as a thumb displayed and popup window controlled
		 * by Javascript
		 * NOTE, the popup is triggered by clicking on the CAPTION, not the image
		 */
		pr.println("<img ");
                if (anchor_content != null)
                {
                    pr.println( " id=\"" +
                             anchor_content + 
				"\" ");
                } 
		// see above as to why this hoohah is here
// HERE we will insert the image border described in the "options.json" file
		pr.println(" style=\"display: block; margin-left: auto; margin-right: auto;float:none;margin-top:1em;color:black;border:solid .1em black; \" \n" +
			"alt=\"[" + 
                         BookUtils.eT(full_image_location) + 
			"]\"\n src=\"thumbs/" + 
                         BookUtils.eT(thumb_image_location) + 
				"\"/>");
			// CAPTION click causes the popup
			pr.println("<p style=\"clear:left;text-align:center;margin-left:15%;margin-right:15%;font-style:italic;\">\n" +
			" <a href=\"javascript:pop_up_image('" +
                             BookUtils.eT(full_image_location) + 
				"'," +  // end first param, start second
			// build multiple captions as Array(); NO single quotes!! 
                         BookUtils.eT(breakCaption(caption)) +   
				"'" + // starting single quote 
			// set up for use in HTML title for page
                         BookUtils.eT(filterForHTMLTitle(caption)) +   
			"');\">");
                pr.println(
			BookUtils.eT(addLineBreaks(caption)) + "</a></p>");
		} // end local image ref, not http
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
    
    public void createPrefixHTML(PrintWriter pr,
		AuxiliaryInformation aux) throws Exception
    {
	/*
	 * the static header creator gets a number of items:
	 * 1) printwriter to put it on
	 * 2) List containing TOC items
	 * 3) auxiliary information to add to metadata
	 *    which can be null
	 */
        g_tit.createStaticHeaders(pr,g_non_state_list,aux);
    }
   

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
	/*
	 * we're going to try to live without the "table of contents"
	 * internal link. Should be obvious to all, and I think
	 * there are other ways to get to the top of the page
	 */
        //pr.print(g_tit.gT("POSTFIX_CONTENT")); // more links, pointer to toc at top of page
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
        pr.print(" <!-- start postfix from Java(2) -->\n");  // no div
        
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
                
                /*
                 * for HTML we repeat the entire facility index in its
                 * own web page. This popup will sit alongside the main
                 * view page and when clicked it will cause the location
                 * to change on the main view page
                 */
                facility_root.add(current_group); // add one item ONLY to this index listing

                /*
                 * make facility index page as a separate page
                 */
                PrintWriter fac_pr = new PrintWriter(new File("facility_index" + g_file_extension));
                createPrefixHTML(fac_pr,null);
                fullWidth(fac_pr);
                
                 fac_pr.print(g_tit.gT("FACILITY_INDEX_START"));
                
                
 // was, sorry               fac_pr.print("<h1 id=\"facility_index\" class=\"heading1\">All Facilities</h1>\n");
                g_tit.renderIndex(fac_pr,facility_root,IndexRenderer.SIMPLE_INDEX);
                fac_pr.print(g_tit.gT("SKELETON_PAGE_CLOSE"));
                
                fac_pr.flush();
                fac_pr.close();   // done with that file
                /*
                 * the facilities index popup differs from the standalone
                 * facilities index, in that the links point back to the parent
                 * window for the URL change
                 */
                
                fac_pr = new PrintWriter(new File("facility_index_popup" + g_file_extension));
                createPrefixHTML(fac_pr,null);
                fullWidth(fac_pr);
                 fac_pr.print(g_tit.gT("FACILITY_INDEX_START"));
                g_tit.renderIndex(fac_pr,facility_root,IndexRenderer.POPUP_INDEX);
                fac_pr.print(g_tit.gT("SKELETON_PAGE_CLOSE"));
                
                fac_pr.flush();
                fac_pr.close();   // done with that file
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
		 * populate the global within this object
		 */
                nof_by_city = BookUtils.makeNOFChecklist(
                    g_tit,
                    fac_index_map); // will be in a different order
//System.out.println("NOF Listing: " + nof_by_city); // debugging
                
            } // end if want facility (NO) index
            // the_root will be the entire thingie
            //        System.out.println(the_root); // this is what we pass to the renderer
            if (make_all_indexes)
            {
		/*
		 * "all" (requested) indexes are usually written at the end
		 * of the document
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
        
        fullWidth(pr);    
        //pr.print(g_tit.gT("HIDDEN_MESSAGE")); // currently EMPTY!!
// don't think we need these any more
        //makeNewLine(pr);
        makeNewLine(pr);
	/*
	 * HOW ABOUT putting here some desired links
	 * as specified in the PROJECT file?
	 */
	ReplacementString rep = g_tit.getProjectKeyValue("PROJECT_HTML_POSTFIX");
	if (rep != null) // if not specified, result will be null
	{
		pr.println(rep.rep); // string only, nothing fancy
	}
pr.println("<p><a href=\"#top\">Top</a></p>"); // get to top of page
        //makeNewLine(pr);
        //makeNewLine(pr);
        /*
         * BUG, use the special CSS display of the validator, not the image
         *   (else image is local and not on the web)
	 * [probably no longer applies]
         */
// don't think we need this        pr.print(g_tit.gT("PARAGRAPH_END"));
        finishSkeletonx(pr);
        pr.print(g_tit.gT("SKELETON_PAGE_CLOSE"));
    } // end create postfix HTML

    public void startTable(String [] header_cells) throws Exception
	{
		PrintWriter pr = getCurrentWriter();
		pr.println("<table>");
		if (header_cells != null) // optional
		{
			pr.println("<th>");
			for (int i = 0 ; i < header_cells.length ; i++)
			{
				pr.println("<td>" + header_cells[i] + "</td>");
			}
			pr.println("</th>");
		}
	} // end start table
            
    public void endTable() throws Exception
	{
		PrintWriter pr = getCurrentWriter();
		pr.println("</table>");
	}
            
    public void insertTableRow(String [] cells) throws Exception // multiple items in a row
	{
		PrintWriter pr = getCurrentWriter();
		pr.println("<tr>");
		for (int i = 0 ; i < cells.length ; i++)
		{
			pr.println("<td>" + cells[i] + "</td>");
		}
		pr.println("</tr>");
	} // end insert table row

} // end  html sink
