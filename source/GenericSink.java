import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.util.TreeMap;
import java.util.List;
import org.w3c.dom.Document;

/*
 *
 * updated 10/9/2019
 * 
 * initial work to create Tables
 * 
 * remove dependency on old XMLUtils, and stub out Adapter usage 
 * 
 * The code here is the abstract
 * Sink for making the book. Children
 * of this object are for PDF,
 * HTML, EPUB, etc
 *
 * Change to allow passing of the Options object to this object
 *
 * TODO:
 *
 * Any of the following done?
 * o) change manifest handling for states, and general
 *     sections
 *
 *  o) expand starting methods to allow short titles to be passed
 *     This may be part of the task to make manifest entries
 *     before any text processing, hence the addToManifest method
 *
 *
 * DONE:
 *
 * o) added font size to startList(). not all sinks will use this number
 *
 * o) removed digest capability, dead-end project
 */
public abstract class GenericSink
{
    /*
     * globals 
     */
    public SpecialContentCreator g_tit;

	public Options g_options;

    public String g_list_font_size; // will contain font size requested
    /*
     * a lot of sinks will write to a "print" style file, so we
     * have there here for convenience
     */
    public PrintWriter g_pr;  // primary printed output
    public PrintWriter g_state_pr;  // printed output for each state as a separate file
    /*
     * the sink may have to perform some XML handling, so here is the
     * Document. Really no longer dependent on XML, everyone should 
     * be using text-based AUTHOR input!
     */
    public Document g_adap;

    
    public GenericSource g_source;
    
    // NO LONGER public int g_digest_flag;
    // NO LONGER public final static int DIGEST_NONE = 0;
 // NO LONGER    public final static int DIGEST_LEVEL1 = 1; // allow for possible other digest types
    
    public final static int MANIFEST_STATE = 0;
    public final static int MANIFEST_APPENDIX = 1; 
    public final static int MANIFEST_GENERAL = 2;
    public final static int MANIFEST_IMAGE = 3;
 
     public boolean g_make_index = true; // default, can be overridden
    
    public GenericSink() // NO PARAM instantiation 
    {   
	}

    public void init(GenericSource sou) 
    {   
        g_source = sou;
        g_tit = null;
        g_state_pr = null;  // no active state file

        g_pr = null; // no active main file
        
    } // end init

	// children must have this big old init method
    public abstract void init(GenericSource sou,
                    TreeMap state_index_map,
                    TreeMap abbrev_map,
                    TreeMap city_index_map,
                    TreeMap fac_index_map,
                    TreeMap general_index_map,
                    String file_ext) throws Exception;
   
	/*
	 * Stub out here, but at least use Document
	 */
    public void setAdapter(Document ada)
    {
        g_adap = ada;
    }
    
    public void setWriter(PrintWriter pr)
    {
        g_pr = pr;  // set primary writer, when this sink writes text output
    }
    public void setStateWriter(PrintWriter pr)
    {
        g_state_pr = pr;  // set state-specific writer, when this sink writes text output
    }
/*
 * at the start of any document is a title page
 *
 * we will have dynamically loaded and instantiated an
 * appropriate object to create the title, and do other
 * special processing.
 *
 * Some sinks don't create their own title page,
 * such as the pure PDF creator (probably). In
 * that case, the TitleCreator is null.
 */
    public abstract void startDocument(SpecialContentCreator tit) throws Exception;   // start the whole thing
    
    public abstract void endDocument( // end the whole thing
                                      TreeMap  state_index_map,
                                      TreeMap  abbrev_map,
                                      TreeMap  city_index_map,
                                      TreeMap  fac_index_map,
                                      TreeMap  general_index_map) throws Exception;
                                             
    
    public abstract Object getSinkResource();
    
    public abstract void startStates();
    
    public abstract void endStates() throws Exception;

    public abstract void startMainSection( String short_title) throws Exception;
    
    public abstract void endMainSection() throws Exception;

    public abstract void startAbbreviationSection(String app_name,
                                                  String app_title,
                                             
                                                  String short_title) throws Exception;
    
    public abstract void endAbbreviationSection(TreeMap abbrev_map,
                                                TreeMap state,
                                                TreeMap city,
                                                TreeMap fac,
                                                TreeMap general) throws Exception;

    public abstract void startAppendixSection(String name,
                                              String title,
                                       
                                              String short_title) throws Exception;
    
    public abstract void endAppendixSection(TreeMap state_index_map,
                                            TreeMap abbrev_map,
                                            TreeMap city_index_map,
                                            TreeMap fac_index_map,
                                            TreeMap general_index_map
                                           
                                            ) throws Exception;

    public abstract void startGeneralSection(String name,
                                              String title,
                                         
                                             String short_title,
					AuxiliaryInformation info) throws Exception;
    
    public abstract void endGeneralSection(TreeMap state_index_map,
                                            TreeMap abbrev_map,
                                            TreeMap city_index_map,
                                            TreeMap fac_index_map,
                                            TreeMap general_index_map) throws Exception;
    
    public abstract void setOptions(Options op) throws Exception;
    
    public abstract void startAState(String abbrev,
                                     String state_name,
                                     String anchor_name,
                                     List index_array,
                                     TreeMap state_map,
                                     TreeMap curr_city_map,
                                     TreeMap curr_fac_map,
                                 //    TitleCreator tit,
                                 
                                     String short_title) throws Exception; // maps needed for index, if per state
    
    public abstract void endAState(TreeMap state_map,
                                   TreeMap current_city,
                                   TreeMap current_fac) throws Exception;
    
    public abstract void startACity(String the_abbrev,
                                    String the_state,
                                    String the_city,
                                    String anchor_name,
                                    List index_list) throws Exception;
    
    public abstract void endACity();
    
    public abstract void startAFacility(String the_abbrev,
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
                                        List index_array) throws Exception;
    
    public abstract void endAFacility(String the_state,
                                      String the_city) throws Exception;
    
    public abstract void setSpecialTerminator(String st); // a kind of a state marker, often used in HTML output

    public abstract void preProcessInlineImage(
		boolean remote,
		String thumb_image_location,
                                               String full_image_location,
                                               String pdf_scale,
                                               String html_width,
                                               String pdf_use,
                                               String caption);

/* no longer
    public abstract void preProcessInlineDigestImage(
        String full_image_location,
        String thumb_image_location,
        String pdf_scale,
        String html_width,
        String pdf_use,
        String title,
        String caption);
*/

    public abstract void processInlineImage(
		boolean remote,
		String thumb_image_location,
                                               String full_image_location,
                                               String pdf_scale,
                                               String html_width,
                                               String pdf_use,
                                               String caption,
						String anchor_content) throws Exception;

/* no longer
    public abstract void processInlineDigestImage (String full_image_location,
                                                   String thumb_image_location,
                                                   String pdf_scale,
                                                   String html_width,
                                                   String pdf_use,
                                                   String title,
                                                   String caption) throws Exception;
*/

    public abstract void startBoilerplate(int type,
                                          String inner,
                                          String preceed,
                                          boolean span) throws Exception;  // boilerplate text
            
    public abstract void insertSeeAlso(String filename,
                                       String link,
                                       String content,
                                       String middle_text,
                                       String final_text) throws Exception;
            
    public abstract void insertLink(String href,
                                    String text) throws Exception;

    public abstract void insertEmphasizedText(String text) throws Exception;

    public abstract void insertQuotedText(String text) throws Exception;
    
    public abstract void insertCitedText(String text) throws Exception;
    
    public abstract void insertBlockQuote(String text) throws Exception;

    public abstract void insertIntermediateText(String text);
            
    public abstract void insertIntermediateBreak();

    public abstract void createHeading(int type, String text,
		String anchor_content) throws Exception;

    public abstract void startList(String size) throws Exception;
            
    public abstract void insertListItem(String text,
		String anchor_content) throws Exception;

// for list items that contain inner sequences (quote, link, etc)
    public abstract void insertListItemStart(String text,
		String anchor_content) throws Exception; 
            
    public abstract void insertListItemEnd(String text) throws Exception;
            
    public abstract void endList() throws Exception;

    public abstract void insertSeparator(String anchor_content) throws Exception;
            
    public abstract void setAnchor(String name) throws Exception; // destination of a "go to"

// destination of jump from index reference
    public abstract void setIndexLocation(List index_items) throws Exception;  

// start of sequence with inserted link, emphasis, etc
    public abstract void startText(String text,
		String anchor_content) throws Exception; 

    // start of sequence with inserted link, emphasis, etc
    // but contains Preformatted text, so that newlines are handled
    public abstract void startPREText(String text,
                                   String anchor_content) throws Exception; 

    public abstract void endText(String text);  // last text in a sequence
            
    public abstract void insertSimpleText(String text) throws Exception;

    public abstract void noteReferenceDuplicate(String key,
                                                String content,
                                                String context); // for debugging, this gets stored somewhere

	public abstract void addToManifest(String name, String title,
		int manifest_flag); // flag is from MANIFEST_items

	/*
	 * Table code
	 * 
	 * 1) start and end table
	 * 
	 * 2) start and end table row
	 * 
	 * 3) insert cell content
	 * 
	 * AS OF NOW, cell content is simple text. Making it more 
	 * complex is a mess. The design of table management in AUTHOR
	 * is a problem, just as it is a problem with underlying display
	 * technology. Tables are a bitch in HTML, they are a bitch in
	 * PDF (using FOP), etc, etc. Note that tables are handled pretty
	 * well in Kindle and EPUB, because they inherit behavior from
	 * the underlying HTML.
	 * 
	 * First cut tags: 
	 * 
	 * TABLE: and ENDTABLE:
	 * ROW: and ENDROW: 
	 * CELL:
	 * 
	 */
            
    public abstract void startTable(String [] header_cells) throws Exception; // header optional
            
    public abstract void endTable() throws Exception;
            
    public abstract void insertTableRow(String [] cells) throws Exception; // multiple items in a row
    
} // end generic sink
