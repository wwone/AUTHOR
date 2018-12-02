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
 * Changed L: (URL) to be listed in Courier. This should 
 * obviate the need for repeating the URL in the text.
 * 
 * ADD processing of options from options.json file. In
 * particular, we must create either 1-column PDF (ideal)
 * or 2-column PDF (DEFAULT for print).
 * 
 * PARENT class will perform formatting for print, getting 
 * its column count flag and boilerplate items from the
 * JSON file. These files are set up for 1 or 2-column
 * our DEFAULT will be "2"
 * 
 * The code here is the sink for creating
 * a FOP file. This file can be passed
 * to the Apache FOP (XML:FO) package, which will
 * create a PDF file which will be printed. This
 * is NOT the same as a PDF file that will be
 * read inside Adobe Reader, etc, because that
 * is an interactive product. Other formats are possible,
 * but this work is being done specifically as an
 * alternative for creating PDF.
 *
 * We are creating the .fo file directly, not
 * using XSLT. While the content for this book
 * system is in created in XML, the XML is too
 * complex to be easily handled by XSLT.
 *
 * UNIQUE for this version is:
 * o) No interactive page headings (DONE)
 * o) No internal links, all links
 *     will become page references using [] (DONE)
 * o) No bookmarks (DONE)
 * o) No external links (manually put URL into text) (DONE)
 * o) Each section should start on right page
 *    (odd). This means that sections start
 *    a new right-left flow. ISSUE: we are
 *    starting a left-right page sequence
 *    whenever we need to alter page numbering,
 *    such as TOC, main body, index
 *    It would be nice to force a right page
 *    when we hit a new section. can this be done?
 * o) The Preface should probably be two pages with
 *    "intentional blank" after. How to do this?
 * o) No cover image (DONE)
 *
 * ISSUES:
 *
 * *) Following no longer applies. We will just use
 *    Helvetica. 
 *    start obsolete
 *    Issues with publishing the final PDF with Lulu
 *    (others?). Embed the fonts!  The primary root
 *    element gives the font of Arial, the
 *    printer ornaments are now PrinterOrnamentsOne
 *    end obsolete
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
 *
 * NEED:
 *
 */
public class FOPPrintSink extends FOPPDFSink
{
    /*
     * globals: use the globals from PDF creator, but
     * change as needed for print requirements
     */
    
    public FOPPrintSink() // NO PARAM
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
            g_pr = null; 
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
    public FOPPrintSink(GenericSource sou)
    {   
    //    super(sou);
    }
    
    /*
     * override parent
     *
     * We DO NOT make bookmarks for print output
     */
    public void makeOutlines(
        PrintWriter pr,
        TreeMap  state_index_map,
        TreeMap  abbrev_map,
        TreeMap  city_index_map,
        TreeMap  fac_index_map,
        TreeMap  general_index_map)
    {
    }
    
    /*
     * override parent
     * link to external resource will NOT
     * be marked
     */
    public void insertLink(String href,
                           String text)
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(
		text +
		" <fo:inline font-family=\"Courier\">[" +
                 BookUtils.eT(href) + // must escape a URL
		"]</fo:inline>"
            );
    } // end insert link (URL)
    
    /*
     * override parent
     * NO INTERNAL LINKS, use page number reference
     */
    public void insertSeeAlso(
        String filename,
        String link,
        String content,
        String middle_text,
        String final_text) throws Exception
    {
        PrintWriter pr = getCurrentWriter();
        pr.print(
                 BookUtils.eT(content) + " [" +
            g_tit.gT("PAGE_NUMBER_LINK") +
                BookUtils.eC(link) + 
            g_tit.gT("PAGE_NUMBER_LINK_END") +
            "]" 
            
            );
        if (final_text != null)
        {
            pr.println(BookUtils.eT(final_text) + 
		g_tit.gT("NORMAL_BLOCK_FOP_END"));
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
        }
    } // end insert see also
    
} // end  FOPPrint sink
