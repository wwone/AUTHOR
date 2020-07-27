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
 * updated 10/24/2019
 * 
 * first cut at Tables. We will use FOP table notation
 *    these are VERY SIMPLE tables, no internal formatting
 * 
 * Fix table output so it is escaped (!) for XML 
 * 
 * remove dependency on old XMLUtils 
 * 
 * Changed checklist to have single-space and underlines to make check place 
 * 
 * changed creation of embedded-image, simplified for various reasons
 * 
 * added code to accept auxiliary metadata, but NOTHING is done with it 
 * 
 * use Options object for various settings, such as indexes 
 * this is ESPECIALLY true for non-book products, such as Presentations
 * 
 * Where bookmarks are created for PDF, make the first letters 
 * significant, because most of the bookmark is truncated by the
 * PDF viewer. 
 * 
 * ADD processing of format-specific options from options.json file. In
 * particular, we must create either 1-column PDF (ideal)
 * or 2-column PDF (old fashioned, only OK for print).
 *
 * added preformatted text start
 *
 * Now using the commonized index creators in
 * BookUtils
 *
 * MAJOR CHANGE: Use the FO strings that are stored
 * in the create special content object, that were read
 * from the JSON configuration file. As part of refactoring,
 * use the special content creator object for all
 * code that is specific to a book and to
 * this format.
 *
 * The code here is the sink for creating
 * a FOP file. This file can be passed
 * to the Apache FOP (XML:FO) package, which will
 * create a PDF file that is read by using
 * a PDF reader, such as Adobe Reader. This is
 * NOT used to make a printed book, which has
 * rather different requirements. Other formats are possible,
 * but this work is being done specifically as an
 * alternative for creating PDF.
 *
 * We are creating the .fo file directly, not
 * using XSLT. While the content for this book
 * system is in created in XML, the XML is too
 * complex to be easily handled by XSLT.
 *
 * *) Version 1 -- first cut, working
 *
 * *) Version 2 -- using inheritance for all
 *     properties that I can, such as "font-family".
 *    These will be specified in the fo:root node
 *
 * *) Version 3 -- Correcting and enhancing the Index area,
 *    which will involve the Table of Contents, too
 *
 * ISSUES:
 *
 * o) insert inline break, HOW TO DO?
 *    currently dummied out
 *
 * *) Following is obsolete. We will just use Helvetica and
 *    not worry about embedding.
 *    start obsolete
 *    (others?). Embed the fonts!  The primary root
 *    element gives the font of Arial, the
 *    printer ornaments are now PrinterOrnamentsOne
 *    end obsolete
 *
 * *) Turns out that I was using an invisible "block" for
 *    jump destinations. The "fo:wrapper" is a much
 *    better thing to use, per the FOP developers. (DONE)
 *
 * *) Certain images DO NOT WORK with the FOP software.
 *    Largely this seems to be images that are badly
 *    distorted, when viewed on the Mac. When un-distorted
 *    and handled by the usual means, they look fine in
 *    everything EXCEPT FOP. They remain distorted.
 *    A similar problem appears when images are extracted
 *    from PDF documents downloaded from Google Books!
 *    In that case, the images are INVISIBLE!
 *
 * *) The "the_state" entry is written
 *    at the top of every section in the book, but
 *    it currently not used in the static header (see
 *    the JSON file for how it could be used). This
 *    is because we want a partial title at the top
 *    of each page, but we don't have that. Right
 *    now a title for an appendix is "Appendix X -- More Information"
 *    does not scan correctly. We could redsign the
 *    interface so that there must be an "appendix title"
 *    and "appendix subtitle". Otherwise, the partial
 *    headers don't look very good. They had to be junked when
 *    we created a non-state-driven book, such as the Great
 *    Lakes book. Until the interface is redesigned, the
 *    page toppers will be simple, and not context-driven.
 *
 * *) Per item above, the name should be changed to "sectionsubtitle" or
 *    some such, since this pattern can be used for any non-state-driven
 *    book. Thus, we will have a topper for each page that is:
 *    "THE BIG BOOK OF FRED (sectionsubtitle)", which are followed
 *    by the links to TOC and Index. For a state-driven doc, the sectionsubtitle
 *    would be "California" for a state, and then "More Information" for
 *    some appendix.
 *
 * *) MUST SUPPORT LINKS within various tags. We have
 *    now allowed "anchor" items within such tags
 *    as content variants. These need to be processed
 *    the same as type="anchor" name="thing". (DONE)
 *
 * *) Bookmarks -- the design of FOP requires that
 *    bookmarks be written BEFORE the text flow.
 *    FIXED -- all sections of the FOP are written
 *     to separate files, and a simple "cat"
 *     constructs the final file. Files:
 *
 *    1) front material (booka_front)
 *    2) bookmarks (bookb_bookmarks)
 *    3) flow start and title page   (bookc_title)
 *    4) move right to:
 *    5) table of contents (booke_toc)
 *    6) main text (bookf_main)
 *    7) appendexes (bookg_appendixes)
 *    8) indexes (bookh_indexes)
 *    9) flow end (booki_endflow)
 *    10) all end (bookj_endall)
 *
 *  *) Front material must include the following sequence:
 *     1) Title page (no graphics, just title and author
 *     2) Front material, author title, ISBN, etc, etc
 *     3) Preface for (PDF) (Paper) users. This describes
 *        the document and helps users navigate the book. 
 *        PDF (by way of FOP) will be the typical format
 *        when sold on LuLu, since they offer printed and
 *        electronic. Printed must have page numbers in
 *        the TOC and indexes. Raw PDF will have jump links. (DONE)
 *
 * NEED:
 *
 *  *) Printed Index
 *       There are MULTIPLE indexes for this document.
 *       DONE
 *
 *
 *  *) Printed Table of Contents
 *       One should suffice
 *       Mostly done, but want references to all the indexes at the end
 *       NEED to have a "Table of Contents" that points to each
 *       index type. 
 *
 *  *) Index heading should have links to all the individual indexes,
 *     along with all the TOC references here noted. Perhaps the
 *     Table of Contents should point to the various indexes. Right
 *     now (11/1/2012) only one entry to the head of the
 *     indexes. There are a LOT of indexes, which is a major part
 *     of the book design. (WORKING, sort of, seems a little busy when
 *       we have only one index, as with the Great Lakes book.)
 *
 * *) Interactive page trailers, such as is done for native
 *    PDF: link to indexes, link to ToC DONE -- mostly works
 *
 *  *) Separate flows for the Appendixes and Index, so
 *     that they are page numbered separately. This
 *     should be a common action. Same with title and TOC.
 *     For now, these are 2-page layouts, so the printed
 *     version paginates like a real book. This leaves
 *     empty pages, which perhaps SHOULD have some marker
 *     for "intentionally blank".
 *
 */
public class FOPPDFSink extends GenericSink
{
    /*
     * globals 
     */

    public String[] BOILERPLATE = BookUtils.FOP_BOILERPLATE;  // use FOP-specific boilerplate 

    /*
     * following does NOT contain closure. When printed, user must add greater than and any other content
     * this allows the object declaration to be extended
     * Notice default font and font size. saves space in other declarations
     * includes:
     * font-size=\"10pt\"\n
     * font-family=\"Helvetica\"\n
     * line-height=\"normal\"\n
     * NONO, not inherited! space-before.optimum=\"10pt\"\n
     * text-align=\"start\"\n
     * NONO, not inherited! span=\"none\" ";
     */
    
    public String g_file_extension; // will be .fo
    public String g_file_extensionq = ""; // NOT USED

    public ArrayList g_appendixes = null; // links and names
    
    public String g_current_state; // used for postfix processing of a state
    
    public String g_tag_terminator;  // needed to finish "some" XML:FO tags
    
    public FOPPDFSink() // NO PARAM
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
        
        //System.out.println("at start, state index: " + state_index_map);
        g_file_extension = file_ext; // may be .html, or .xhtml, etc
        
        g_appendixes = new ArrayList();
        
        g_state_pr = null;  // no active state file
        
        try
        {
            g_pr = null; // new PrintWriter(new File("book" + g_file_extension)); // write to this file only
            g_state_pr  = g_pr;    // SAME writer for everyone
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
    public FOPPDFSink(GenericSource sou)
    {   
    //    super(sou);
    }
    
    public void startDocument(SpecialContentCreator tit) throws Exception
    {
	g_tit = tit; // make global, used a lot
        /*
         * write start of FOP
         */
        PrintWriter mypr =  new PrintWriter(new File("booka_front" + g_file_extension)); // write to this file only
        createPrefixFOP(mypr);
        mypr.close();
        mypr = null;
/*
 * OK, work this out. We now have OPTIONAL title, preface,TOC, etc
 *
 * historically, we printed a two-column BOOK, BUT, we now
 * would rather have 1-column for interactive PDF, etc, etc.
 *
 * in the past, we HAD to create the title page as 1-column,
 * for readability, leaving the body of the book 2-column. This
 * required different layouts and flows
 *
 * In a normal 2-column layout, if we want some of the
 * content to be optional, no problem, we just don't
 * print the <blocks> containing that content. 
 *
 * NOW, things are also more complicated because we had
 * STATIC content with each flow, in particular, page number
 * at BOTTOM, and at TOP, we had title and links to TOC and Index head
 *
 * With TOC and indexes OPTIONAL, this static content must be
 * changed by flag values.
 *
 * ALSO, that title at the top of flowed content was WRONG,
 * because, we are trying to insert a state name in there. Well,
 * that DOESN'T WORK with non-facilities projects. In particular,
 * BlueJacket book, presentations, articles, etc, etc
 *
 * MORE COMPLICATIONS: the "intentional blank" page was done
 * for printed books. The title page, etc, must total an even
 * number of pages, so if we are short, we make a single
 * blank one, so that the start of the book is in recto??? (odd page number from start)
 * perhaps this restriction should be maintained for the PRINT
 * PDF creator. 
 *
 *
 *
 */
        
        mypr =  new PrintWriter(new File("bookc_start_and_title" + g_file_extension)); // write to this file only
        
        createFlowStartAndTitleFOP(mypr); // dynamically loaded object creates the title content
        mypr.close();
        mypr = null;
        
        /*
         * MAIN text will be written using a global
         */
        g_pr =  new PrintWriter(new File("bookf_main" + g_file_extension)); // write to this file only
        g_state_pr = g_pr; // used by some code
    }
    
    public void createFlowStartAndTitleFOP(PrintWriter pr) throws Exception
    {
        /*
         * flow master for cover, title page,front matter, and preface are different for PDF
         * 
         * Page sequence needs to enclose title, front, preface pages 
         * IF optionally these are not done, then no enclosing page sequence!
         * 
         */
        /*
         * invoke the object that makes the desired title matter, including:
         * 1) cover
         * 2) title page
         * 3) front matter
         * 4) preface for PDF/paper users
         */
		g_tit.createTitlePage(pr);
/*
        else
        {
            // hmm, no object for making title page, put in dummy info
            pr.print("<fo:block  font-size=\"24pt\"\nfont-weight=\"bold\"\nspace-after.optimum=\"20pt\"\nspace-before.optimum=\"40pt\"\ntext-align=\"center\">\n");
//            pr.print("<fo:block  font-size=\"24pt\"\nfont-family=\"Helvetica\"\nfont-weight=\"bold\"\nline-height=\"normal\"\nspace-after.optimum=\"20pt\"\nspace-before.optimum=\"40pt\"\ntext-align=\"center\"\nspan=\"none\">\n");
            pr.print("NO TITLE INFORMATION!</fo:block>\n");
        }
*/
    } // end create flow start and title page, moving on to TOC
    

    public void printTitleSeries(PrintWriter pr,
                                 String[] series) throws Exception
    {
        for (int inner = 0 ; inner < series.length ; inner++)
        {
            pr.print(g_tit.gT("NORMAL_BLOCK_FOP") + series[inner] +
                     g_tit.gT("NORMAL_BLOCK_FOP_END"));
        }
    }
    
    public void endDocument(
        TreeMap  state_index_map,
        TreeMap  abbrev_map,
        TreeMap  city_index_map,
        TreeMap  fac_index_map,
        TreeMap  general_index_map
        ) throws Exception
    {
        /*
         * CLOSE out the appendixes, which were the last group
         */
        g_pr.close();
        g_pr  = null;
        
        /*
         * start writing on different files, NOT THE global one (it should be closed)
         */
        PrintWriter mypr =  new PrintWriter(new File("booki_endflow" + g_file_extension)); // write to this file only
        createFlowEndFOP(mypr);
        mypr.close();
        mypr = null;
        //        mypr =  new PrintWriter(new File("indexes" + g_file_extension)); // write to this file only
        /*
         * print indexes (which include links for interactive PDF
         * and page numbers
         */
        //        mypr =  new PrintWriter(new File("indexes" + g_file_extension)); // write to this file only
        mypr =  new PrintWriter(new File("bookh_indexes" + g_file_extension)); // write to this file only
        makeIndexes(
            mypr,
            state_index_map,
            abbrev_map,
            city_index_map,
            fac_index_map,
            general_index_map
            );
        mypr.close();
        mypr = null;
        /*
         * bookmarks that we construct are then converted
         * to the outlines and bookmarks used in PDF
         * THESE ARE WRITTEN TO SEPARATE FILE, because they
         * have to be put BEFORE the main text content
         */
        mypr =  new PrintWriter(new File("bookb_bookmarks" + g_file_extension)); // write to this file only
        makeOutlines(
            mypr,
            state_index_map,
            abbrev_map,
            city_index_map,
            fac_index_map,
            general_index_map);
        mypr.close(); // done with the bookmarks, they have to be re-assembled into the front of the final file
        mypr = null;

        mypr =  new PrintWriter(new File("booke_toc" + g_file_extension)); // write to this file only
        makeTableOfContents(
            mypr,
            state_index_map,
            abbrev_map,
            city_index_map,
            fac_index_map,
            general_index_map
            );
        mypr.close(); // done with the toc, it will have to be re-assembled into the front of the final text
        mypr = null;

        mypr =  new PrintWriter(new File("bookj_endall" + g_file_extension)); // write to this file only
        createAllEndFOP(
            mypr);
        mypr.close(); // done with the toc, it will have to be re-assembled into the front of the final text
        mypr = null;
    }

    public void createFlowEndFOP(PrintWriter pr) throws Exception
    {
        pr.print(g_tit.gT("FLOW_END_FOP"));
        pr.print(g_tit.gT("PAGE_SEQ_END_FOP"));
    }
    
    public void createAllEndFOP(PrintWriter pr) throws Exception
    {
        pr.print(g_tit.gT("ALL_FOP_END"));
    }
    
    /*
     * The "state" writer is the same as the general stream writer
     */
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
        // PrintWriter (global) has already been set up
    } // start states grouping
    
    public void endStates() throws Exception
    {
        g_pr.print(g_tit.gT("FLOW_END_FOP"));

        g_pr.print(g_tit.gT("PAGE_SEQ_END_FOP"));
        
        /*
         * CLOSE out the file for the states, start for appendixes
         */
        g_pr.close();
        g_pr  = null;
        /*
         * HERE HERE we would like to separate states fro each
         * appendix with a separate flow. This will have to
         * be done by using a global STATE value!
         */
        try {
        g_pr =  new PrintWriter(new File("bookg_appendixes" + g_file_extension)); // write to this file only
        g_state_pr = g_pr;
        }
        catch (Exception ee)
        {
            System.err.println("FILE FAILURE: " + ee);
            ee.printStackTrace();
            System.exit(-1);
        }
    } // end states grouping
    
    public void startMainSection(
				String short_title) throws Exception
    {
   //     g_state_pr.print(" <!-- main section -->\n"); // debugging
    // Introduction follows, we must make a link destination for it
        g_pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
"_intro" +
                         g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
    }  // end start up main section
    
    public void endMainSection()
    {

        
    } // end terminate main section
    
    public void startAbbreviationSection(String app_name,
                                         String app_title,
					String short_title) throws Exception
    {
        String appendix_name = app_name;
        /*
         * force page break
         * it would be nice to have a centered heading, but
         * headings are treated separately for appendix sections....
         */
        g_pr.print(g_tit.gT("STATE_TITLE_BLOCK_FOP1") +
                   appendix_name +   // ID for linking
                   g_tit.gT("STATE_TITLE_BLOCK_FOP2") + // no printable content, but place marker
                   g_tit.gT("MARKER_START_STATE_FOP") +
                   short_title +
                   g_tit.gT("MARKER_END_FOP") + // content could follow this, but it isnt (why?)
                   g_tit.gT("STATE_TITLE_BLOCK_FOP_END"));
//        app_title.substring(0,10) +  (previous short title)
     
        g_appendixes.add(appendix_name);
        g_appendixes.add(app_title); // bad boy!
        //   g_state_pr.print(" <!-- abbreviation section -->\n"); // debug
    }
    
    public void endAbbreviationSection(TreeMap abbrev_map,
                                       TreeMap state_index_map,
                                       TreeMap city_index_map,
                                       TreeMap fac_index_map,
                                       TreeMap general_index_map
				) throws Exception
    {
        createAbbreviationIndex(g_state_pr,abbrev_map);
    } // end abbreviation section

    
    public void startAppendixSection(String appendix_name,
                                     String appendix_title,
					String short_title) throws Exception
    {
        /*
         * force page break
         * it would be nice to have a centered heading, but
         * headings are treated separately for appendix sections....
         */
        g_pr.print(g_tit.gT("STATE_TITLE_BLOCK_FOP1") +
                   appendix_name +   // ID for linking
                   g_tit.gT("STATE_TITLE_BLOCK_FOP2") + // no printable content, but place marker
                   g_tit.gT("MARKER_START_STATE_FOP") +
                   short_title +
                   g_tit.gT("MARKER_END_FOP")  +   // content could follow this, but it isnt (why?)
                   g_tit.gT("STATE_TITLE_BLOCK_FOP_END"));
//        appendix_title.substring(0,10) +
        
        g_appendixes.add(appendix_name);
        g_appendixes.add(appendix_title); // bad boy!
        
//        g_state_pr.print(" <!-- appendix section: " + appendix_name + " -->\n");  // debugging
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
        g_state_pr.print(" <!-- appendix section -->\n"); // debug
    } // end appendix section
    
    public void startGeneralSection(String general_name,
                                     String general_title,
				String short_title,
				AuxiliaryInformation aux) throws Exception
    {
        /*
         * force page break
         * it would be nice to have a centered heading, but
         * headings are treated separately for general sections....
         */
        
        g_pr.print(g_tit.gT("STATE_TITLE_BLOCK_FOP1") +
                   general_name +   // ID for linking
                   g_tit.gT("STATE_TITLE_BLOCK_FOP2") + // no printable content, but place marker
                   g_tit.gT("MARKER_START_STATE_FOP") +
                   short_title +
                   g_tit.gT("MARKER_END_FOP") +   // content could follow this, but it isnt (why?)
		general_title + // WHY NOT? putting title here
                   g_tit.gT("STATE_TITLE_BLOCK_FOP_END"));
//        general_title.substring(0,10) +
        
        g_appendixes.add(general_name);
        g_appendixes.add(general_title); // bad boy!
        
        //        g_state_pr.print(" <!-- appendix section: " + appendix_name + " -->\n");  // debugging
    } // end start general section
    
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
        g_state_pr.print(" <!-- general section -->\n"); // debug
    } // end general section
 
    /*
     * will go to new page and make headings for a new state
     *
     * note: short_title may be null!
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
         * if more IDs needed, they will be put into an invisible block
         * BEFORE the main heading
         */
        if (anchor_name != null)
        {
            g_state_pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                             BookUtils.eC(anchor_name) +
                             g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
        }
        if (index_array != null)
        {
            Iterator it = index_array.iterator();
            IndexRef ir = null;
            while (it.hasNext())
            {
                ir = (IndexRef)it.next();
                g_state_pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                                 "general_" + BookUtils.eC(ir.name) + "_" +
                                 ir.getRefNumber() +
                                 g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
//                String.valueOf(ir.position) +  
            } // end for each index entry
        } // if embedded index(es) in the state tag
        //
        // FOP forces a page break before
        //
        // state ID must be included in the fo:block
        //
        // title block only
        //
        g_state_pr.print(g_tit.gT("STATE_TITLE_BLOCK_FOP1") +
                         "state_" + BookUtils.eC(the_state) +   
                         g_tit.gT("STATE_TITLE_BLOCK_FOP2") +
                         g_tit.gT("MARKER_START_STATE_FOP"));
        if (short_title == null)
        {
            g_state_pr.print(the_state);
        }
        else
        {
            g_state_pr.print(short_title);
        }
        g_state_pr.print(
            g_tit.gT("MARKER_END_FOP") +  
                         BookUtils.eT(the_state) + 
                         g_tit.gT("STATE_TITLE_BLOCK_FOP_END"));
    } // end start a state
     
    public void makeTableOfContents(PrintWriter pr,
                                    TreeMap state_index_map,
                                    TreeMap abbrev_map,
                                    TreeMap city_index_map,
                                    TreeMap fac_index_map,
                                    TreeMap general_index_map
                                    ) throws Exception
    {
        
        /*
         * we use hard-coded flags for the
         * desired index type
         * instead should use the configurable names.
         * for now, map them, and create a flag set
         "INDEX_STATES" , "1",
         "INDEX_CITIES", "2",
         "INDEX_FACILITIES","4",
         "INDEX_GENERAL","8",
         "INDEX_NO_POSTAL_HISTORY","16",
         "INDEX_ABBREVIATIONS","32",
         */
        /*
         * map of all indexes wanted. this is built from the
         * flags that came in from the options.json file
         * by way of the Options object passed by the main source
         */
        TreeMap index_flags = new TreeMap(); 
        /*
         * build a general structure of maps for all of
         * the index/toc content
	 *
	 * special case is general only, commonly appears with
	 * simple books, articles, and newsletters
         */
	if (g_options.wantGeneralIndexONLY())
        {
		// no other indexes besides general
            index_flags.put("INDEX_GENERAL","8");
        }
	else
	{
		if (g_options.wantStateIndex())
		{
		    index_flags.put(
			"INDEX_STATES" , "1"); // content does not mean much at this time
		}
		if (g_options.wantCityIndex())
		{
		    index_flags.put(
			"INDEX_CITIES", "2");
		}
		if (g_options.wantFacilityIndex())
		{
		    index_flags.put("INDEX_FACILITIES","4");
		}
		if (g_options.wantGeneralIndex())
		{
		    index_flags.put("INDEX_GENERAL","8");
		}
		if (g_options.wantNoPostalHistoryIndex())
		{
		    index_flags.put(
			"INDEX_NO_POSTAL_HISTORY","16");
		}
	} // end if more than one type of index wanted
        TreeMap all = new TreeMap();
        all.put("STATE",state_index_map);
        all.put("ABBREVIATIONS",abbrev_map);
        all.put("CITY",city_index_map);
        all.put("FACILITY",fac_index_map);
        all.put("GENERAL",general_index_map);
        g_tit.createTOC(pr,all, g_appendixes, index_flags); // special content creator will do the TOC
        
	/*
	 * NOTE HERE HERE, we may have not created ANYTHING
	 * in the above TOC creator code
	 */
        
        /*
         * start Introduction section
         */
        g_tit.startFlow(pr,"Page"); // start a new flow with different page number indicator
        
         /*
          * FUTURE EXPANSION, we will make the automatic usage of
          * the first text as "Introduction", into an optional feature
          * 
          * We want to have "Introduction" in the header 
          * 
          * The formatting has become so complex, we are 
          * putting the FOP back into this Java code, rather
          * than use the list of strings in the JSON.
          * 
          */
        pr.print("<fo:block page-break-before=\"always\" font-size=\"16pt\"\nfont-weight=\"bold\"\nspace-after.optimum=\"10pt\"\nspace-before.optimum=\"0\"\ntext-align=\"center\" id=\"" +
	"_iiii" +
	"\">\n" +
	"<fo:marker marker-class-name=\"the_state\">" +
	"Introduction" +
	"</fo:marker>" +
	"Introduction" +
	"</fo:block>\n");
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
        // anything to be done?
    }

    
    
    /*
     * make city headings and link references for access
     */
    public void startACity(String the_abbrev, 
                            String the_state,
                           String the_city,
                            String anchor_name,
                            List index_array) throws Exception
    {
        /*
         * if more IDs needed, they will be put into an invisible block
         * BEFORE main heading
         */
        if (anchor_name != null)
        {
            g_state_pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                             BookUtils.eC(anchor_name) +
                             g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
        }
        if (index_array != null)
        {
            Iterator it = index_array.iterator();
            IndexRef ir = null;
            while (it.hasNext())
            {
                ir = (IndexRef)it.next();
                g_state_pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                                 "general_" + BookUtils.eC(ir.name) + "_" +
                                 ir.getRefNumber() +
                                 g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
//                String.valueOf(ir.position) +  
            } // end for each index entry
        } // if embedded index(es) in the state tag
        g_state_pr.print(g_tit.gT("CITY_TITLE_BLOCK_FOP1") +
                         "state_" + BookUtils.eC(the_state) + "_city_" + 
                         BookUtils.eC(the_city) + 
                         g_tit.gT("CITY_TITLE_BLOCK_FOP2") +
                         BookUtils.eT(the_city) + 
                         g_tit.gT("CITY_TITLE_BLOCK_FOP_END"));
    }

    public void endACity()
    {
        g_state_pr.print("<!-- city -->\n"); // debugging
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
        PrintWriter pr = getCurrentWriter();
        
        /*
         * if more IDs needed, they will be put into an invisible block
         * BEFORE the heading
         */
        if (anchor_name != null)
        {
            pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                             BookUtils.eC(anchor_name) +
                             g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
        }
        if (index_array != null)
        {
            Iterator it = index_array.iterator();
            IndexRef ir = null;
            while (it.hasNext())
            {
                ir = (IndexRef)it.next();
                pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                                 "general_" + BookUtils.eC(ir.name) + "_" +
                         ir.getRefNumber() +
                                 g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
//                String.valueOf(ir.position) +  
            } // end for each index entry
        } // if embedded index(es) in the state tag
        pr.print(g_tit.gT("FACILITY_HEADING_BLOCK_FOP1") +
                 "state_" + BookUtils.eC(the_state) + "_city_" + BookUtils.eC(the_city) + 
"_fac_" + BookUtils.eC(the_facility) +
                 g_tit.gT("FACILITY_HEADING_BLOCK_FOP2") +
                 BookUtils.eT(the_facility));
        pr.print(g_tit.gT("HEADING1_BLOCK_FOP_END"));   // end special text block 
        /*
         * if any of the "reference" items are present, print them too
         */
        boolean first_time = true;

        first_time = printReference(pr,
                                    source_page,
                                    "Source Page: ",
                                    first_time);
        first_time = printReference(pr,
                                    open_date,
                                    "Open: ",
                                    first_time);
        first_time = printReference(pr,
                                    close_date,
                                    "Closed: ",
                                    first_time);
        first_time = printReference(pr,
                                    personnel_count,
                                    "Maximum Personnel: ",
                                    first_time);
        first_time = printReference(pr,
                                    hospital_admissions,
                                    "Hosp Admiss: ",
                                    first_time);
        
    } // end start a facility

    public boolean printReference(PrintWriter pr,
                                  String ref,
                                  String narrative,
                                  boolean first_time) throws Exception
    {
        
        if (ref != null)
        {
            if (first_time)
            {
                pr.print(g_tit.gT("REFERENCE_BLOCK_FOP") +
                         narrative + ref +
                         g_tit.gT("REFERENCE_BLOCK_FOP_END"));
            }
            else
            {
                // not first time, give less spacing between
                pr.print(g_tit.gT("REFERENCE_BLOCK_FOP_REST") +
                         narrative + ref +
                         g_tit.gT("REFERENCE_BLOCK_FOP_END"));
            }
            return false; // whatever happened, no longer first time
        }
        return first_time; // nothing printed, pass back what we were passed
    }
    /*
     * facility heading, necessary links so city and references will work
     */

    public void endAFacility(String the_state,
                             String the_city)
    {
        // what to do?
    }
    
    public void setSpecialTerminator(String st) // a kind of a state marker, often used in HTML output
    {
        g_tag_terminator = st; // specify it from outside
    }
    
    /*
     * boilerplate will be different with FOP
     */
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
        pr.print(g_tit.gT("NORMAL_BLOCK_FOP"));   // start new paragraph
        
        if (allow_span)
        {
//            g_tag_terminator = "\n"; // no paragraph termination, someone else must do this
            print_terminator = false; // don't end paragraph at this point
        }
//        else
  //      {
        g_tag_terminator = g_tit.gT("NORMAL_BLOCK_FOP_END");   // would terminate this tag, someone else has to handle
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
            pr.println(g_tit.gT("NORMAL_BLOCK_FOP_END")); 
        }
        else
        {
            pr.println(); // just terminate line for readibility
        }
        // if spanning, let the following constructions print the paragraph terminator
//        pr.print(g_tag_terminator);  // will be end para, or just a newline, depending on "span" flag
    } // end start boilerplate text
    
    
    public void startText(String text,
		String anchor_name) throws Exception // start of sequence with inserted link, emphasis, etc
    {
        PrintWriter pr = getCurrentWriter();

        if (anchor_name != null)
        {
            pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                             BookUtils.eC(anchor_name) +
                             g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
        }
        
        pr.print(g_tit.gT("NORMAL_BLOCK_FOP"));   // start new paragraph
        pr.print(BookUtils.eT(text));
        g_tag_terminator = g_tit.gT("NORMAL_BLOCK_FOP_END");   // would terminate this tag, someone else has to handle
    }
    
    /*
     * must perform special coding for preformatted text
     * but it is actually rather simple
     */
    public void startPREText(String text,
                          String anchor_name) throws Exception // start of sequence with inserted link, emphasis, etc
    {
        PrintWriter pr = getCurrentWriter();

        if (anchor_name != null)
        {
            pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                     BookUtils.eC(anchor_name) +
                     g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
        }
        
        pr.print(g_tit.gT("PREFORMATTED_BLOCK_FOP"));   // start new paragraph allowing newlines to be processed
        pr.print(BookUtils.eT(text));
        g_tag_terminator = g_tit.gT("NORMAL_BLOCK_FOP_END");   // would terminate this tag, someone else has to handle
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
        /*
         * create an invisible block with the ID= in it
         */
        PrintWriter pr = getCurrentWriter();
        Iterator it = index_items.iterator();
        IndexRef ir = null;
        while (it.hasNext())
        {
            ir = (IndexRef)it.next(); // contains name and number
            pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                     "general_" + BookUtils.eC(ir.name) + "_" +
                     ir.getRefNumber() +
                             g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
//            String.valueOf(ir.position) + 
        }
    }
    
    
    public void setAnchor(String name) throws Exception // destination of a "go to"
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                 BookUtils.eC(name) + 
                 g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
    }

    
    public void insertSeparator(String anchor_name) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        if (anchor_name != null)
        {
            pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                             BookUtils.eC(anchor_name) +
                             g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
        }
        
        pr.print(g_tit.gT("SEPARATOR_BLOCK_FOP"));
    }
    
    /*
     * ALL LISTS will be a formatting matter, rather than letting the
     * renderer (browser) do the work, we have to indent, add bullet at
     * front, etc, etc.
     */
    public void endList() throws Exception // ends bulleted list
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("LIST_END_FOP")); // end the entire list
    }
    
    public void startList(String size)  throws Exception // start bulleted list
    {
        PrintWriter pr = getCurrentWriter();
	// if no size, set null
	g_list_font_size = BookUtils.returnContentsOrNull(size); // make global
	if (g_list_font_size != null)
	{
	System.err.println("FOP List Start, Size: " + g_list_font_size); // debug
		pr.print(g_tit.gT("LIST_START_FONT1_FOP") +
			g_list_font_size +
			g_tit.gT("LIST_START_FONT2_FOP"));
	}
	else
	{
		// ordinary list, no font size override
		pr.print(g_tit.gT("LIST_START_FOP"));
	}
    }

// for list items that contain inner sequences (quote, link, etc)
    public void insertListItemStart(String text,
		String anchor_name) throws Exception 
    {
        // new block with bullet and some text. more text will be added as we pass through
        PrintWriter pr = getCurrentWriter();
        /*
         * HERE HERE is there an issue with the bulleted list items and the fo:block
         * inheriting?
         */
// HERE set font size????
        pr.print(g_tit.gT("LIST_ITEM_START_FOP"));
        /*
         * now for some content blocks. try putting the anchor block first
         */
        if (anchor_name != null)
        {
            pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                             BookUtils.eC(anchor_name) +
                             g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
        }
        pr.print(g_tit.gT("NORMAL_BLOCK_FOP"));  // try a normal one for the list item
        pr.print(BookUtils.eT(text));
        // MUST BE TERMINATED ELSEWHERE
    }
    
    public void insertListItemEnd(String text) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        // end of list item
            pr.print(text + g_tit.gT("NORMAL_BLOCK_FOP_END") +
                     g_tit.gT("LIST_ITEM_END"));
    }
            
    public void insertListItem(String text,
		String anchor_name) throws Exception  // add bullet item
    {
        // new block with bullet and some text. 
        /*
         * is there an issue with inheriting properties from fo:block?
         */
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("LIST_ITEM_START_FOP"));
        /*
         * try putting the anchor block here, before the text block in the list item
         */
        if (anchor_name != null)
        {
            pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                             BookUtils.eC(anchor_name) +
                             g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
        }
         pr.print(g_tit.gT("NORMAL_BLOCK_FOP")); // we will try ours
        pr.print(BookUtils.eT(text));
        pr.print(g_tit.gT("NORMAL_BLOCK_FOP_END") +
                 g_tit.gT("LIST_ITEM_END"));
    }

    /*
     * this will be done by creating special blocks with formatting,
     * as we cannot rely on the renderer (browser) to do the work
     * THERE WILL BE various heading types, for testing one size fits all
     */
    public void createHeading(int type, String text,
		String anchor_name) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        if (anchor_name != null)
        {
            pr.print(g_tit.gT("INVISIBLE_BLOCK_FOP1") +
                             BookUtils.eC(anchor_name) +
                             g_tit.gT("INVISIBLE_BLOCK_FOP2")); // no content
        }
        /*
         * heading type 2 gets special handling. I have
         * to create two of these to make them work in
         * other media. First is always empty. Sooo,
         * we will give different handling to an empty
         * heading type 2
         */
        if (type == 2)
        {
            if (text.equals(""))
            {
                // for FOP, don't print it
           // debug     System.out.println("bypassed leavel 2 empty head");
            }
            else
            {
                // some text, print it
                pr.print(g_tit.gT("HEADING1_BLOCK_FOP"));   // start special text block 
                pr.print(BookUtils.eT(text));
                pr.print(g_tit.gT("HEADING1_BLOCK_FOP_END"));   // end special text block 
            }
        }
        else
        {
            pr.print(g_tit.gT("HEADING1_BLOCK_FOP"));   // start special text block 
            pr.print(BookUtils.eT(text));
            pr.print(g_tit.gT("HEADING1_BLOCK_FOP_END"));   // end special text block 
        }
    }
    
    /*
     * will use span
     */
    public void insertQuotedText(String text) throws Exception  // text in "middle" of a sequence with italics as a quote
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("START_INLINE_ITALIC_FOP") +
                 BookUtils.eT(text) + 
                 g_tit.gT("END_INLINE_FOP"));
    }
    
    public void insertCitedText(String text) throws Exception // text in "middle" of a sequence with italics as a cite
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("START_INLINE_ITALIC_FOP") +
        BookUtils.eT(text) + 
                 g_tit.gT("END_INLINE_FOP"));
    }
    
    public void insertEmphasizedText(String text) throws Exception  // text in "middle" of a sequence which is bolded
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("START_INLINE_BOLD_FOP") + 
                 BookUtils.eT(text) + 
                 g_tit.gT("END_INLINE_FOP"));
    }

    public void insertIntermediateText(String text) // text in the "middle" of a sequence that is ordinary
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(BookUtils.eT(text) + "\n");  // nothing much, just newline (hope it looks OK)
    }
    
    public void insertIntermediateBreak() // break in the "middle" of a sequence that is ordinary
    {
        PrintWriter pr = getCurrentWriter();
        // HOW TO DO THIS, dummy for now, but I think it can be done
  //      pr.print(BookUtils.eT(text) + "\n");  // nothing much, just newline (hope it looks OK)
    }

    /*
     * will be replaced with the appropriate "link" code
     */
    public void insertLink(String href,
                           String text) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("LINK_EXTERNAL_FOP1") +
                 BookUtils.eT(href) +
                 g_tit.gT("LINK_FOP2") +
                 text +
                 g_tit.gT("LINK_END"));
    }
    
    public void insertSimpleText(String content) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("NORMAL_BLOCK_FOP"));   // start new paragraph
        pr.print(BookUtils.eT(content));
        pr.print(g_tit.gT("NORMAL_BLOCK_FOP_END"));   // end new paragraph
        g_tag_terminator = g_tit.gT("NORMAL_BLOCK_FOP_END");   // would terminate this tag, someone else has to handle
    }
    
    public void insertBlockQuote(String content) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("NORMAL_BLOCK_QUOTE_FOP"));   // start new paragraph
        pr.print(BookUtils.eT(content));
        pr.print(g_tit.gT("NORMAL_BLOCK_QUOTE_FOP_END"));   // end new paragraph
        g_tag_terminator = g_tit.gT("NORMAL_BLOCK_QUOTE_FOP_END");   // would terminate this tag, someone else has to handle
    }
    
    /*
     * will use link code
     */
    public void insertSeeAlso(
        String filename,
        String link,
        String content,
        String middle_text,
        String final_text) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(g_tit.gT("LINK_FOP1") +
                 BookUtils.eC(link) + 
                 g_tit.gT("LINK_FOP2") +
                 BookUtils.eT(content) +
                 g_tit.gT("LINK_END"));
//        pr.print("See: " + filename + " : " +
        if (final_text != null)
        {
            pr.println(BookUtils.eT(final_text) + g_tit.gT("NORMAL_BLOCK_FOP_END"));
            //
            // this sequence is now OVER
            //
            return; // Either we have final text, middle text, or none
        } // end if final text
        if (middle_text != null)
        {
            /*
             * the last thing we printed was the anchor tag. The
             * inner text will be the anchor text,
             * and the following text will be printed
             * with nothing following
             */
            pr.println(BookUtils.eT(middle_text));
        }
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
					String anchor_name) throws Exception
    {
	if (remote)
	{
		throw new Exception("PDF cannot use remote images! " +
			full_image_location);	
	}
        // imbed image as external-image
        //
        // add block(s) with caption (breaks used???)
        //
        PrintWriter pr = getCurrentWriter();
	/*
	 * like the HTMLSink, we have a very complex set of
	 * XML to create. Using the tag fragments in the JSON
	 * becomes pretty useless. For now, we will just hard-code.
	 * Also, using some options that were suggested online
	 */
	// pdf_scale may become obsolete, we are ignoring it (could be null)
//        float scale = Float.valueOf(pdf_scale) * 100.f;  // make into percentage from decimal <= 1.0
        
        if (anchor_name == null)
        {
		throw new Exception("Images MUST have anchor ID's! Image: " +
                                    full_image_location);
	}
        pr.println("<fo:block id=\"" +
		BookUtils.eC(anchor_name) + 
		"\" text-align=\"center\" space-before=\"15pt\">" +
		"\n<fo:external-graphic src=\"pics/" +

                 BookUtils.eT(full_image_location) + 
		    "\" \nwidth=\"100%\" content-height=\"100%\" content-width=\"scale-to-fit\" scaling=\"uniform\" /></fo:block>");
/*
suggested from online forum
<fo:external-graphic
    src="url('...')"
    width="100%"
    content-height="100%"
    content-width="scale-to-fit"
    scaling="uniform"
not sure about following, omitted
    xsl:use-attribute-sets="img"/>
*/
        
        
        if (caption != null)
        {
            /*
             * there is a caption, put it in after the image
             * CAPTION MAY CONTAIN LINE BREAKS!!
             * Separate block of caption for each line
             */
            String tokens[] = getBrokenLines(caption);
            for (int inner = 0 ; inner < tokens.length ; inner++)
            {
                if (inner == 0)
                {
                    pr.print(g_tit.gT("CAPTION_BLOCK_FOP_FIRST"));
                }
                else
                {
                    pr.print(g_tit.gT("CAPTION_BLOCK_FOP_REST"));
                }
                pr.print(BookUtils.eT(tokens[inner]) + 
                     g_tit.gT("CAPTION_BLOCK_FOP_END"));
            }
        }
    } // end process inline image

    public void noteReferenceDuplicate(String key,
                                       String content,
                                       String context) // for debugging, this gets stored somewhere
    {
     // NOT NOW FOR FOP  
    }
    

    /**
     * receive a String and return it with HTML line breaks present for any
     * '::' characters
     */
    public String addLineBreaks(String x)
    {
        return x; // NEED TO FIGURE OUT WHAT IS TO BE DONE WITH FOP
    }
    
    public String[] getBrokenLines(String s)
    {
        String [] tokens = s.split("::");
        return tokens;
    }

    /**
     * receive a String and return it with simple HTML line breaks present for any
     * '::' characters
     * also remove any single quotes (sorry about that)
     */
    public String addSimpleLineBreaks(String x)
    {
        return x;  // FOP MUST DO SOMETHING HERE
    }

    /**
     * receive a String and return it so that break requests (::) are
     * removed
     * also remove any single quotes (sorry about that)
     */
    public String filterForFOPTitle(String x)
    {
        String newer = x.replaceAll("::"," -- ");   // replace every double colon with some simple text
        String newer2 = newer.replaceAll("'"," ");   // replace every single quote with a space
        return newer2;
    }
    
    public void makeNewLine(PrintWriter pr)
    {
        // WHAT IS FOP TO DO?
    }
    
    public void createPrefixFOP(PrintWriter pr) throws Exception
    {
        pr.print(g_tit.gT("ROOT_XML"));

        /*
         * WE SPECIFY AS MUCH FORMATTING PROPERTIES as we can here,
         * so they inherit all the way down the tree, and so
         * simplify the XML that follows. Example is the font
         * and its nominal size.
         */
        /*
         * HERE HERE, we will get the default font and size from
         * the options.json!
         */
        pr.print(g_tit.gT("ROOT_FOP") + ">\n"); // notice CLOSURE here

	/*
	 * To handle column count, we just use slightly
	 * different boilerplate. Name of boilerplate is
	 * terminated by "1" or "2" meaning 1 or 2 column
	 */
	String col_count = g_tit.getProperty("PDF_COLUMN_COUNT");

        pr.print(g_tit.gT("LAYOUT_MASTER_FOP1"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP2"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP3_" + col_count)); // pick based on number of columns
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP4"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP5"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP6"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP7"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP8_" + col_count));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP9"));
        
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP10"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP11"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP12_" + col_count));

        pr.print(g_tit.gT("LAYOUT_MASTER_FOP13"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP14"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP15"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP16"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP17"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP18"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP19"));
        pr.print(g_tit.gT("LAYOUT_MASTER_FOP20"));

        /*
         * metadata, such as title and author provided by specialcontent provider
         */
        g_tit.createMetadata(pr);
    } // end createprefixfop
    
    
/*
 * terminate for now, but will create Indexes at end of document
 */
    public void createPostfixFOP(PrintWriter pr, 
                                  boolean make_index,
                                  TreeMap state_index_map,
                                  TreeMap abbrev_map,
                                  TreeMap city_index_map,
                                  TreeMap fac_index_map,
                                  TreeMap general_index_map) throws Exception
    {
    } // end create postfix FOP
    
    

    public void createAbbreviationIndex(PrintWriter pr,
                                        TreeMap abbrev_map) throws Exception
    {
        // not an index, but just the abbreviations listed by
        // using simple blocking, etc, as is done with PDF
        
        AbbreviationKey the_key = null;
        String the_abbrev = null;
        String abbrev_def = null;
        String a_title = null;
        Iterator inner = abbrev_map.keySet().iterator();
        boolean first_time = true;
        
        //process_header(1,"List of Military Abbreviations",false,true);  // this will be the abbreviation list
        
        while (inner.hasNext())
        {
            the_key = (AbbreviationKey)inner.next();
            the_abbrev = the_key.the_abbreviation;   // original format
            //            the_abbrev = the_key.comparison_key;   // lower case, no parens
            abbrev_def = (String)((List)abbrev_map.get(the_key)).get(0);
            a_title = the_abbrev + " -- " + abbrev_def;
            if (first_time)
            {
                pr.print(g_tit.gT("NORMAL_BLOCK_FOP") +
                         BookUtils.eT(a_title) +
                         g_tit.gT("NORMAL_BLOCK_FOP_END"));
                first_time = false;
            }
            else
            {
                // no whitespace above
                
                pr.print(g_tit.gT("NORMAL_BLOCK_FOP_REST") +
                         BookUtils.eT(a_title) +
                         g_tit.gT("NORMAL_BLOCK_FOP_END"));
            }
        } // end for each abbreviation
        
    } // create abbreviation listing

    
    /*
     * not used any more
     */
    public void makeOutlinesempty(
        PrintWriter pr,
        TreeMap  state_index_map,
        TreeMap  abbrev_map,
        TreeMap  city_index_map,
        TreeMap  fac_index_map,
        TreeMap  general_index_map)
    {
        // no PDF bookmarks at this time for a non-state book
    }
    /*
     * not used any more
     */
    public void makeOutlinesdirect( // WILL REMOVE this when we have the generic system working
        PrintWriter pr,
        TreeMap  state_index_map,
        TreeMap  abbrev_map,
        TreeMap  city_index_map,
        TreeMap  fac_index_map,
        TreeMap  general_index_map) throws Exception
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
         */
        
        /*
         * only create special bookmarks for PDF, if we want any indexes
         */
	if (g_options.wantAnyIndex())
        {
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
            
            pr.println(g_tit.gT("BOOKMARKS_FOP1")); // start bookmarks
            
            
            // GENERAL INDEX FIRST
            //
	if (g_options.wantGeneralIndex())
            {
                
                int colon_location = 0;
                pr.println(g_tit.gT("BOOKMARKS_DUMMY_FOP"));
                pr.println(g_tit.gT("BOOKMARKS_GENERAL_FOP1")); // unterminated at this point

                
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
                        pr.print(g_tit.gT("BOOKMARKS_GENERAL_FOP2") +
                                 BookUtils.eC(index_item) + 
                                 g_tit.gT("BOOKMARKS_SEP_FOP") +
                                 index_ref.ref_number +
                                 g_tit.gT("BOOKMARKS_END_FOP"));
                        pr.print(g_tit.gT("BOOKMARKS_TITLE_FOP") +
                                          index_item +
                                          g_tit.gT("BOOKMARKS_TITLE_END_FOP") +
                                 g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP"));
                    } // end for each reference
                } // end for each general index item
                pr.println(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // terminate general tree
            } // end want general index
	if (g_options.wantStateIndex())
            {
                // STATE INDEX
                //
                pr.println(g_tit.gT("BOOKMARKS_DUMMY_FOP"));
                pr.println(g_tit.gT("BOOKMARKS_STATE_FOP1")); // no terminator
                
                inner = state_index_map.keySet().iterator();
                
                while (inner.hasNext())
                {
                    index_item = (String)inner.next();  // state name
                    // ONLY ONE STATE PER ENTRY
                    some_items = (List)state_index_map.get(index_item);
                    the_ref = new ExactReferenceComparable((ExactReference)some_items.get(0)); // first item only
                    
                    // we are only processing state items
                    
                    pr.print(g_tit.gT("BOOKMARKS_STATE_FOP2") +
                        //     g_tit.gT("BOOKMARKS_SEP_FOP") +
                             BookUtils.eC(the_ref.state_name) +  // escape state name
                    g_tit.gT("BOOKMARKS_END_FOP"));
                    pr.print(g_tit.gT("BOOKMARKS_TITLE_FOP") +
                             index_item +
                             g_tit.gT("BOOKMARKS_TITLE_END_FOP") +
                             g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP"));
                } // end for each state index item
                pr.println(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // terminate state tree
            } // end want state index
	if (g_options.wantCityIndex())
            {
                
                // CITY INDEX, grouped by initial letter
                
                first_time = true;
                
                
                String city_initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
                String city_current_initial_letter = "ZZZZZ";  
                /*
                 * in order to make a set of bookmarks with the alphabet letters (easier navigation
                 * to alphabet headings), we must gather them up for later placement
                 */
                TreeMap city_links = new TreeMap();        
                
                inner = city_index_map.keySet().iterator();
                
                while (inner.hasNext())
                {
                    index_item = (String)inner.next();  // city name
                    city_initial_letter = index_item.substring(0,1);
                    if (!city_initial_letter.equals(city_current_initial_letter))
                    {
                        /*
                         * New letter, we make a new bookmark grouping
                         */
                        city_links.put(city_initial_letter,"Cities '" + city_initial_letter + "'"); // index name "Cities A" etc
                        city_current_initial_letter = city_initial_letter;
                    }
                } // end for each item in city map
                
                pr.println(g_tit.gT("BOOKMARKS_DUMMY_FOP"));
                pr.println(g_tit.gT("BOOKMARKS_CITY_FOP1")); // no terminator
                
                /*
                 * start over again. We will create each city bookmark grouping separately
                 */
                inner = city_index_map.keySet().iterator();
                city_current_initial_letter = "ZZZZ";
                
                while (inner.hasNext())
                {
                    index_item = (String)inner.next();  // city name
                    city_initial_letter = index_item.substring(0,1);
                    if (!city_initial_letter.equals(city_current_initial_letter))
                    {
                        /*
                         * NEW INITIAL letter! Start new bookmark group
                         */
                        if (first_time)
                        {
                            first_time = false;
                        }
                        else
                        {
                            pr.print(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // TERMINATE PREVIOUS INITIAL LETTER ENTRY
                        }
                        pr.print(g_tit.gT("BOOKMARKS_DUMMY_FOP"));
                        pr.print(g_tit.gT("BOOKMARKS_TITLE_FOP") +
			(String)city_links.get(city_initial_letter) +
                                 " - Cities Group" + 
                                 //"Cities -- " + (String)city_links.get(city_initial_letter) +
                                 g_tit.gT("BOOKMARKS_TITLE_END_FOP")); // NO TERMINATOR (actual cities are below in the tree)
                        
                        city_current_initial_letter = city_initial_letter;
                    } // end if different initial letter seen
                    
                    // CAN BE SEVERAL CITIES PER ENTRY
                    some_items = (List)city_index_map.get(index_item);
                    inner2 = some_items.iterator();
                    while (inner2.hasNext())
                    {
                        
                        the_ref = new ExactReferenceComparable((ExactReference)inner2.next()); 
                        
                        // we are only processing city items
                        pr.print(g_tit.gT("BOOKMARKS_STATE_FOP2") +
                                 BookUtils.eC(the_ref.state_name) +  // escape state name
                                 g_tit.gT("BOOKMARKS_CITY_FOP3") +
                                 BookUtils.eC(the_ref.city_name) +  // escape city name
                                 g_tit.gT("BOOKMARKS_END_FOP"));
                        pr.print(g_tit.gT("BOOKMARKS_TITLE_FOP") +
                                 index_item +
                                 "  (" + the_ref.state_abbrev + ")" +
                                 g_tit.gT("BOOKMARKS_TITLE_END_FOP") +
                                 g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // terminate bookmark
                    } // end for each city with same name
                } // end for each city index item
                pr.print(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // TERMINATE PREVIOUS INITIAL LETTER ENTRY
                pr.print(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // terminate the high-level tree of "cities"
            } // end if want cities index
	if (g_options.wantFacilityIndex())
            {
                
                // FACILITY INDEXES (One for each initial letter)
                first_time = true;
                
                
                initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
                current_initial_letter = "ZZZZZ";  
                
                facs_it = fac_index_map.keySet().iterator();
                
                fac_ref = null;
                /*
                 * in order to make a set of bookmarks with the alphabet letters (easier navigation
                 * to alphabet headings), we must gather them up for later placement
                 */
                facility_links = new TreeMap();        
                while (facs_it.hasNext())
                {
                    fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
                    initial_letter = fac_ref.facility_name.substring(0,1);
                    if (!initial_letter.equals(current_initial_letter))
                    {
                        /*
                         * New letter, we make a new bookmark grouping
                         */
                        facility_links.put(initial_letter,"Facilities '" + initial_letter + "'"); // index name "Facilities A" etc
                        current_initial_letter = initial_letter;
                    }
                } // end for each item in facility map
                pr.println(g_tit.gT("BOOKMARKS_DUMMY_FOP"));
                pr.println(g_tit.gT("BOOKMARKS_FACILITY_FOP1")); // no terminator
                
                
                /*
                 * start over again. We will create each facility bookmark grouping separately
                 */
                facs_it  = fac_index_map.keySet().iterator();
                current_initial_letter = "ZZZZZ";  
                
                while (facs_it.hasNext())
                {
                    fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
                    initial_letter = fac_ref.facility_name.substring(0,1);
                    if (!initial_letter.equals(current_initial_letter))
                    {
                        /*
                         * NEW INITIAL letter! Start new bookmark group
                         */
                        if (first_time)
                        {
                            first_time = false;
                        }
                        else
                        {
                            pr.print(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // TERMINATE PREVIOUS INITIAL LETTER ENTRY
                        }
                        
                        pr.print(g_tit.gT("BOOKMARKS_DUMMY_FOP"));
                        pr.print(g_tit.gT("BOOKMARKS_TITLE_FOP") +
                                 "Facilities -- " + (String)facility_links.get(initial_letter) +
                                 g_tit.gT("BOOKMARKS_TITLE_END_FOP")); // NO TERMINATOR (actual facilities are below in the tree)
                        
                        current_initial_letter = initial_letter;
                    } // end if different initial letter seen
                    
                    inner2 = ((List)fac_index_map.get(fac_ref)).iterator();
                    while (inner2.hasNext())
                    {
                        ref2 = (ExactReference)inner2.next();
                        /*
                         * must build a reference that has facility in it, to
                         * use as a key to the page location to go to
                         */
                        the_ref2 = new ExactFacilityReferenceComparable(
                            ref2.state_abbrev,ref2.state_name,ref2.city_name,
                            BookUtils.createFacilityName(fac_ref,BookUtils.FOR_TEXT,false));
                        
                        // we are only processing facility items
                        
                        
                        a_title = BookUtils.createFacilityName(fac_ref,
                                                               BookUtils.FOR_HTML,  // must escape
                                                               false) + 
", " + ref2.state_abbrev;
                        pr.print(g_tit.gT("BOOKMARKS_STATE_FOP2") +
                                 BookUtils.eC(the_ref2.state_name) +  // escape state name
                                 g_tit.gT("BOOKMARKS_CITY_FOP3") +
                                 BookUtils.eC(the_ref2.city_name) +  // escape city name
                                 g_tit.gT("BOOKMARKS_FACILITY_FOP3") +
                                 BookUtils.eC(the_ref2.fac_name) +
                                 g_tit.gT("BOOKMARKS_END_FOP"));
                        pr.print(g_tit.gT("BOOKMARKS_TITLE_FOP") +
                                 a_title +
                                 g_tit.gT("BOOKMARKS_TITLE_END_FOP") +
                                 g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // terminate bookmark
                    } // end for each facility with same name
                } // end for each facility index item
                pr.print(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // TERMINATE PREVIOUS INITIAL LETTER ENTRY
                pr.print(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // terminate the high-level tree of "facilities"
            } // end want facility index
	if (g_options.wantNoPostalHistoryIndex())
            {
                // FACILITY (NO POSTAL HISTORY) INDEXES (One for each initial letter)
                first_time = true;
                
                the_fac = null;
                ref2 = null;
                the_ref2 = null;
                
                initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
                current_initial_letter = "ZZZZZ";  
                
                facs_it = fac_index_map.keySet().iterator();
                
                fac_ref = null;
                /*
                 * in order to make a set of bookmarks with the alphabet letters (easier navigation
                 * to alphabet headings), we must gather them up for later placement
                 */
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
                            /*
                             * New letter, we make a new bookmark grouping
                             */
                            facility_links.put(initial_letter,"Facilities (NO) '" + initial_letter + "'"); // index name "Facilities (NO) A" etc
                            current_initial_letter = initial_letter;
                        }
                    } // end if at least one internal entry is a NO Postal History
                } // end for each item in facility map
                
                pr.println(g_tit.gT("BOOKMARKS_DUMMY_FOP"));
                pr.println(g_tit.gT("BOOKMARKS_FACILITYNO_FOP1")); // no terminator
                
                /*
                 * start over again. We will create each facility bookmark grouping separately
                 */
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
                            /*
                             * NEW INITIAL letter! Start new bookmark group
                             */
                            if (first_time)
                            {
                                first_time = false;
                            }
                            else
                            {
                                pr.print(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // TERMINATE PREVIOUS INITIAL LETTER ENTRY
                            }
                            
                            pr.print(g_tit.gT("BOOKMARKS_DUMMY_FOP"));
                            pr.print(g_tit.gT("BOOKMARKS_TITLE_FOP") +
                                     "Facilities (NO) -- " + (String)facility_links.get(initial_letter) +
                                     g_tit.gT("BOOKMARKS_TITLE_END_FOP")); // NO TERMINATOR (actual facilities are below in the tree)
                            
                            current_initial_letter = initial_letter;
                        } // end if different initial letter seen
                        
                        inner2 = ((List)fac_index_map.get(fac_ref)).iterator();
                        while (inner2.hasNext())
                        {
                            ref2 = (ExactReference)inner2.next();
                            if (ref2.nopost)
                            {
                                // ONLY include inner items that have no postal history
                                /*
                                 * must build a reference that has facility in it, to
                                 * use as a key to the page location to go to
                                 */
                                the_ref2 = new ExactFacilityReferenceComparable(
                                    ref2.state_abbrev,ref2.state_name,ref2.city_name,
                                    BookUtils.createFacilityName(fac_ref,BookUtils.FOR_TEXT,false));
                                
                                // we are only processing facility items
                                
                                
                                a_title = BookUtils.createFacilityName(fac_ref,
                                                                       BookUtils.FOR_HTML,  // must escape
                                                                       false) + 
", " + ref2.state_abbrev;
                                
                                pr.print(g_tit.gT("BOOKMARKS_STATE_FOP2") +
                                         BookUtils.eC(the_ref2.state_name) +  // escape state name
                                         g_tit.gT("BOOKMARKS_CITY_FOP3") +
                                         BookUtils.eC(the_ref2.city_name) +  // escape city name
                                         g_tit.gT("BOOKMARKS_FACILITY_FOP3") +
                                         BookUtils.eC(the_ref2.fac_name) +
                                         g_tit.gT("BOOKMARKS_END_FOP"));
                                pr.print(g_tit.gT("BOOKMARKS_TITLE_FOP") +
                                         a_title +
                                         g_tit.gT("BOOKMARKS_TITLE_END_FOP") +
                                         g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // terminate bookmark
                            } // end if inner item has no postal history
                        } // end for each facility (NO) with same name
                    } // if at least one inner item has no postal history
                } // end for each facility (NO) index item
                pr.print(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // TERMINATE PREVIOUS INITIAL LETTER ENTRY
                pr.print(g_tit.gT("BOOKMARKS_BOOKMARK_END_FOP")); // terminate the high-level tree of "facilities (NO)"
            } // end want no postal history index
            pr.print(g_tit.gT("BOOKMARKS_TREE_END_FOP"));
        } // end if any bookmarks wanted at all
    } // end make bookmarks
     
    /*
     * generic code will create a structure with the index groups and entries
     * the output-specific object will make them into code that
     * is used, for instance, for PDF interactive, PDF print, HTML, etc
     */
    public void makeOutlines(
        PrintWriter pr,
        TreeMap  state_index_map,
        TreeMap  abbrev_map,
        TreeMap  city_index_map,
        TreeMap  fac_index_map,
        TreeMap  general_index_map) throws Exception
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
         */
        
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
            
            // GENERAL INDEX FIRST
            //
	if (g_options.wantGeneralIndex())
            {
                current_group = new IndexGroup(
                    g_tit.gT("GENERAL_INDEX_ID"),
                    g_tit.gT("GENERAL_INDEX_TITLE"),
                    g_tit.gT("GENERAL_INDEX_TITLE"));
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
                        current_group.children.add(new IndexEntry(
                                "general_" + BookUtils.eC(index_item) + 
                                 "_" + index_ref.ref_number, // ID contains escaped name and ref number
                                          index_item  // text name
                            ));
                    } // end for each reference
                } // end for each general index item
            } // end want general index
	if (g_options.wantStateIndex())
            {
                // STATE INDEX
                //
                current_group = new IndexGroup(
                    g_tit.gT("STATE_INDEX_ID"),
                    g_tit.gT("STATE_INDEX_TITLE"),
                    g_tit.gT("STATE_INDEX_TITLE"));
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
                        index_item  // text name
                        ));
                } // end for each state index item
            } // end want state index
	if (g_options.wantCityIndex())
            {
                current_group = new IndexGroup(
                    g_tit.gT("CITY_INDEX_ID"),
                    g_tit.gT("CITY_INDEX_TITLE"),
                    g_tit.gT("CITY_INDEX_TITLE"));
                    
                the_root.add(current_group); // add top to the list
                IndexGroup letter_group = null; // one more level are the lettered groups
                
                // CITY INDEX, grouped by initial letter
                
                first_time = true;
                
                String city_initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
                String city_current_initial_letter = "ZZZZZ";  
                /*
                 * in order to make a set of bookmarks with the alphabet letters (easier navigation
                 * to alphabet headings), we must gather them up for later placement
                 */
                TreeMap city_links = new TreeMap();        
                
                inner = city_index_map.keySet().iterator();
                
                while (inner.hasNext())
                {
                    index_item = (String)inner.next();  // city name
                    city_initial_letter = index_item.substring(0,1);
                    if (!city_initial_letter.equals(city_current_initial_letter))
                    {
                        /*
                         * New letter, we make a new bookmark grouping
                         */
                        city_links.put(city_initial_letter,"'" + city_initial_letter + "' Cities"); // index name "'A' Cities " etc
                        //city_links.put(city_initial_letter,"Cities '" + city_initial_letter + "'"); // index name "Cities A" etc
                        city_current_initial_letter = city_initial_letter;
                    }
                } // end for each item in city map
                
                /*
                 * start over again. We will create each city bookmark grouping separately
                 */
                inner = city_index_map.keySet().iterator();
                city_current_initial_letter = "ZZZZ";
                
                while (inner.hasNext())
                {
                    index_item = (String)inner.next();  // city name
                    city_initial_letter = index_item.substring(0,1);
                    if (!city_initial_letter.equals(city_current_initial_letter))
                    {
                        /*
                         * NEW INITIAL letter! Start new bookmark group
                         */
                        if (first_time)
                        {
                            first_time = false;
                        }
                        else
                        {
                        }
                        letter_group = new IndexGroup(
                                 "_city_" + city_initial_letter,
				(String)city_links.get(city_initial_letter) + 
				" - Cities Group",
                        //    "Cities -- " + (String)city_links.get(city_initial_letter),
				(String)city_links.get(city_initial_letter) + 
				" - Cities Group");
//                            "Cities -- " + (String)city_links.get(city_initial_letter));
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
                                                        "  (" + the_ref.state_abbrev + ")")); 
                        
                    } // end for each city with same name
                } // end for each city index item
            } // end want city index
	if (g_options.wantFacilityIndex())
            {
                current_group = new IndexGroup(
                    g_tit.gT("FACILITY_INDEX_ID"),
                    g_tit.gT("FACILITY_INDEX_TITLE"),
                    g_tit.gT("FACILITY_INDEX_TITLE"));
                    
                the_root.add(current_group); // add top to the list
                IndexGroup letter_group = null; // one more level are the lettered groups
                
                // FACILITY INDEXES (One for each initial letter)
                
                
                first_time = true;
                
                
                initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
                current_initial_letter = "ZZZZZ";  
                
                facs_it = fac_index_map.keySet().iterator();
                
                fac_ref = null;
                /*
                 * in order to make a set of bookmarks with the alphabet letters (easier navigation
                 * to alphabet headings), we must gather them up for later placement
                 */
                facility_links = new TreeMap();        
                while (facs_it.hasNext())
                {
                    fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
                    initial_letter = fac_ref.facility_name.substring(0,1);
                    if (!initial_letter.equals(current_initial_letter))
                    {
                        /*
                         * New letter, we make a new bookmark grouping
                         */
                        facility_links.put(initial_letter,"'" +
				initial_letter + "' Facilities Group"); // index name "A Facilities Group" etc
                        //facility_links.put(initial_letter,"Facilities '" + initial_letter + "'"); // index name "Facilities A" etc
                        current_initial_letter = initial_letter;
                    }
                } // end for each item in facility map
                
                /*
                 * start over again. We will create each facility bookmark grouping separately
                 */
                facs_it  = fac_index_map.keySet().iterator();
                current_initial_letter = "ZZZZZ";  
                
                while (facs_it.hasNext())
                {
                    fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
                    initial_letter = fac_ref.facility_name.substring(0,1);
                    if (!initial_letter.equals(current_initial_letter))
                    {
                        /*
                         * NEW INITIAL letter! Start new bookmark group
                         */
                        if (first_time)
                        {
                            first_time = false;
                        }
                        else
                        {
                        }
                        letter_group = new IndexGroup(
                            "_facility_" + 
                            initial_letter,
				(String)facility_links.get(initial_letter) +
                            " - Facilities Group",
                            //"Facilities -- " + (String)facility_links.get(initial_letter),
				(String)facility_links.get(initial_letter) +
                            " - Facilities Group");
//                            "Facilities -- " + (String)facility_links.get(initial_letter));
                        current_group.children.add(letter_group); // main group gets the initial letter
                        
                        current_initial_letter = initial_letter;
                    } // end if different initial letter seen
                    
                    inner2 = ((List)fac_index_map.get(fac_ref)).iterator();
                    while (inner2.hasNext())
                    {
                        ref2 = (ExactReference)inner2.next();
                        /*
                         * must build a reference that has facility in it, to
                         * use as a key to the page location to go to
                         */
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
                                                        a_title));
                    } // end for each facility with same name
                } // end for each facility index item
            } // end want facility index
	if (g_options.wantNoPostalHistoryIndex())
            {
                current_group = new IndexGroup(
                    g_tit.gT("FACILITY_NO_INDEX_ID"),
                    g_tit.gT("FACILITY_NO_INDEX_TITLE"),
                    g_tit.gT("FACILITY_NO_INDEX_TITLE"));

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
                /*
                 * in order to make a set of bookmarks with the alphabet letters (easier navigation
                 * to alphabet headings), we must gather them up for later placement
                 */
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
                            /*
                             * New letter, we make a new bookmark grouping
                             */
                            facility_links.put(initial_letter,"'" + initial_letter + "' FacNO"); // index name "A FacNO" etc
                            //facility_links.put(initial_letter,"Facilities (NO) '" + initial_letter + "'"); // index name "Facilities (NO) A" etc
                            current_initial_letter = initial_letter;
                        }
                    } // end if at least one internal entry is a NO Postal History
                } // end for each item in facility map
                
                
                /*
                 * start over again. We will create each facility bookmark grouping separately
                 */
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
                            /*
                             * NEW INITIAL letter! Start new bookmark group
                             */
                            if (first_time)
                            {
                                first_time = false;
                            }
                            else
                            {
                            }
                            letter_group = new IndexGroup(
                                "_facilityno_" + 
                                (String)facility_links.get(initial_letter),
                                //"Facilities (NO) -- " + (String)facility_links.get(initial_letter),
                                (String)facility_links.get(initial_letter) +
					" - FacNO",
//                                "Facilities (NO) -- " + (String)facility_links.get(initial_letter));
                                (String)facility_links.get(initial_letter) +
					" - FacNO");
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
                                /*
                                 * must build a reference that has facility in it, to
                                 * use as a key to the page location to go to
                                 */
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
                                                                a_title));
                                
                            } // end if inner item has no postal history
                        } // end for each facility (NO) with same name
                    } // if at least one inner item has no postal history
                } // end for each facility (NO) index item
            } // end if want facility (NO) index
            // the_root will be the entire thingie
    //        System.out.println(the_root); // this is what we pass to the renderer
            g_tit.renderBookmarks(pr,the_root);
        } // end if any bookmarks wanted at all
    } // end make bookmarks
            /*
            HAS BEEN moved to the special content creator object,
            so it will be specific to this book and this format
    public void endPageSeq(
        PrintWriter pr)
    {
        pr.print("</fo:page-sequence>\n");
    }
    */
    /*
     * generic code will create a structure with the index groups and entries
     * the output-specific object will make them into code that
     * is used, for instance, for PDF interactive, PDF print, HTML, etc
     */
    public void makeIndexes(
        PrintWriter pr,
        TreeMap  state_index_map,
        TreeMap  abbrev_map,
        TreeMap  city_index_map,
        TreeMap  fac_index_map,
        TreeMap  general_index_map) throws Exception
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
         */
        
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
            }
	if (g_options.wantNoPostalHistoryIndex())
            {
                current_group = BookUtils.createOverallNOFacilityIndex(
                    g_tit,
                    fac_index_map); // all entries
                the_root.add(current_group); // add top to the list
		/*
		 * HERE create a SEPARATE FILE with the body
		 * of the "no postal history" checklist.
		 * It will later be merged with some boilerplate FOP
		 * to make a stand-alone PDF (print format) file
		 * that contains the checklist only
		 * code stolen from HTML creator
		 */
                TreeMap nof = BookUtils.makeNOFChecklist(
                    g_tit,
                    fac_index_map); // will be in a different order
		/*
		 * There will be NO INDEX RENDERING, we just print it out
		 * HERE would be renderNOFChecklist in the g_tit
		 * kluge, we just make it here, and more FOP is needed to
		 * make it into a readable document
		 */
		Iterator city_it = nof.keySet().iterator();
		PrintWriter mypr = new PrintWriter("checklist_body_fopdf.txt");
                
		ExactFacilityReferenceComparableByCity the_ref22 = null;
		while (city_it.hasNext())
		{
		    the_ref22 = (ExactFacilityReferenceComparableByCity)city_it.next(); 
			mypr.println(
				//"<fo:block space-before=\"15pt\">" + NOT double space
				"<fo:block >" + // single space
				"___ " + // underlines for checking
				BookUtils.eT(the_ref22.state_name) + ", " +
				BookUtils.eT(the_ref22.city_name) + ", " +
				BookUtils.eT(the_ref22.fac_name) + "</fo:block>");
		} // end for each facility (NO) index item ordered by state,city
		mypr.flush();
		mypr.close();
            } // end if want facility (NO) index
            // the_root will be the entire thingie
            //        System.out.println(the_root); // this is what we pass to the renderer
            g_tit.renderIndex(pr,the_root,IndexRenderer.COMPLETE_INDEX);
        } // end if any bookmarks wanted at all
    } // end make indexes

	public void addToManifest(String name, String title, int flag)
	{
// NEEDED FOR PDF? We do make our own TOC, but
// it is embedded in the document content (??)
	}
    
	public void setOptions(Options op)
	{
		g_options = op;
	}

    public void startTable(String [] header_cells) throws Exception
	{
		PrintWriter pr = getCurrentWriter();
		pr.println("<fo:table>");
		if (header_cells != null) // optional
		{
			// separate table header section, ONE ROW
			pr.println("<fo:table-header><fo:table-row>");
			for (int i = 0 ; i < header_cells.length ; i++)
			{
				pr.println("<fo:table-cell><fo:block>" + 
				 BookUtils.eT(header_cells[i]) + "</fo:block></fo:table-cell>"); // escape text for XML
//				pr.println("<fo:table-cell><fo:block>" + header_cells[i] + "</fo:block></fo:table-cell>");
			}
			pr.println("</fo:table-row></fo:table-header>");
		}
		pr.println("<fo:table-body>"); // start body
	} // end start table
            
    public void endTable() throws Exception
	{
		PrintWriter pr = getCurrentWriter();
		pr.println("</fo:table-body>"); // end body
		pr.println("</fo:table>");
	}
            
    public void insertTableRow(String [] cells) throws Exception // multiple items in a row
	{
		PrintWriter pr = getCurrentWriter();
		pr.println("<fo:table-row>");
		for (int i = 0 ; i < cells.length ; i++)
		{
			pr.println("<fo:table-cell><fo:block>" + 
                         BookUtils.eT(cells[i]) + "</fo:block></fo:table-cell>"); // escape text for XML
			// cells[i] + "</fo:block></fo:table-cell>");
		}
		pr.println("</fo:table-row>");
	} // end insert table row

} // end  FOP interactive PDF sink
