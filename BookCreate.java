import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringWriter;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;

/*
 * Leave XML input handling stubbed in. Use
 * W3C DOM. HOWEVER, we do not currently support 
 * this method of input. 
 * This system will try to read all input as AUTHOR format.
 */
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;


/*
 * 
 * edited 12/1/2018
 *
 * Remove dependency on old XMLUtils
 *
 * instantiate the Sink's by using the SinkFactory, rather
 * than hard-coding the Sink objects here
 * we use "g_special_content_object_end" for the name
 *
 * add RIM: for remote image (will be handled differently from local image)
 *
 * remove EPUB capability (use Calibre)
 *
 * add auxiliary metadata capability (section data to be
 * added to meta tags in such output as HTML)
 *
 * generalize options handling with Options object
 *
 * Just to say it: this is an e-book
 * creation system, driven by plain-ish
 * text input (called AUTHOR).
 *
 * Simplified ALL Sink objects. Many
 * options for the sinks are listed in
 * the "options.json" file.
 *
 * Removed dead-end projects, such as "digest".
 *
 * Notes:
 *
 * 1) Allow BOILERPLATE in AUTHOR. This is actually
 *    pretty easy, in that anything between BOILER: and
 *    ENDBOILER: can be stored as objects and dumped into
 *    the paragraph stream on request (BP: request)
 *
 * 2) Add font size setting to LIST:, so that in certain
 *    Sinks, the font size can be controlled. Some sinks
 *    ignore the value, such as Kindle and EPUB. Use of this
 *    value is up to the Sink. (DONE)
 *
 * 3) Add "lists" to the anchors. This allows items
 *    to be linked to that are organized by the author.
 *    The A: tag will have a second parameter, which is
 *    list name. When someone wants to digest all the items
 *    so added, they specify list name on MAKELIST: entry.
 *    For now, we use bulleted list display format. (DONE)
 *
 * 4) Study how to add AUTHOR entry for line breaks. It seems
 *    that implementations require that the whole paragraph
 *    or "block" need to have a setting. So, allow another
 *    paragraph type "PRE:". All text there and in subsequent
 *    lines would be shown with linefeeds as contained therein.
 *    Special formatting, such as bold and italic won't work
 *    because they are separate AUTHOR input lines, and
 *    would cause a linefeed to pass through. (??) (PRE DONE)
 *
 *    Actually no one allows for this type of pre-formatted
 *    or line break type in the source/sink pattern now. 
 *
 *    For the XML input, we have a "break" flag which is
 *    passed through, see next paragraph:
 *
 *    Rather messy, but we could do it. The text insertion methods
 *    would have a "break" flag. Default, no. If set, the sinks
 *    have to handle breaks after each text element. For instance,
 *    HTML-based (HTML, EPUBsimple, MOBI) would just put a
 *    "<br>" at the end of each line in a paragraph started
 *    with this flag set.
 *
 *    FOP, would have to specify "linefeed-treatment='preserve'"
 *    in the "fo:block" tag, and every newline in the
 *    block content would be passed through to the formatter.
 *    By default the linefeed-treatment attribute would NOT
 *    appear. The same issues apply, that is this does not work
 *    well with bold and italic, or any internal item like
 *    internet link, see also, etc, etc.
 * 
 *    ABOVE discussion of PRE and breaks all seems to work now 
 *
 */
/*
 * NOTES for AUTHORS using XML input (you should
 *   not do this any more, use AUTHOR input)
 *
 *   Funny place for it, but I've created a monster
 *   of issues with which attributes are noted with
 *   which tags. So,
 *
 * "anchor" attributes are allowed with:
 *
 * "state"
 * "city"
 * "fac"
 * "content" type "image"
 * "content" type "heading1"
 * "content" type "heading2"
 * "content" type "heading3"
 * "content" type "listitem"
 * "content" type "listitemfront"
 * "content" type "separator"
 * "content" type "textfront"
 *   NOTHING ELSE???!!!? (this includes plain text)
 *
 * "index" entry attribute is allowed with:
 *
 * "state"
 * "city"
 * "fac"
 *   NOTHING ELSE!
 */
/*
 * The code will now process the AUTHOR or XML and then
 * invoke a "back end" creator object. The basic
 * pattern is sink/source. This will be the source
 * pump, as there is only one kind of input.
 *
 * There will be sinks for:
 *
 *     1) HTML (several options control the format and type)
 *     2) REVEAL (an HTML format that supports stand-alone
 *        presentations. It is quite different from HTML, so it gets
 *        its own sink) look up "reveal.js" on the web.
 *     3) EPUB (a major variant of the HTML code) 6/16/2018 NO LONGER SUPPORTED
 *     4) FOPPDF (to make .fo file for Apache XML:FO creator, which
 *          becomes PDF, that is read in a PDF viewer (Adobe Reader?))
 *          replaces FOP sink
 *     5) EPUBSIMPLE (a variant of EPUB where we do NOT use 6/16/2018 NO LONGER SUPPORTED
 *          fancy CSS classes, etc
 *     6) KINDLE, a one-file, simplified HTML that can more
 *        easily be converted into a "MOBI" or "AZW3" file for Kindle
 *        (This DEFINITELY works better than making an EPUBSIMPLE 6/16/2018 NO LONGER SUPPORTED
 *        and converting to MOBI with various tools, such as Kindle 
 *        previewer or Calibre)
 *     7) FOPPrint (to make .fo file for Apache XML:FO creator, which
 *          becomes PDF that is passed to a printer for making
 *          a PAPER copy. This  is NOT like the interactive PDF
 *          created with FOPPDF)
 *
 *      8) Author (the newest way to write a book, a plain-ish text 
 *         format for authoring). We have a Sink, mustly to take
 *         old projects and convert them for new input as AUTHOR files.
 *
 *
 *  TODO:
 *
 *  *) Throw exception when "index" item is empty. This can happen
 *     easily when double colons are used. Normally, index items are
 *     separated by single colons. This causes an "empty" index reference,
 *     which can murder certain sinks. They die, because they are
 *     trying to strip off the initial letter. Also, an empty index
 *     entry looks dumb.
 *
 *  *) Many book formats require a "manifest" or TOC of the
 *     sections. We should use the "sink" to accumulate entries
 *     before the main sink code is invoked. This is a bit
 *     like pre-processing images, anchors, and other items
 *     that are accumulated before referenced. Each sink will 
 *     make a manifest according to its needs. Manifests
 *     are mostly DONE, work well with EPUB and MOBI 6/16/2018 EPUB NO LONGER SUPPORTED
 *
 *  *) Allow "see also" for fixed locations, not defined by "anchor"s. Example
 *     is general index, but could be other locations. EVERY book using this
 *     system must have a general index.
 */
public class BookCreate extends GenericSource
{
    /*
     * globals 
     */
     
	// project-specific metadata filename to read
	public final static String PROJECT_JSON = "project.json"; 
	// project- and format- specific options filename to read
	public final static String OPTIONS_JSON = "options.json"; 
    
    public int g_char;
    
    public Facility g_facility = null; // no facility seen yet

	public Options g_options = null;

    public boolean g_spanning; // true, we are inside a "textfront", no "textend" seen, or "span" active
    
    public String g_file_extension; // will be .html, .xhtml, or .xml, depending on the output processor

	public String g_last_list_item; // for debugging users author data

	public Document g_doc;
    
	public Project master_record = null;
      
    int author_state = 0;
    
    StringWriter cur = null;
    
     
	ImageReferenceNew current_image = null; // must be kept so we can update caption

	PreformattedTextGroup current_text = null; // must be kept as we add content

         ArrayList current_paragraph = new ArrayList();

    
    public TreeMap g_state_index_map;
    public TreeMap g_city_index_map;
    public TreeMap g_fac_index_map;
	public TreeMap g_current_city_index_map = null;  // filled and used for each state
	public TreeMap g_current_fac_index_map = null;  // filled and used for each state
    public TreeMap g_general_index_map;
    public TreeMap g_anchor_lookup_map;
    public int g_general_index_position = 0;
    public TreeMap g_abbrev_map;
       
    public TreeMap g_anchor_list_map;

    public SpecialContentCreator g_special_content_handler = null; // an interface to the desired creator
    public String g_special_content_object_end = "";    // will be HTML, MOBI, etc
    /*
     * list of options that indicate which, if any, indexes will be built
     * by the Sink code. Some may print nothing, others will
     * have the elaborate indexes used in the original state-listing-oriented
     *   WWI book
     *
     * NOTE NOTE, this is in the PROJECT: header for AUTHOR,
     * not in the format-specific options present in the project.json file!
     *
     */
    public String g_index_options = null; 
                    
    public final static void main(String[] args)
    {

        if (args.length < 2)
        {
            System.err.println("Usage: bookcreate inputfile type [HTML,KINDLE,FOPPDF,FOPPRINT,AUTHOR,REVEAL]");
            //System.err.println("Usage: bookcreate inputfile type [HTML,EPUB,KINDLE,FOPPDF,FOPPRINT,AUTHOR,REVEAL]");
            System.exit(1);
        } 

        try
        {
		BookCreate this_is_it = new BookCreate(args[0],args[1]);  // create
	}
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    public BookCreate(String filename, String the_type) throws Exception
    {   // everything in constructor
        super();
        g_state_index_map = new TreeMap();
        g_city_index_map = new TreeMap();
        g_fac_index_map = new TreeMap();
        g_general_index_map = new TreeMap();
        g_anchor_list_map = new TreeMap();
        g_abbrev_map = new TreeMap();
        g_spanning = false; // haven't started any text sequences yet

	/*
	 * instantiate Options from the JSON file
	 */
	g_options = new Options(OPTIONS_JSON);
System.out.println("Options: " + g_options); // debugging

        /*
         * try to figure out what kind the caller wanted
         * only escape is to "break" from the endless loop,
         * or throw an exception
         */
        while (true)
        {
            if (the_type.equalsIgnoreCase("html"))
            {
                g_file_extension = ".html";  // we make html files
                g_special_content_object_end = "HTML"; 
                break;
            } // end if html
            if (the_type.equalsIgnoreCase("htmlsingle"))
            {
                g_file_extension = ".html";  // we make html files
                g_special_content_object_end = "HTMLSingle"; 
                break;
            } // end if html single version
            if (the_type.equalsIgnoreCase("reveal"))
            {
                g_file_extension = ".html";  // we make html files
                g_special_content_object_end = "REVEAL"; 
                break;
            } // end if reveal
            if (the_type.equalsIgnoreCase("kindle"))
            {
                g_file_extension = ".html";  // we make html files
                g_special_content_object_end = "KINDLE";
                break;
            } // end if html for kindle
            if (the_type.equalsIgnoreCase("epub"))
            {
		throw new Exception("EPUB Sinks are no longer available: use Calibre to create EPUB from AZW3");
/*
                g_file_extension = ".xhtml";  // we make xhtml files
                g_special_content_object_end = "EPUB";
                g_sink = new EPUBSink(this,
                                      g_state_index_map,
                                      g_abbrev_map,
                                      g_city_index_map,
                                      g_fac_index_map,
                                      g_general_index_map,
                                      g_file_extension
                                      );
		g_sink.setOptions(g_options);
                break;
*/
            } // end if epub
            if (the_type.equalsIgnoreCase("epubsimple"))
            {
		throw new Exception("EPUB Sinks are no longer available: use Calibre to create EPUB from AZW3");
/*
                g_file_extension = ".xhtml";  // we make xhtml files
                g_special_content_object_end = "EPUBSIMPLE";
                g_sink = new EPUBSimpleSink(this,
                                      g_state_index_map,
                                      g_abbrev_map,
                                      g_city_index_map,
                                      g_fac_index_map,
                                      g_general_index_map,
                                      g_file_extension
                                      );
		g_sink.setOptions(g_options);
                break;
*/
            } // end if epubsimple
            if (the_type.equalsIgnoreCase("foppdf"))
            {
                g_file_extension = ".fopdf";  // we eventually make one big .fo file
                g_special_content_object_end = "FOPPDF";
                break;
            } // end if FOPPDF
            if (the_type.equalsIgnoreCase("author"))
            {
                g_file_extension = ".txt";  // we eventually make one big .fo file
                g_special_content_object_end = "AUTHOR";
                break;
            } // end if AUTHOR
            if (the_type.equalsIgnoreCase("fopprint"))
            {
                g_file_extension = ".foprint";  // we eventually make one big .fo file
                g_special_content_object_end = "FOPPrint";
                break;
            } // end if FOP print version
            // not any of the above
            //
            System.err.println("Output type not recognized: " + the_type);
            System.exit(1);
        } // end of endless loop, only can break out if valid input
	/*
	 * Appropriate variables are now set, use the Factory 
	 * to instantiate the Sink
	 */
	g_sink = SinkFactory.getSinkInstance(
		g_special_content_object_end); // string passed must be object name
	g_sink.init(this,
		      g_state_index_map,
		      g_abbrev_map,
		      g_city_index_map,
		      g_fac_index_map,
		      g_general_index_map,
		      g_file_extension
		      );
	g_sink.setOptions(g_options); // pass to special output handler for use later

	/*
	 * Sink now instantiated, we start processing the input
	 */

        int state = 0;
        g_state_index_map = new TreeMap();
        g_city_index_map = new TreeMap();
        g_fac_index_map = new TreeMap();
        g_general_index_map = new TreeMap();
        g_anchor_lookup_map = new TreeMap();
        g_abbrev_map = new TreeMap();
        
        Object obj = null;
        try
        {
            BufferedReader rr = new BufferedReader(new FileReader(new File(filename)));
		/* 
		 * read first line
		 */
		String first_line = rr.readLine();
		if (first_line == null)
		{
			throw new Exception("File: " +
				filename + ", is empty");
		}
		if (first_line.startsWith("PROJECT:"))
		{
			System.out.println("Recognized PROJECT");
			/*
			 * first, 
			 * create master record (global)
			 * then, split out sections
			 * then, pre-process sections, setting up such items
			 *  as anchors and images
			 */
			 		
			master_record = new Project(first_line.substring(8)); // remove PROJECT:
        		splitAuthorInput(rr); // split into sections
			processGlobalReferences();  // get globals from sections
			System.out.println("Project: " + master_record);   
			/*
			 * NOW, create document, and pass each 
			 * section and all of its contents
			 */      
			// pass instance of SpecialContentCreator that was created during instantiation of Project object
			g_sink.startDocument(g_special_content_handler); 
			processSections();
			g_sink.endDocument(
			 g_state_index_map,
			 g_abbrev_map,
			 g_city_index_map,
			 g_fac_index_map,
			 g_general_index_map
			);
	//		System.exit(0);
		} // end process AUTHOR input
		else
		{
			throw new Exception("First Line of AUTHOR input not recognized, must be PROJECT:");
            /*
             * ASSUME XML, but we no longer support it. Everything
             * stubbed out.
     DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setCoalescing(true); // we want CDATA to be treated as text
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        // stdin is the InputStream source
        g_doc = dBuilder.parse(System.in);

            Object top = g_doc.getRoot(doc);
             */
            /*
             * gather information from the structure to use later. in
             * particular:
             *     "anchors" to be referred to later with "see_also"
             *     state name/abbreviation list that can be used
             *         to create a state pull-down
	     *     pre-process images
            processGlobalReferences(top);  // note no text inside top element            
            g_sink.startDocument(g_special_content_handler); // pass instance of SpecialContentCreator
            processSections(top);
            g_sink.endDocument(
                g_state_index_map,
                g_abbrev_map,
                g_city_index_map,
                g_fac_index_map,
                g_general_index_map
                );
             */
	} // end process XML, which is just stubbed out
        } // end try working with input
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    } // end constructor
    

    /*
     * process all sections, using the split out AUTHOR lines
     */
    public void processSections() throws Exception
    {
    		// start with main section
			List main_section = (List)	master_record.all_sections.get(0); 	// ALWAYS first
	                g_sink.startMainSection(
                    "Introduction"
                    );
                processTextContent(main_section,
                                   new ExactReference("introduction","main section","main section"));
//                new ExactReference("index","main section","main section"));
                g_sink.endMainSection(
                    );

	//
/*
 * NOW, process contents of each  other sections
 */
	
		int len = master_record.all_sections.size();
		for (int inners = 1 ; inners < len ; inners++)
		{
			List section_content = (List)master_record.all_sections.get(inners); // next section
			BaseSection sec = getSectionInformation(section_content);
			if (sec instanceof StateSection)
			{
				StateSection stato = (StateSection)sec; // cast
				  String state_name = stato.content;
				String state_title = stato.primary;
				 if ( (state_name == null) || (state_title == null) )
				{
				    throw new Exception ("Abbreviation or Name of State entry is NULL!  Abbreviation: " +
						 state_name + ", Name: " + state_title);
				}
			    g_current_city_index_map = new TreeMap(); // ready for next state
			    g_current_fac_index_map = new TreeMap(); // ready for next state
			/* 
			 * UNTIL we can figure out how to integrate
			 * states, cities and facilities into the AUTHOR
			 * system, we will pass a null to the sink.
			 * probably wrong.
			 * this state was added to the g_state_index_map
			 * during the first pass
			 */
			g_sink.startAState(state_name,state_title,
				null, // anchor
				null, // index items
				       g_state_index_map,
			               g_current_city_index_map,
				   g_current_fac_index_map,
                               stato.secondary
                               );                
                
			       processTextContent(section_content,
			       new ExactReference(state_name,state_title,""));

		//        processTextContent(section_content,
          //                         new ExactReference(general_name,"General",general_name));
		// 
			       g_sink.endAState(g_state_index_map,
				     g_current_city_index_map,
				     g_current_fac_index_map); // would be needed to finish any existing state
   
			} // saw State object
			else
			{
				// not State, try abbreviations
				if (sec instanceof AbbreviationSection)
				{
					AbbreviationSection abbg = (AbbreviationSection)sec;
					String abb_name = abbg.content;
					String abb_title = abbg.primary;
					if ( (abb_name == null) || (abb_title == null) )
					{
					    throw new Exception ("Name or Title of Abbreviation Section is NULL!  Name: " +
						 abb_name + ", Title: " + abb_title);
					}
					g_sink.startAbbreviationSection(abb_name,abb_title,
						   abbg.secondary
					   );
					processTextContent(section_content,
					   new ExactReference(abb_name,"Abbreviation",abb_name));
					g_sink.endAbbreviationSection(
					    g_abbrev_map,
					    g_state_index_map,
					    g_city_index_map,
					    g_fac_index_map,
					    g_general_index_map
					    );
				} // end if abbreviation section
				else
				{
					// not state or abbreviations, call it general
					GeneralSection sectg = (GeneralSection)sec;
					 String general_name = sectg.content;
					String general_title = sectg.primary;
					if ( (general_name == null) || (general_title == null) )
					{
					    throw new Exception ("Name or Title of General Section is NULL!  Name: " +
						 general_name + ", Title: " + general_title);
					}
				g_sink.startGeneralSection(general_name,
					general_title,
					sectg.secondary,
					sectg.meta // meta could be null
				);
				processTextContent(section_content,
                                   new ExactReference(general_name,"General",general_name));
				g_sink.endGeneralSection(
				    g_state_index_map,
				    g_abbrev_map,
				    g_city_index_map,
				    g_fac_index_map,
				    g_general_index_map
				    );
			       } // end if general section (after checking for others)
			} // end not state
		} // end for each section
	
    } // end process the sections in the document

   
    /**
 	* split the Author input into groups of input
 	* lines, separating out all sections (of all types,
 	* including states). Note that the first section
 	* is always there by default, and does not need
 	* a SECTION: marker
     *
     */
    public void splitAuthorInput(BufferedReader rr) throws Exception
    {
		/*
		 * read the AUTHOR file, splitting into sections
		 * FIRST LINE (PROJECT:) already read in the stream
		 * "rr" is a bufferedreader that contains the rest of the Author code
		   *
   		* ALWAYS have a "main" section, which is all unseparated 
   		* text from the input stream before any SECTION: marker is seen
   		* NOTE: "master_record" is global and has been created
   		*/
   ArrayList xcurrent = new ArrayList(); // will contain main section
   master_record.all_sections.add(xcurrent); // add at very top
   while (true)
   {
	   String result = rr.readLine();
	   if (result == null)
	   {
	   	break;
	   }
	   if (result.startsWith("SECTION:"))
	   {
	   	// new section
	   	xcurrent = new ArrayList();
	   	xcurrent.add(result);
			master_record.all_sections.add(xcurrent);	   	
		System.out.println("Saw General Section: " + result); // debugging
	   } // end if general section (abbrev, appendix?)
	   else
	   {
		// not section: try others
		   if (result.startsWith("STATE:"))
		   {
		   	// new state
		   	xcurrent = new ArrayList();
	   		xcurrent.add(result);
			master_record.all_sections.add(xcurrent);
			System.out.println("Saw State Section: " + result); // debugging
		   } // end if state
		   else	   	
			{
			   if (result.startsWith("ABBREVIATIONS:"))
			   {
				// new (and only?) abbreviation section
				xcurrent = new ArrayList();
				xcurrent.add(result);
				master_record.all_sections.add(xcurrent);
				System.out.println("Saw Abbreviation Section: " + result); // debugging
			   } // end if abbreviations
			   else
			   {
				// no new section or state or abbreviations add this line
				xcurrent.add(result);
		           } // end else, not state: or section: or abbreviations:
			} // end not section or state
	} // end not general section
	} // end while reading entire author file stream
	master_record.dumpSections(); // debugging
} // end split author input

    /**
     * FOR AUTHOR INPUT
     * 
     * pre-process references that will be used globally:
     *  1) anchors (for see_also)
     *  2) states (for state pull-down)
     * 3) flags for:
     *     o) types of indexes wanted
     *     o) title page processor (object name)
     * 4) "anchor" items inside of "content" tags
     *    pre-process images
     *
     */
    public void processGlobalReferences() throws Exception
    {

	/*
	 * Pre-process, looking for items to cache, such as anchors and images
	 */
	// start with main
	List main_section = (List)	master_record.all_sections.get(0); 	// ALWAYS first
	// NOTE we have not started the document, per the sink, just
	// accumulating important items into the sink
	//
		g_sink.addToManifest("introduction","Introduction",
			GenericSink.MANIFEST_GENERAL);
		processGlobalContentReferences(main_section,"introduction" + g_file_extension);
	/*
	 * NOW, pre-process each of the other sections
	 */
	
		int len = master_record.all_sections.size();
		for (int inners = 1 ; inners < len ; inners++)
		{
			List section_content = (List)master_record.all_sections.get(inners); // next section
			BaseSection sec = getSectionInformation(section_content);
			// add to manifest, making sure the sink knows the type
			if (sec instanceof StateSection)
			{
				g_sink.addToManifest(sec.content,sec.primary,
				GenericSink.MANIFEST_STATE);
			}
			else
			{
				if (sec instanceof AppendixSection)
				{
					g_sink.addToManifest(sec.content,sec.primary,
					GenericSink.MANIFEST_APPENDIX);
				}
				else
				{
					// assume general
					g_sink.addToManifest(sec.content,sec.primary,
					GenericSink.MANIFEST_GENERAL);
				} // end not appendix or state
			} // end not state
			// sec may be any children of Section
			// process the global items, such as anchors
			processGlobalContentReferences(section_content,sec.content + g_file_extension);
		} // end for each section
	
	/*
	 * for debugging, dump out the anchor LIST map
	 */
	Iterator anchs = g_anchor_list_map.keySet().iterator();
/* debugging
	System.err.println("Dump of Anchor Lists\n");
	while (anchs.hasNext())
	{
		String top_name = (String)anchs.next();
		Map lists = (Map)g_anchor_list_map.get(top_name);
		Iterator listi = lists.keySet().iterator();
		while (listi.hasNext())
		{
			System.err.println(top_name + ": " + listi.next());
		}
	}
*/
	/*
	 * end debugging, dump out the anchor LIST map
	 */
    } // end pre-process the global references in the sections in the document


	/*
	 *  may return StateSection or GeneralSection or AbbreviationSection
	 *
	 */
	public BaseSection getSectionInformation(List section_content) throws Exception
	{
			Iterator inside = section_content.iterator();
			BaseSection sec = null;
			while (inside.hasNext())
			{
				String the_line = (String)inside.next();
// WORK HERE FOR ABBREVIATION and APPENDIX sections
				if (the_line.startsWith("SECTION:"))
				{
					 sec = new GeneralSection(the_line.substring(8)); // remove SECTION:
     				break; // found it
				} // end if general section:
				if (the_line.startsWith("STATE:"))
				{
					 sec = new StateSection(the_line.substring(6)); // remove STATE:
     				break; // found it
				} // end state: section	
				if (the_line.startsWith("ABBREVIATIONS:"))
				{
					 sec = new AbbreviationSection(the_line.substring(14)); // remove ABBREVIATIONS:
     				break; // found it
				} // end if abbreviations: section	
			} // end search for section: or state: indicator
			if (sec == null)
			{
				throw new Exception("Missing SECTION: or STATE: tag");
			}
			return sec;
	} // end get section info
		
    
    private List breakOutIndexEntry(String index_string,
                                    String the_abbrev,
                                    String the_state,
                                    String the_city,
                                    String narrative) throws Exception
    {
        ArrayList index_array = null;  // this method may return null, if no index entries present
        if (index_string != null)
        {
            index_array = new ArrayList();
            /*
             * there can be multiple index references embedded in a "state", "facility", etc
             * tag, as well as in the "index" content tag
             * THEY CANNOT CONTAIN THE COLON! IT IS THE SEPARATOR!
             * THEY CANNOT CONTAIN an empty entry (double colons)
             */
            String [] tokens = index_string.split(":");
            for (int inner = 0 ; inner < tokens.length ; inner++)
            {
                if (tokens[inner].equals(""))
                {
                    throw new Exception("Empty Index Entry (probably double colon) in: " +
                                        index_string + ", info: " + the_abbrev + ", " +
                                        the_state + ", " + the_city + ", " +
                                        narrative);
                }
                g_general_index_position++;
                putIntoMap(g_general_index_map,tokens[inner],
                           // make a new reference from the passed info
                           //
                           new GeneralIndexReference(the_abbrev,
                                                     the_state,
                                                     the_city,  // may be null
                                                     g_general_index_position),
                           narrative); // narrative used for debugging
                index_array.add(new IndexRef(tokens[inner],g_general_index_position));                                
            } // end for each index entry
        } // if embedded index(es) in the passed string from state, city, fac tag
        return index_array;
    } // end break out index entry
    




    /**
     * Process the text content of a section. At this point, we are 
     * interested in "image", "anchor" and "abbreviation" and so on.
     * 
     * Since this is AUTHOR input, the List we are passed contains
     * all lines within a particular section
     *
     * We will store the references for global use inside the sink.
     *
     * One of the results will be to add all States to the map
     *
     */
    public void processGlobalContentReferences(List section, String html_file_name)
        throws Exception
    {
        Iterator it = null;
        Object pel = null;
        Object el = null;
        List atts = null;
        Iterator attribute_it;
        String anchor_name = null;
        String the_type = null;
        String the_inline_image = null;
        // debugging System.out.println("processGlobalContentReferences called for: " +
          // debugging html_file_name);
          /*
           * look at all lines in SECTION, checking for anchors. 
           * When we see one
           * we store it in a lookup. This also checks for duplicates.
           *
           * Where are anchors?
           *
           * A: anchor statement stands alone
           * IM: optional anchor for an image
           * IM2: same, different image construct
           * we HAVE NOT embedded anchors inside objects, unlike the XML version
           *
           */
	 current_image = null; // must be kept so we can update caption
			
           author_state = 0; // must use to bypass image captions IM: type
           Iterator inner = section.iterator();
           while (inner.hasNext())
           {
           	String the_line = (String)inner.next();
           	switch (author_state)
           	{
           		case 0:
           		{
           			// watching for A: RIM: IM: and IM2:
           			if (the_line.startsWith("A:"))
           			{
           				// ANCHOR, test it
           				Anchor an = new  Anchor(the_line.substring(2)); // remove A:
           				if (g_anchor_lookup_map.containsKey(an.content))
					{
					    // already have an entry
					    //
					    throw new Exception("Duplicate anchor name: " +
                                                an.content + ", in file: " +
                                                html_file_name);
					}
					// unique, that is good
                        
					/*
					 * process special LISTS of anchors here
					 */
					// first, basic anchor list
					g_anchor_lookup_map.put(an.content,html_file_name); 
					addToAnchorList(an);

	           			break;
           			} // end if anchor marker
           			if (the_line.startsWith("RIM:"))
           			{
           				// new style image marker, remote
           			   current_image = new ImageReferenceNew(the_line.substring(4),true); 
//System.out.println("Created: " + current_image); //debug
           			   // check anchor for dups (anchor required for new style image tag)
           				if (g_anchor_lookup_map.containsKey(current_image.the_anchor))
					{
					    // already have an entry
					    //
					    throw new Exception("Duplicate anchor name: " +
                                                current_image.the_anchor + ", in file: " +
                                                html_file_name);
					}
					// unique, that is good
                        
					g_anchor_lookup_map.put(current_image.the_anchor,html_file_name); // that's all we need
   			   
					author_state = 1; // bypass next line because it is caption, then we go back to state 0
           				break;
           			} // end if RIM (new) image indicator
           			if (the_line.startsWith("IM:"))
           			{
           				// new style image marker, test it
           			   current_image = new ImageReferenceNew(the_line.substring(3),false); 
//System.out.println("Created: " + current_image); //debug
           			   // check anchor for dups (anchor required for new style image tag)
           				if (g_anchor_lookup_map.containsKey(current_image.the_anchor))
					{
					    // already have an entry
					    //
					    throw new Exception("Duplicate anchor name: " +
                                                current_image.the_anchor + ", in file: " +
                                                html_file_name);
					}
					// unique, that is good
                        
					g_anchor_lookup_map.put(current_image.the_anchor,html_file_name); // that's all we need
   			   
					author_state = 1; // bypass next line because it is caption, then we go back to state 0
           				break;
           			} // end if IM (new) image indicator
           			if (the_line.startsWith("IM2:"))
           			{
           				// old style image marker, test it
					ImageReference ir = new ImageReference(the_line.substring(4)); // remove IM2:

         				// check for existence of anchor (it was optional with old tag)
         					if (ir.the_anchor != null)
         					{
         				
           				   // check anchor for dups (anchor optional with old style image tag)
           					if (g_anchor_lookup_map.containsKey(ir.the_anchor))
                        {
                            // already have an entry
                            //
                            throw new Exception("Duplicate anchor name: " +
                                                ir.the_anchor + ", in file: " +
                                                html_file_name);
                        }
                        // unique, that is good
                        
                        g_anchor_lookup_map.put(ir.the_anchor,html_file_name); // that's all we need
          	           } // end if any anchor to test
          	           // NOW, preprocess the image within the sink
          	           
          	            g_sink.preProcessInlineImage(
				false, // local
          	            ir.the_thumb,
          	            ir.the_image,
          	            ir.the_pdfscale,
          	            ir.the_htmlwidth,
          	            ir.the_pdfuse,
          	            ir.the_caption);
						// also add manifest entry for image
								g_sink.addToManifest(
									ir.the_image,"Image",
									GenericSink.MANIFEST_IMAGE);

   			   
           				break;
           			} // end if IM2 (old) image indicator
					/*
					 * check for STATE: and if seen
					  * add this state to the global list, so that when we
             			* make pass 2, we'll have all states in the pulldown
             			*/
             			if (the_line.startsWith("STATE:"))
           			{
						StateSection sec = new StateSection(the_line.substring(6)); // remove STATE:
    			        		putIntoMap(g_state_index_map,sec.primary,
                       			new ExactReference(sec.content,sec.primary,""),
                       			"processGlobalContentReferences (Author) state index");
                       			break; 
           			 } // end if STATE seen
  
           			// not recognized as special at this time: FALL THROUGH
           			break; // some other text, ignore it
           		} // end state 0
           		case 1:
           		{
				String the_caption = ""; // initially empty
				if ( (the_line.startsWith("EMPTY:")) ||
				 (the_line.startsWith("E:")) )
				{
					// empty caption, leave as is
				}
				else
				{
					// line contents is the caption
					the_caption = the_line;
				}
           			// saw IM: or RIM: 
//           			   current_image has most of the info, caption is here
          	           // NOW, preprocess the image within the sink
          	           
          	            g_sink.preProcessInlineImage(
          	            current_image.remote,
          	            current_image.the_thumb,
          	            current_image.the_image,
          	            current_image.the_pdfscale,
          	            current_image.the_htmlwidth,
          	            current_image.the_pdfuse,
          	            //  caption is in the current line
				// unless "EMPTY:" specified
				the_caption);
				// also add manifest entry for image
				g_sink.addToManifest(current_image.the_image,"Image",
					GenericSink.MANIFEST_IMAGE);
				// Image has to be processed when we re-read the text

           			author_state = 0; // back to checking the tags
           			break;
           		} // end state 1
           	} // end switch on parsing author_state
           } // end loop over all lines in AUTHOR section
//        System.out.println("Section: " + html_file_name + ", anchor map: " + g_anchor_lookup_map);
    } // end process references, such as anchor, explicit and embedded

	/*
	 * add anchor to list by "list_name"
	 */
	public void addToAnchorList(Anchor an) throws Exception
	{
		// an.content is anchor name
		// an.list_name is list name IF ANY
		
		if (an.list_name == null)
		{
			return; // nothing, no list
		}
// debug		System.err.println("addToAnchorList: " + an);
		Map entries = (Map)g_anchor_list_map.get(an.list_name);
		if (entries == null)
		{
			// FIRST TIME
			TreeMap thelist = new TreeMap();
			thelist.put(an,an); // anchor object is key
			g_anchor_list_map.put(an.list_name,thelist);
		}
		else
		{	
			// existing list, add to it
			entries.put(an,an); // anchor object is key
		}
	} // end addtoanchorlist

        
    /**
     * Process the text content of a section. 
	  * markers are embedded for many
	  * items, such as bold, italics, images, anchors,
	  * index items, etc
	  * See Author.java for more     
     *
     * the_content, a list of Strings containing a section
     * pr PrintWriter to create the HTML stream
     * ref is an instance of the ExactReference for the position we
     *     are currently in. Of primary interest is the "abbrevation", which
     *     is used to get to the HTML file containing the text content
     *
     */
    public void processTextContent(List the_content, 
                                   //PrintWriter pr,
                                   ExactReference ref) throws Exception
    {
    	
    	// this is the AUTHOR input loop, not the processor for XML input!
    	
    	author_state = 0;
    	
    	Iterator lines = the_content.iterator();

		Object oo = null;
		
        current_paragraph.clear(); // make sure empty
    	
    	while (lines.hasNext())
    	{
    		String the_line = (String)lines.next();
            
            String trimmed = the_line.trim();
            if (trimmed.length() == 0)
            {
		/*
		 * this is an empty line. under the original
		 * design, this indicated an end of paragraph.
		 * 
		 * however, with added features, such as preformatted
		 * text, and bulleted lists, this is not a hard
		 * and fast rule.
		 * 
		 * an empty line is any line containing whitespace only, or
		 * zero length
		 */
		switch (author_state)
		{
			case 20:
			case 22:
			/*
			 * WONT work with bulleted list (kluge...)
			 */
			{
			    throw new Exception("Sorry, No Blank Lines in Bulleted Lists! Last seen list item:\n" + g_last_list_item);
			} // end states 20 or 22, inside bulleted list
			case 100:
			// preformatted retains empty lines
			{
				current_text.text_items.add(new Text(""));
				continue; // keep on reading
			}
			case 101:
			// preformatted retains empty lines
			{
				current_text.text_items.add(new Bold(""));
				continue; // keep on reading
			}
			case 102:
			// preformatted retains empty lines
			{
				current_text.text_items.add(new Italics(""));
				continue; // keep on reading
			}
		} // switch on state when see empty line
		// fall through, we treat as end of paragraph
		//  cur.write("<!-- saw empty line -->\n");
                dump_paragraph(current_paragraph);
                // above starts a new paragraph
                author_state = 1; // read stuff until end of paragraph
                continue; // read next and see what happens
            }
            /*
             * before checking state, we look for MAKELIST: to dump
             * out an Anchor  list
             */
            if (the_line.startsWith("MAKELIST:"))
            {
// debugging System.out.println("Saw MAKELIST:, when state=" + author_state);
                dump_paragraph(current_paragraph); // empty out any previous work
		/*
		 * name after MAKELIST: is list name, find it
		 */
		Map desired_list = (Map)g_anchor_list_map.get(
			the_line.substring(9));
		if (desired_list == null)
		{
			throw new Exception("MAKELIST: tried to dump: " +
				the_line + ", not found in anchor lists");
		}
		Iterator listit = desired_list.keySet().iterator();
                    g_sink.startList(""); // START a bulleted list
		while (listit.hasNext())
		{
			Anchor an = (Anchor)listit.next(); // Anchor is key
			// LIST will contain a link to the anchor location!
			// an.content is the anchor name, have to look it up
			g_sink.insertListItemStart("",null); // start, no anchor
			//g_sink.insertListItemStart("See: ",null); // start, no anchor
			String see_also_filename = (String)
				g_anchor_lookup_map.get(an.content);
                    g_sink.insertSeeAlso(
                        see_also_filename, // just looked it up
                        an.content, // link
                        an.narrative, // text that triggers link
                        null, // see_also_middle,  null
                        null // see_also_final, null
				); 
			g_sink.insertListItemEnd("");
		}
                    g_sink.endList(); // END a bulleted list
                continue; // read another line of input
            } // end if MAKELIST:
            /*
             * before checking state, we look for LIST: to start
             * a list
             */
            if (the_line.startsWith("LIST:"))
            {
// debugging System.out.println("Saw LIST:, when state=" + author_state);
                dump_paragraph(current_paragraph); // empty out any previous work
                // this begins special processing for a bulleted list
           //     cur.write("<content type=\"list\"/>\n");
		/*
		 * if there is a number following the LIST: string,
		 * it will be the PERCENTAGE font size desired. Not
		 * every sink will use the value, such as Kindle and
		 * EPUB. However, FOP and HTML-related should work
		 */
                    g_sink.startList(the_line.substring(5)); // START the list
                /*
                 * all following paragraphs will be treated as list items, until
                 * we see LISTEND:
                 */
                author_state = 20; // processing list items
                continue; // read another line of input
            } // end if LIST:
            if (the_line.startsWith("LISTEND:"))
            {
// debugging System.out.println("Saw LISTEND:, when state=" + author_state);
                // this ends special processing for a bulleted list
                dump_paragraph(current_paragraph); // finish any existing listitems
                        // debugging System.out.println("<!-- ending list -->\n");
                    g_sink.endList();
         //       cur.write("<content type=\"endlist\"/>\n");
                author_state = 0; // start looking for regular text
                continue; // read another line of input
            } // end if LISTEND:
            /*
             * BEFORE checking state, we look for comments
             */
            if (the_line.startsWith("!:"))
            {
                String comment_content  = the_line.substring(2); // remove !:
         //       cur.write("<!-- " + comment_content.replace("--","(double)") + " -->\n");
                continue; // not passed to the state processors
            } // end if comment seen
            /*
             * regardless of state, we look for PROJECT:
             */
            /*
             * ALL the various processing of the input stream
             */
            switch (author_state)
            {
                case 0:
                    // waiting for paragraph marker (or A:, RIM:, IM:, or IN:, or unmarked text to start a paragraph)
                {
                        
                    if (the_line.startsWith("STATE:"))
                    {
			break; // eat the STATE: line
			}
                    if (the_line.startsWith("ENDCITY:"))
                    {
			// no SINK code uses end city, but
			// we never know about the future
			// meanwhile...
			break; // eat the ENDCITY: line
			}
                    if (the_line.startsWith("CITY:"))
                    {
			City the_city = new City(the_line.substring(5)); // remove CITY:
                        dump_paragraph(current_paragraph); // finish whatever
			the_city.performSink(); // do what is needed (SIDE EFFECT, add to index maps)
				//g_sink.insertSimpleText(the_line);
				//g_sink.insertSimpleText("City above, length: " + result.length);
			// eat the CITY: line
                        break;
                    } // end if CITY:
                    if (the_line.startsWith("FAC:"))
                    {
                    	/*
                    	 * Facility object must be global, because
                    	 * we are going to have to terminate it
                    	 * when we see the ENDFAC: line. THERE IS NO
                    	 * OTHER WAY to terminate a facility!
                    	 */
					g_facility = new Facility(the_line.substring(4)); // remove FAC:
                        dump_paragraph(current_paragraph); // finish whatever
					g_facility.performSink(); // do what is needed (SIDE EFFECT, add to index maps and run startAFacility)
			// eat the FACILITY: line
                        break;
                    } // end if FAC:
                    if (the_line.startsWith("ENDFAC:"))
                    {
                    	/*
                    	 * Use the global Facility object to end the current
                    	 * facility
                    	 */
					dump_paragraph(current_paragraph); // finish whatever
					g_sink.endAFacility(g_facility.state_name, // g_facility had better not be null
					g_facility.city_name); 
					// eat the ENDFAC: line
                        break;
                    } // end if ENDFAC:
                    if (the_line.startsWith("SECTION:"))
                    {
			       // section_processing(the_line);
                        break; // eat the section: line
                    } // end if SECTION:
                    if (the_line.startsWith("ABBREVIATIONS:"))
                    {
                        break; // eat the abbreviations: line
                    } // end if SECTION:

                    if (the_line.startsWith("ABB:"))
                    {
			// abbreviation entry
			// item 0 key, item 1 value
			    String[] result = the_line.substring(4).split("::");
				// make entry for abbreviation with the 2 items
                           putIntoMap(g_abbrev_map,new AbbreviationKey(result[0]),result[1]," AUTHOR storing abbreviation definition");
                        break;
                    } // end if ABB:

                    if (the_line.startsWith("RIM:"))
                    {
                        image_processing_new(true,the_line.substring(4));
                        break;
                    } // end if RIM:

                    if (the_line.startsWith("IM:"))
                    {
                        image_processing_new(false,the_line.substring(3));
                        break;
                    } // end if IM:

                    if (the_line.startsWith("IM2:"))
                    {
                        image_processing_old(the_line);
                        break;
                    } // end if IM2:

                    if (the_line.startsWith("H1:"))
                    {
                        heading_processing(the_line,1);
                        break;
                    } // end if H1:

                    if (the_line.startsWith("H2:"))
                    {
                        heading_processing(the_line,2);
                        break;
                    } // end if H2:

                    if (the_line.startsWith("H3:"))
                    {
                        heading_processing(the_line,3);
                        break;
                    } // end if H3:

                    if (the_line.startsWith("S:"))
                    {
                        // finish previous paragraph
               //         cur.write("<!-- Start Paragraph with See also -->\n");
                        dump_paragraph(current_paragraph);
                        // above starts a new paragraph
                        current_paragraph.add(new SeeAlso(the_line.substring(2))); // remove S:
                        author_state = 1; // now looking for end of paragraph
                        break;
                    } // end if S:

                    if (the_line.startsWith("L:"))
                    {
                        // finish previous paragraph
             //           cur.write("<!-- Start Paragraph with Link -->\n");
                        dump_paragraph(current_paragraph);
                        // above starts a new paragraph
                        current_paragraph.add(new Link(the_line.substring(2))); // remove L:
                        author_state = 1; // now looking for end of paragraph
                        break;
                    } // end if L:
                    
                    if (the_line.startsWith("B:"))
                    {
                        // finish previous paragraph
            //            cur.write("<!-- Start Paragraph with Bold -->\n");
                        dump_paragraph(current_paragraph);
                        // above starts a new paragraph
                        current_paragraph.add(new Bold(the_line.substring(2))); // remove B:
                        author_state = 1; // now looking for end of paragraph
                        break;
                    } // end if B:
                        
                    if (the_line.startsWith("I:"))
                    {
                        // finish previous paragraph
               //         cur.write("<!-- Start Paragraph with Italics -->\n");
                        dump_paragraph(current_paragraph);
                        // above  starts a new paragraph
                        current_paragraph.add(new Italics(the_line.substring(2))); // remove I:
                        author_state = 1; // now looking for end of paragraph
                        break;
                    } // end if I:
                    if (the_line.startsWith("P:"))
                    {
                        // finish previous paragraph
             //           cur.write("<!-- Start Paragraph Plain -->\n");
                        dump_paragraph(current_paragraph);
                        // above starts a new paragraph
                        current_paragraph.add(new Text(the_line.substring(2))); // remove P:
                        author_state = 1; // now looking for end of paragraph
                        break;
                    } // end if P:
                    if (the_line.startsWith("A:"))
                    {
                        anchor_processing(the_line);
                        break;
                    } // end if A:
                    
                    if (the_line.startsWith("IN:"))
                    {
                        index_processing(the_line,ref);
                        break;
                    } // end if IN:
                    if (the_line.startsWith("PRE:"))
                    { // content empty, ignored...
                    	// finish previous, this is a special grouping
                    	dump_paragraph(current_paragraph);
					current_text = new PreformattedTextGroup();
                    	current_paragraph.add(current_text);
                    	 author_state = 100; // look for content until ENDPRE:
                        break;
                    } // end if PRE:
                    if (the_line.startsWith("BPRE:"))
                    { // content empty, ignored...
                    	// finish previous, this is a special grouping
                    	dump_paragraph(current_paragraph);
					current_text = new PreformattedTextGroup();
                    	current_paragraph.add(current_text);
                    	 author_state = 101; // look for content until ENDPRE:
                        break;
                    } // end if BPRE:
                    if (the_line.startsWith("IPRE:"))
                    { // content empty, ignored...
                    	// finish previous, this is a special grouping
                    	dump_paragraph(current_paragraph);
					current_text = new PreformattedTextGroup();
                    	current_paragraph.add(current_text);
                    	 author_state = 102; // look for content until ENDPRE:
                        break;
                    } // end if IPRE:
                    /*
                     * here we drop in if no special markers, we think this is text, but
                     * have seen nothing before. Treat as start of new paragraph
                     * WE HAVE NEVER SEEN ANY OTHER PARAGRAPH CONTENTS
                     */
                    // finish previous paragraph
       //             cur.write("<!-- Plain text, may be new paragraph -->\n");
                    dump_paragraph(current_paragraph);
                    // above starts a new paragraph
                    current_paragraph.add(new Text(the_line));
                    author_state = 1; // look for end of para
                    break;
                } // end case 0, waiting for start of paragraph
                
                case 1:
                {
                    // process entries, have seen start of paragraph, now waiting for end of paragraph
                        
                    if (the_line.startsWith("SECTION:"))
                    {
           // NEEDED? I wonder, since the text being processed is entirely WITHIN a section_processing(the_line);
			// eat the SECTION: line
                        break;
                    } // end if SECTION:
                    if (the_line.startsWith("STATE:"))
                    {
			break; // eat the STATE: line
			}
                    if (the_line.startsWith("ABBREVIATIONS:"))
                    {
			break; // eat the abbreviations: line
			}
                    if (the_line.startsWith("ENDCITY:"))
                    {
			// no SINK code uses end city, but
			// we never know about the future
			// meanwhile...
			break; // eat the ENDCITY: line
			}
                    if (the_line.startsWith("CITY:"))
                    {
			City the_city = new City(the_line.substring(5)); // remove CITY:
                        dump_paragraph(current_paragraph); // finish whatever
			the_city.performSink(); // do what is needed (SIDE EFFECT, added to index maps)
				//g_sink.insertSimpleText(the_line);
				//g_sink.insertSimpleText("City above, length: " + result.length);
			// eat the CITY: line
                        break;
                    } // end if CITY:
                    if (the_line.startsWith("FAC:"))
                    {
                    	// global!
					g_facility = new Facility(the_line.substring(4)); // remove FAC:
                        dump_paragraph(current_paragraph); // finish whatever
			
					g_facility.performSink(); // do what is needed (SIDE EFFECT, add to index maps)
					// eat the FAC: line
                        break;
                    } // end if FAC:
                    if (the_line.startsWith("ENDFAC:"))
                    {
                    	/*
                    	 * Use the global Facility object to end the current
                    	 * facility
                    	 */
					dump_paragraph(current_paragraph); // finish whatever
					g_sink.endAFacility(g_facility.state_name, // g_facility had better not be null
					g_facility.city_name); 
					// eat the ENDFAC: line
                        break;
                    } // end if ENDFAC:
  

                    if (the_line.startsWith("ABB:"))
                    {
			// abbreviation entry
			// item 0 key, item 1 value
			    String[] result = the_line.substring(4).split("::");
				// make entry for abbreviation with the 2 items
                           putIntoMap(g_abbrev_map,new AbbreviationKey(result[0]),result[1]," AUTHOR storing abbreviation definition");
                        break;
                    } // end if ABB:

                    if (the_line.startsWith("RIM:"))
                    {
                        image_processing_new(true,the_line.substring(4));
                        break;
                    } // end if RIM:

                    if (the_line.startsWith("IM:"))
                    {
                        image_processing_new(false,the_line.substring(3));
                        break;
                    } // end if IM:
                 
                    if (the_line.startsWith("IM2:"))
                    {
                        image_processing_old(the_line);
                        break;
                    } // end if IM2:

                    if (the_line.startsWith("H1:"))
                    {
                        heading_processing(the_line,1);
                        break;
                    } // end if H1:

                    if (the_line.startsWith("H2:"))
                    {
                        heading_processing(the_line,2);
                        break;
                    } // end if H2:

                    if (the_line.startsWith("H3:"))
                    {
                        heading_processing(the_line,3);
                        break;
                    } // end if H3:


                        
                    if (the_line.startsWith("S:"))
                    {
                        /*
                         * There is a whitespace issue with see also, something we have
                         * had to deal with in the book creator as well, and often not
                         * so well. PROBABLY should make sure there is whitespace on the
                         * end of preceeding text, regardless of what kind of text
                         * it is.....
                         */
                        // just insert see also
                        current_paragraph.add(new SeeAlso(the_line.substring(2))); // remove S:
                        author_state = 1; // now looking for end of paragraph
                        break;
                    } // end if S:
                        
                    if (the_line.startsWith("L:"))
                    {
                        /*
                         * There is a whitespace issue with link, something we have
                         * had to deal with in the book creator as well, and often not
                         * so well. PROBABLY should make sure there is whitespace on the
                         * end of preceeding text, regardless of what kind of text
                         * it is.....
                         */
                        // just insert link
                        current_paragraph.add(new Link(the_line.substring(2))); // remove L:
                        author_state = 1; // now looking for end of paragraph
                        break;
                    } // end if L:
                 
                    if (the_line.startsWith("B:"))
                    {
                        // just insert bold paragraph
                        current_paragraph.add(new Bold(the_line.substring(2))); // remove B:
                        author_state = 1; // now looking for end of paragraph
                        break;
                    } // end if B:
                        
                    if (the_line.startsWith("I:"))
                    {
                        current_paragraph.add(new Italics(the_line.substring(2))); // remove I:
                        author_state = 1; // now looking for end of paragraph
                        break;
                    } // end if I:
                    if (the_line.startsWith("PRE:"))
                    { // content empty, ignored...
                    	// finish previous, this is a special grouping
                    	dump_paragraph(current_paragraph);
					current_text = new PreformattedTextGroup();
                    	current_paragraph.add(current_text);
                    	 author_state = 100; // look for content until ENDPRE:
                        break;
                    } // end if PRE:
                    if (the_line.startsWith("BPRE:"))
                    { // content empty, ignored...
                    	// finish previous, this is a special grouping
                    	dump_paragraph(current_paragraph);
					current_text = new PreformattedTextGroup();
                    	current_paragraph.add(current_text);
                    	 author_state = 101; // look for content until ENDPRE:
                        break;
                    } // end if BPRE:
                    if (the_line.startsWith("IPRE:"))
                    { // content empty, ignored...
                    	// finish previous, this is a special grouping
                    	dump_paragraph(current_paragraph);
					current_text = new PreformattedTextGroup();
                    	current_paragraph.add(current_text);
                    	 author_state = 102; // look for content until ENDPRE:
                        break;
                    } // end if IPRE:
                    if (the_line.startsWith("P:"))
                    {
                        // finish previous paragraph
            //            cur.write("<!-- Start Paragraph Plain -->\n");
                        dump_paragraph(current_paragraph);
                        // above starts a new paragraph
                        current_paragraph.add(new Text(the_line.substring(2))); // remove P:
                        author_state = 1; // now looking for end of paragraph
                        break;
                    } // end if P:
                    
                    if (the_line.startsWith("A:"))
                    {
                        anchor_processing(the_line);
                        break;
                    } // end if A:
                    
                    if (the_line.startsWith("IN:"))
                    {
                        index_processing(the_line,ref);
                        break;
                    } // end if IN:
                    /*
                     * drop in here if no special markers, we treat this as plain text. However,
                     * to prevent too many "more text" entries, we will concatenate this
                     * with the previous Text entry (if there is one)
                     */
                    if (current_paragraph.size() > 0)
                    {
                        // there is a last entry, see if it is text
                        oo = current_paragraph.get(current_paragraph.size() - 1); // get last entry
                        if (oo instanceof Text)
                        {
                            Text work = (Text)oo; // cast
                            // now, append the new text content, checking for whitespace
                            // in the existing entry
                            if (work.content.endsWith(" "))
                            {
                                // already enough whitespace at end of current text item
                                work.content += the_line; // append new text, don't make a new Text entry
                            }
                            else
                            {
                                work.content += " " + the_line; // ensure whitespace
                            }
                        } // end last paragraph content item is text
                        else
                        { // not text preceeding, just add this text as new plain text item
                            current_paragraph.add(new Text(the_line));
                        }
                    } // end something already in paragraph content list
                    else
                    {  // current paragraph is empty, just add this text item
                        current_paragraph.add(new Text(the_line));
                    }
                        
                    author_state = 1; // look for end of para
                    break;
                    
                } // case 1, waiting end of para
                
                case 10:
                {
			/*
			 * this line will be the caption for
			 * the current image, or EMPTY: to indicate
			 * empty caption
			 */
			String the_caption = "";
			if (the_line.startsWith("EMPTY:"))
			{
				// empty caption, leave as is
			}
			else
			{
				// line contents is the caption
				the_caption = the_line;
			}

			current_image.setCaption(the_caption); // all one line
			g_sink.processInlineImage(
          	            current_image.remote,
          	            current_image.the_thumb,
          	            current_image.the_image,
          	            current_image.the_pdfscale,
          	            current_image.the_htmlwidth,
          	            current_image.the_pdfuse,
          	            current_image.the_caption,
          	            current_image.the_anchor); // new format, anchor NOT null
	//		cur.write(current_image.toString());
			author_state = 0; // after image, starting again
			break;
		}
                
                case 20:
                    // waiting for listitem marker
                {
                        
                    if (the_line.startsWith("RIM:"))
                    {
                        throw new Exception("Cannot have Image inside Bulleted List");
                    } // end if RIM:
                        
                    if (the_line.startsWith("IM:"))
                    {
                        throw new Exception("Cannot have Image inside Bulleted List");
                    } // end if IM:

                    if (the_line.startsWith("H1:") ||
                        the_line.startsWith("H2:") ||
                        the_line.startsWith("H3:"))
                    {
                        throw new Exception("Cannot have Heading inside Bulleted List");
                    } // end if Hx:

                    if (the_line.startsWith("S:"))
                    {
                        // finish previous paragraph
            //            cur.write("<!-- Start Listitem with See also -->\n");
                        dump_paragraph(current_paragraph);
                        // above starts a new paragraph
                        current_paragraph.add(new ListMarker()); // will tell dumper to dump out list items, not normal paragraphs
                        current_paragraph.add(new SeeAlso(the_line.substring(2))); // remove S:
                        author_state = 21; // now looking for end of listitem
                        break;
                    } // end if S:

                    if (the_line.startsWith("L:"))
                    {
                        // finish previous paragraph
             //           cur.write("<!-- Start Listitem with Link -->\n");
                        dump_paragraph(current_paragraph);
                        // above starts a new paragraph
                        current_paragraph.add(new ListMarker()); // will tell dumper to dump out list items, not normal paragraphs
                        current_paragraph.add(new Link(the_line.substring(2))); // remove L:
                        author_state = 21; // now looking for end of paragraph
                        break;
                    } // end if L:
                    
                    if (the_line.startsWith("B:"))
                    {
                        // finish previous paragraph
          //              cur.write("<!-- Start Listitem with Bold -->\n");
                        dump_paragraph(current_paragraph);
                        // above starts a new paragraph
                        current_paragraph.add(new ListMarker()); // will tell dumper to dump out list items, not normal paragraphs
                        current_paragraph.add(new Bold(the_line.substring(2))); // remove B:
                        author_state = 21; // now looking for end of paragraph
                        break;
                    } // end if B:
                        
                    if (the_line.startsWith("I:"))
                    {
                        // finish previous paragraph
        //                cur.write("<!-- Start Listitem with Italics -->\n");
                        dump_paragraph(current_paragraph);
                        // above  starts a new paragraph
                        current_paragraph.add(new ListMarker()); // will tell dumper to dump out list items, not normal paragraphs
                        current_paragraph.add(new Italics(the_line.substring(2))); // remove I:
                        author_state = 21; // now looking for end of paragraph
                        break;
                    } // end if I:
                    if (the_line.startsWith("LI:"))
                    {
                        // finish previous paragraph
         //               cur.write("<!-- Start Listitem  Plain -->\n");
                        dump_paragraph(current_paragraph);
                        // above starts a new paragraph
                        current_paragraph.add(new ListMarker()); // will tell dumper to dump out list items, not normal paragraphs
                        current_paragraph.add(new Text(the_line.substring(3))); // remove LI:
			g_last_list_item = the_line; // keep copy in case of user error
                        author_state = 21; // now looking for end of paragraph
                        break;
                    } // end if LI:
                    if (the_line.startsWith("P:"))
                    {
                        throw new Exception("Regular paragraph not allowed inside Bulleted List");
                    } // end if P:
                    if (the_line.startsWith("A:"))
                    {
                        throw new Exception("Anchor marker not allowed inside Bulleted List");
                    } // end if A:
                    
                    if (the_line.startsWith("IN:"))
                    {
                        throw new Exception("Index marker not allowed inside Bulleted List");
                    } // end if IN:
                    /*
                     * here we drop in if no special markers, we think this is text, but
                     * have seen nothing before. Treat as start of new paragraph
                     * WE HAVE NEVER SEEN ANY OTHER PARAGRAPH CONTENTS
                     */
                    // finish previous paragraph
         //           cur.write("<!-- Plain text, may be new listitem -->\n");
                    dump_paragraph(current_paragraph);
                    // above starts a new paragraph
                    current_paragraph.add(new ListMarker()); // will tell dumper to dump out list items, not normal paragraphs
                    current_paragraph.add(new Text(the_line));
                    author_state = 21; // look for end of para
                    break;
                } // end case 20, waiting for start of listitem
                case 21:
                {
                    // process entries, have seen start of listitem, now waiting for end of listitem
                        
                    if (the_line.startsWith("RIM:"))
                    {
                        throw new Exception("Cannot have Image inside Bulleted List");
                    } // end if RIM:
                        
                    if (the_line.startsWith("IM:"))
                    {
                        throw new Exception("Cannot have Image inside Bulleted List");
                    } // end if IM:

                    if (the_line.startsWith("H1:") ||
                        the_line.startsWith("H2:") ||
                        the_line.startsWith("H3:"))
                    {
                        throw new Exception("Cannot have Heading inside Bulleted List");
                    } // end if Hx:
                        
                    if (the_line.startsWith("S:"))
                    {
                        /*
                         * There is a whitespace issue with see also, something we have
                         * had to deal with in the book creator as well, and often not
                         * so well. PROBABLY should make sure there is whitespace on the
                         * end of preceeding text, regardless of what kind of text
                         * it is.....
                         */
                        // just insert see also
                        current_paragraph.add(new SeeAlso(the_line.substring(2))); // remove S:
                        author_state = 21; // now looking for end of listitem
                        break;
                    } // end if S:
                        
                    if (the_line.startsWith("L:"))
                    {
                        /*
                         * There is a whitespace issue with link, something we have
                         * had to deal with in the book creator as well, and often not
                         * so well. PROBABLY should make sure there is whitespace on the
                         * end of preceeding text, regardless of what kind of text
                         * it is.....
                         */
                        // just insert link
                        current_paragraph.add(new Link(the_line.substring(2))); // remove L:
                        author_state = 21; // now looking for end of listitem
                        break;
                    } // end if L:
                 
                    if (the_line.startsWith("B:"))
                    {
                        // just insert bold paragraph
                        current_paragraph.add(new Bold(the_line.substring(2))); // remove B:
                        author_state = 21; // now looking for end of listitem
                        break;
                    } // end if B:
                        
                    if (the_line.startsWith("I:"))
                    {
                        current_paragraph.add(new Italics(the_line.substring(2))); // remove I:
                        author_state = 21; // now looking for end of listitem
                        break;
                    } // end if I:
                    if (the_line.startsWith("P:"))
                    {
                        throw new Exception("Regular paragraph not allowed inside Bulleted List");
                    } // end if P:
                    if (the_line.startsWith("A:"))
                    {
                        throw new Exception("Anchor marker not allowed inside Bulleted List");
                    } // end if A:
                    
                    if (the_line.startsWith("IN:"))
                    {
                        throw new Exception("Index marker not allowed inside Bulleted List");
                    } // end if IN:
                    if (the_line.startsWith("LI:"))
                    {
                        // finish previous paragraph
         //               cur.write("<!-- Start listitem Plain -->\n");
                        dump_paragraph(current_paragraph);
                        // above starts a new paragraph
                        current_paragraph.add(new ListMarker()); // will tell dumper to dump out list items, not normal paragraphs
                        current_paragraph.add(new Text(the_line.substring(3))); // remove LI:
			g_last_list_item = the_line; // keep copy in case of user error
                        author_state = 21; // now looking for end of paragraph
                        break;
                    } // end if LI:
                    
                    /*
                     * drop in here if no special markers, we treat this as plain text. However,
                     * to prevent too many "more text" entries, we will concatenate this
                     * with the previous Text entry (if there is one)
                     */
                    if (current_paragraph.size() > 0)
                    {
                        // there is a last entry, see if it is text
                        oo = current_paragraph.get(current_paragraph.size() - 1); // get last entry
                        if (oo instanceof Text)
                        {
                            Text work = (Text)oo; // cast
                            // now, append the new text content, checking for whitespace
                            // in the existing entry
                            if (work.content.endsWith(" "))
                            {
                                // already enough whitespace at end of current text item
                                work.content += the_line; // append new text, don't make a new Text entry
                            }
                            else
                            {
                                work.content += " " + the_line; // ensure whitespace
                            }
                        } // end last paragraph content item is text
                        else
                        { // not text preceeding, just add this text as new plain text item
                            current_paragraph.add(new Text(the_line));
                        }
                    } // end something already in paragraph content list
                    else
                    {  // current paragraph is empty, just add this text item
                        current_paragraph.add(new Text(the_line));
                    }
                        
                    author_state = 21; // look for end of para
                    break;
                    
                } // case 21, waiting end of listitem
                case 100:
                {
                	// accumulate text until ENDPRE:
                	if (the_line.startsWith("ENDPRE:"))
                    {
                    	// done with this grouping
                    	dump_paragraph(current_paragraph);
                    	author_state = 0; // back to looking for tags
                    	current_text = null; // new one needed
                    	break; // out of case 100
                    }
                    // anything is OK
                    if ( (the_line.startsWith("EMPTY:")) ||
				 (the_line.startsWith("E:")) )
				{
					// special case, empty line
					current_text.text_items.add(new Text(""));
				}
				else
				{
					current_text.text_items.add(new Text(the_line));
				}
				break; // out of case 100
                } // end case 100 accumulating preformatted text
                case 101:
                {
                	// accumulate bold text until ENDPRE:
                	if (the_line.startsWith("ENDPRE:"))
                    {
                    	// done with this grouping
                    	dump_paragraph(current_paragraph);
                    	author_state = 0; // back to looking for tags
                    	current_text = null; // new one needed
                    	break; // out of case 101
                    }
                    // anything is OK
                    if ( (the_line.startsWith("EMPTY:")) ||
				 (the_line.startsWith("E:")) )
				{
					// special case, empty line
					current_text.text_items.add(new Bold(""));
				}
				else
				{
					current_text.text_items.add(new Bold(the_line));
				}
				break; // out of case 101
                } // end case 101 accumulating preformatted bold text
                case 102:
                {
                	// accumulate italics text until ENDPRE:
                	if (the_line.startsWith("ENDPRE:"))
                    {
                    	// done with this grouping
                    	dump_paragraph(current_paragraph);
                    	current_text = null; // new one needed
                    	author_state = 0; // back to looking for tags
                    	break; // out of case 102
                    }
                    // anything is OK
                    if ( (the_line.startsWith("EMPTY:")) ||
				 (the_line.startsWith("E:")) )
				{
					// special case, empty line
					current_text.text_items.add(new Italics(""));
				}
				else
				{
					current_text.text_items.add(new Italics(the_line));
				}
				break; // out of case 102
                } // end case 102 accumulating italics preformatted text
                default:
                {
                    throw new Exception("State error: " + author_state);
                }
            } // end state check
        } // end while loop on input
        /*
         * input complete, process last paragraph
         */
        dump_paragraph(current_paragraph);
    		
   	
    } // end process text content AUTHOR version





    public void heading_processing(String in, int level) throws Exception
    {
        // finish previous paragraph
   //     cur.write("<!-- process heading -->\n");
        dump_paragraph(current_paragraph);
        
          g_sink.createHeading(level,
                                 in.substring(3), // text content of heading
                                 null); // no anchor content
        // dump out the heading alone
//        Heading hh = new Heading(in.substring(3),level); // remove Hx:, dump with toString
     //   cur.write(hh.toString());
        // heading stands alone, so we start another paragraph on the next input
        author_state = 0; // look for start of pargraph
    }
    
    
    public void image_processing_old(String in) throws Exception
    {
        // finish previous paragraph
   //     cur.write("<!-- process image -->\n");
        dump_paragraph(current_paragraph);
        // dump out the image alone
	// OLD object, left for compatibility
        ImageReference ir = new ImageReference(in.substring(4)); // remove IM2:, dump with toString
                g_sink.processInlineImage(
			false, // local
          	            ir.the_thumb,
          	            ir.the_image,
          	            ir.the_pdfscale,
          	            ir.the_htmlwidth,
          	            ir.the_pdfuse,
          	            ir.the_caption,
          	            ir.the_anchor); // old format, anchor could be null
// no text content!
   //     cur.write(ir.toString());
        // image stands alone, so we start another paragraph on the next input
        author_state = 0; // look for start of pargraph
    }
    
	/*
	 * NEW image processing has the caption on the
	 * next line. So we change the state to 10 and
	 * process the next line as caption.
	 */
    public void image_processing_new(boolean rremote,String in) throws Exception
    {
        // finish previous paragraph
        dump_paragraph(current_paragraph);
	// first, save what object we have, and add caption on
	// next line
        current_image = new ImageReferenceNew(in,rremote); 
//System.out.println("Created: " + current_image); //debug
        // image stands alone, so we start another paragraph on the next input
        author_state = 10; // use next line for caption, then we go to state 0
    }
    
    public void index_processing(String in, ExactReference ref) throws Exception
    {
        // finish previous paragraph
        dump_paragraph(current_paragraph);
        // above starts a new paragraph
        IndexEntry ie  = new IndexEntry(in.substring(3),ref); // remove IN:, dump with toString
   //     cur.write(ie.toString());
   // debugging System.out.println(ie.toString());
        author_state = 0; // now looking for P: again
    }
    
    public void anchor_processing(String in) throws Exception
    {
        // finish previous paragraph
        dump_paragraph(current_paragraph);
        // above starts a new paragraph
        Anchor an = new  Anchor(in.substring(2)); // remove A:, dump with toString
	g_sink.setAnchor(an.content);
        author_state = 0; // now looking for P: again
    }
    

   
    private void dump_paragraph(List p) throws Exception
    {
        if (p.size() == 0)
        {
    //        cur.write("<!-- no paragraph content to print -->\n");
            /*
             * no reason to clear() this list, just return
             */
            return; // empty paragraph contents
        }
        Object oo = null;
        String st = "";
        oo = p.get(0);  // first item
        /*
         * see what first item is
         */
        if (!(oo instanceof Text))
        {
            if (oo instanceof ListMarker)
            {
		dump_list(p); // special processing
                return; // we are done with listitem grouping
            } // end if ListMarker all LI: items dumped
            if (oo instanceof PreformattedTextGroup)
            {
            	// let the object handle processing, there is only one!
            	PreformattedTextGroup ttgg = (PreformattedTextGroup)oo;
            	ttgg.performSink();
            	p.clear(); // clear out current work area, we are done
            	return; // done with this special grouping item
            }
            // first not text or list marker, so put in dummy
       //     cur.write("<content type=\"textfront\" />\n");
       // debugging System.out.println("<content type=\"textfront\" />\n");
                    g_sink.startText("",null); // empty textfront
            /*
             * now write out all the items, checking at the end
             *
             * WE KNOW that the first item is not Text, could be bold, etc
             * thus, all intermediate text is "middle" stuff
             * 
             * special end checking is done
             */
            for (int i = 0 ; i < p.size() ; i++)
            {
                oo = p.get(i); // next item
                if (oo instanceof Text)
                {
                    // text, test position
                    if (i == p.size() - 1)
                    { // last item
                        st = ((Text)oo).content;
             //           cur.write("<content type=\"textend\">" + st + "</content>\n"); // last text entry
             // debugging System.out.println("<content type=\"textend\">" + st + "</content>\n"); // last text entry
			    g_sink.endText(st); // last bit of text
                    }
                    else
                    { // intermediate text
                        st = ((Text)oo).content;
                //        cur.write("<content type=\"middle\">" + st + "</content>"); // intermediate text entry
                // debugging System.out.println("<content type=\"middle\">" + st + "</content>"); // intermediate text entry
				    g_sink.insertIntermediateText(st); // intermediate text entry
                    }
                    continue; // done with this item
                } // end if Text
                // not text, just use the toString()
          //      cur.write(oo.toString());
                  // debugging System.out.println("<!-- object -->");
          // debugging System.out.println(oo.toString());
			Item ii = (Item)oo;
			ii.performSink(); // do whatever is needed
                /*
                 * if the LAST item is not Text, we have to put in a dummy "textend"
                 */
                if (i == p.size() - 1)
                { // last item
                    if (!(oo instanceof Text))
                    {
                        // force dummy textend
         //               cur.write("<content type=\"textend\"/>\n"); // dummy
         // debugging System.out.println("<content type=\"textend\"/>\n"); // dummy
			    g_sink.endText(""); // dummy
                    }
                    // it is text, we are OK
                } // end if last item was just processed
            } // loop through all 
        } // end not Text object
        else
        {
            // first item IS text, check rest of array for other item types
            
            if (p.size() == 1)
            { // only one text item, we are done
                st = ((Text)oo).content;
        //        cur.write("<content>" +   st + "</content>\n");
        // debugging System.out.println("<content>" +   st + "</content>\n");
                g_sink.insertSimpleText(st);
            }
            else
            {
                //  one or more items follow the first text
                st = ((Text)oo).content;
        //        cur.write("<content type=\"textfront\">" + st + "</content>"); // first entry, others follow  
        // debugging System.out.println("<content type=\"textfront\">" + st + "</content>"); // first entry, others follow  
                    g_sink.startText(st,null); // start text, no anchor now
                for (int i = 1 ; i < p.size() ; i++)
                {
                    oo = p.get(i); // next item
                    if (oo instanceof Text)
                    {
                        // text, test position
                        if (i == p.size() - 1)
                        { // last item
                            st = ((Text)oo).content;
                 //           cur.write("<content type=\"textend\">" + st + "</content>\n"); // last text entry
                 // debugging System.out.println("<content type=\"textend\">" + st + "</content>\n"); // last text entry
			    g_sink.endText(st); // last text bit
                        }
                        else
                        { // intermediate text
                            st = ((Text)oo).content;
                //            cur.write("<content type=\"middle\">" + st + "</content>"); // intermediate text entry
                // debugging System.out.println("<content type=\"middle\">" + st + "</content>"); // intermediate text entry
				    g_sink.insertIntermediateText(st); // intermediate text entry
                        }
                        continue; // done with this item
                    }
                    // not text, use the toString()
            //        cur.write(oo.toString());
                  // debugging System.out.println("<!-- object -->");
            // debugging System.out.println(oo.toString());
			Item ii = (Item)oo;
			ii.performSink(); // do whatever is needed
                    /*
                     * if the LAST item is not Text, we have to put in a dummy "textend"
                     */
                    if (i == p.size() - 1)
                    { // last item
                        if (!(oo instanceof Text))
                        {
                            // force dummy textend
              //              cur.write("<content type=\"textend\"/>\n"); // dummy
              // debugging System.out.println("<content type=\"textend\"/>\n"); // dummy
			    g_sink.endText(""); // dummy
                        }
                        // it is text, we are OK
                    } // end if last item was just processed
                } // loop through all except first
            } // end if more than one
        } // end it is Text
     //  // cur.write(); // terminate line
        p.clear(); // CLEAR OUT the List that we just dumped!
    } // end dump paragraph

	public void dump_list(List p) throws Exception
	{
                /*
                 * special processing for bulleted lists
                 * the first item (ignored) is a special marker
                 * from here down, we are much like a typical paragraph,
                 * except we use listitemfront, etc
                 */
		Object oo = null;
		String st = "";

                if (p.size() == 1)
                {
                    // messy, the marker was the ONLY item, we are out of here
                    p.clear();
               //     cur.write("<!-- no listitem to print -->\n");
                    return;  // essentially an empty paragraph list item
                }
              // debugging System.out.println("<!-- dumping list -->\n");
                oo = p.get(1); // second item contains first actual text content
                if (!(oo instanceof Text))
                { // first real content not text, we make dummy
              // debugging System.out.println("<!-- listitemfront, empty -->\n");
                    g_sink.insertListItemStart("",null); // dummy, no anchor
                    /*
                     * now write out all the items, checking at the end
                     *
                     * WE KNOW that the first item is not Text, could be bold, etc
                     * thus, all intermediate text is "middle" stuff
                     * 
                     * special end checking is done
                     */
                    for (int i = 1 ; i < p.size() ; i++)
                    {
                        oo = p.get(i); // next item
                        if (oo instanceof Text)
                        {
                            // text, test position
                            if (i == p.size() - 1)
                            { // last item
                                st = ((Text)oo).content;
                            // debugging System.out.println("<!-- listitemend " + st + "-->\n"); // last text entry
                    g_sink.insertListItemEnd(st);
                            }
                            else
                            { // intermediate text
                                st = ((Text)oo).content;
                        // debugging System.out.println("<!-- middle " + st + "-->"); // intermediate text entry
				    g_sink.insertIntermediateText(st); // intermediate text entry
                            }
                            continue; // done with this item
                        } // end if Text
                        // not text, just use the toString()
                  // debugging System.out.println("<!-- object -->");
                  // debugging System.out.println(oo.toString());
			Item ii = (Item)oo;
			/*
			 * do whatever is needed
			 * if the method is invoked incorrectly,
			 * it will throw an exception
			 */
			ii.performSink(); // do whatever is needed
                        /*
                         * if the LAST item is not Text, we have to put in a dummy "listitemend"
                         */
                        if (i == p.size() - 1)
                        { // last item
                            if (!(oo instanceof Text))
                            {
                                // force dummy textend
                        // debugging System.out.println("<!-- listitemend, empty -->\n"); // dummy
			    g_sink.insertListItemEnd("");
                            }
                            // it is text, we are OK
                        } // end if last item was just processed
                    } // loop through all 
                } // end not Text object inside list item
                else
                {
                    // first item IS text, check rest of array for other item types
            
                    if (p.size() == 2)
                    { // only one text item, we are done
                        st = ((Text)oo).content;
                // debugging System.out.println("<!-- listitem " +   st + " -->\n");
                    g_sink.insertListItem(st,null); // no anchor
                        p.clear(); // CLEAR OUT the List that we just dumped!
                        return;
                    }
                    else
                    {
                        //  one or more items follow the first text
                        st = ((Text)oo).content;
                // debugging System.out.println("<!-- listitemfront " + st + " -->\n"); // first entry, others follow  
                    g_sink.insertListItemStart(st,null); // start, no anchor
                        for (int i = 2 ; i < p.size() ; i++)
                        {
                            oo = p.get(i); // next item
                            if (oo instanceof Text)
                            {
                                // text, test position
                                if (i == p.size() - 1)
                                { // last item
                                    st = ((Text)oo).content;
                        // debugging System.out.println("<!-- listitemend " + st + " -->\n"); // last text entry
                    g_sink.insertListItemEnd(st);
                                }
                                else
                                { // intermediate text
                                    st = ((Text)oo).content;
                       // debuggin System.out.println("<!-- middle " + st + " -->"); // intermediate text entry
				    g_sink.insertIntermediateText(st); // intermediate text entry
                                }
                                continue; // done with this item
                            }
                            // not text, use the toString()
                       //     cur.write(oo.toString());
                  // debugging System.out.println("<!-- object -->");
                       // debugging System.out.println(oo.toString());
			Item ii = (Item)oo;
			ii.performSink(); // do whatever is needed, could throw exception
                            /*
                             * if the LAST item is not Text, we have to put in a dummy "listitemend"
                             */
                            if (i == p.size() - 1)
                            { // last item
                                if (!(oo instanceof Text))
                                {
                                    // force dummy textend
			     // debugging System.out.println("<!-- listitemend, empty -->\n"); // dummy
				    g_sink.insertListItemEnd("");
                                }
                                // it is text, we are OK
                            } // end if last item was just processed
                        } // loop through all except first
                    } // end if more than one
                } // end first item in List is Text
                p.clear(); // CLEAR OUT the List that we just dumped!
	} // end dump list contents
    
    /*
     * put into a map, but spit out a warning if the key already exists.
     * we are storing all references in a List, so that every reference
     * will show up
     *
     * BUG when this method is invoked before the <xml>, the final
     * HTML output will not parse
     */
    public void putIntoMap(TreeMap the_map, Object the_key, Object the_content, String context)
    {
        if (the_map.containsKey(the_key))
        {
            // already have a List
            g_sink.noteReferenceDuplicate(
                the_key.toString(),
                the_content.toString(),
                context);
            ((List)the_map.get(the_key)).add(the_content); // add a new entry to the list
        }
        else
        {
            List contents = new ArrayList(); // one entry so far
            contents.add(the_content);
            the_map.put(the_key,contents);
        }
    } // end put into map
        
	public void dumpMap(Map the_map, String descriptive)
	{
		// nothing, use other code for debugging
	}
        
	public void dumpMaporigusefordebugging(Map the_map, String descriptive)
	{
		System.out.println(descriptive + ": " + the_map.size());
		if (the_map.size() > 0)
		{
			Iterator xx = the_map.keySet().iterator();
			while (xx.hasNext())
			{
				System.out.println("Item 0: " + xx.next());
				break; // only the first
			}

		}
		else
		{
			System.out.println("EMPTY!");
		}
	}
    public class Project
    {
        //public String title;
        public String special_content_name; // needed for creating object names
        //public String resources_name;
        //public String index_types; NOT USED
        public ArrayList all_sections;
        
        public Project(String c) throws Exception
        {
		String[] result = c.split("::");
		if (result.length < 1)
		{
			throw new Exception ("PROJECT: descriptor parse failed, need 1 item: " + c);
		}
		// NO LONGER USED title = result[0];
		// is NOT project-specific, but rather generic for the desired format
		special_content_name = result[0];// needed for object creation 
		// NOT USED resources_name = result[2];
		//index_types = result[3];
		all_sections = new ArrayList();
		/*
		 * load and instantiate special output object
		 */
		g_special_content_handler = (SpecialContentCreator) 
			Class.forName(
			special_content_name +  // from this line
			g_special_content_object_end // will be HTML, EPUBSIMPLE, etc  
			).newInstance();
		g_special_content_handler.setOptions(g_options);
		/* 
		 * load JSON file that is for this project ONLY
		 */
		g_special_content_handler.getMetaData(PROJECT_JSON);
		g_special_content_handler.modifyMetaData(); // make project-specific
                        // NOTE that util_object is not used, but appears in the PROJECT: headline (will be used by BookUtils for json, not yet written)

        } // end Project constructor
    
        public String toStringold()
        {
            StringBuffer buf = new StringBuffer();
//            buf.append("<!-- " + title.replace("::","(double)") + " -->\n");
            buf.append("<content type=\"title_object\" object=\"" +
                    special_content_name + "\"/>\n");
/*
            buf.append("<content type=\"util_object\" object=\"" +
                    resources_name + "\"/>\n");
*/
            /* buf.append("<content type=\"index_setting\" values=\"" +
                    index_types + "\"/>\n"); 
		*/
            return buf.toString();
        } // end tostringNOT USED

        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append("Project Object: \n");
            buf.append("  Special Content Object Name Base: " +
                    special_content_name + "\n");
//            buf.append("  Resources Name: " +
  //                  resources_name + "\n");
            /* buf.append("  Index Setting: " +
                    index_types + "\n"); 
		*/
            return buf.toString();
        }        // end to string
        
        public void dumpSections()
        {
        	// for debugging, show all sections
        	Iterator ii = all_sections.iterator();
        	while (ii.hasNext())
        	{
        		// debugging System.out.println("---------------new section ----------------\n");
        		List innerx = (List)ii.next();
        		int leng = innerx.size(); // only dump some
        		if (leng > 5)
        		{
        			leng = 5; // force view only 5 lines (can be altered to see MORE)
        		}
        		for (int inner = 0 ; inner < leng ; inner++)
        		{
        			// debugging System.out.println(innerx.get(inner));
        		} // end loop on all section content
        	} // end loop on all sections
        } // end dump sections of book
    } // end Project object
    
      /*
     * generic item from AUTHOR input
     */
    public abstract class Item
    {
        public String content;
        public int the_type;
        int TEXT = 0; // text, location dependent
        int BOLD = 1; // bolded text
        int ITALIC = 2; // italicized text
        int ANCHOR = 3; // anchor to be referred to elsewhere
        int INDEX = 4; // index item to be placed in general index
        int SEE_ALSO = 5; // points to an anchor
        int IMAGE = 6; 
        int HEADING = 7;
        int LINK = 8;
        int GENERALSECTION = 9; // a kind of section
	int PREFORMATTED = 10;
	int STATESECTION = 11; // a kind of a section
	int ABBREVIATIONSECTION = 12; // a kind of a section
	int APPENDIXSECTION = 13; // a kind of a section
	int CITY = 14; // object for city
	int FACILITY = 15; // object for facility
        
        public Item(String c)
        {
            content = c;
            the_type = TEXT; // default, knowing nothing else
        }
        public Item()
        {
            // not to be used, but children will invoke
            content = null;
            the_type = TEXT;
        }
	abstract void performSink() throws Exception;
    } // end base class item
    
	/*
	 * implements Comparable so they can be sorted by narrative
	 */
    public class Anchor extends Item  implements Comparable
    {
	public String list_name;
	public String narrative;
        public Anchor(String c)throws Exception
        {
            String[] result = c.split("::");
            content = result[0];
		list_name = null; // no LIST so far
		narrative = null; // no narrative so far
            the_type = ANCHOR;
            if (result.length == 1)
		{
			return; // no LIST membership
		}
            if (result.length == 3)
		{
			list_name = result[1]; // GROUP NAME
			narrative = result[2]; // narrative for link
		}
		else
		{
			throw new Exception ("ANCHOR descriptor parse failed, need either 1 or 3 items: " + c);
		}
        } // end constructor

       public int compareTo(Object ob)
        {
            int result = 0;

            if (ob instanceof Anchor)
            {
                Anchor them = (Anchor)ob;  // cast it
		// sort by narrative first
		result = this.narrative.compareTo(them.narrative);
		if (result != 0)
		{
			return result; // narratives not equal, ok
		}
		// narratives same, use anchor name, ALWAYS UNIQUE
		return this.content.compareTo(them.content);
		} // end if right type
		else
		{
			return -1; // wrong object, make a mess
		}
	} // end compareto
     public boolean equals(Object o)
        {
            boolean result = false;

            if (o instanceof Anchor)
            {
                Anchor them = (Anchor)o;  // cast it
		result = this.narrative.equals(them.narrative);
		if (result)
		{
			// same use anchor name ALWAYS UNIQUE
			return this.content.equals(them.content);
		}
		else
		{
			// different
			return result;
		}
		} // if right object
		return false; // wrong object, never matches
	} // end equals
        
        public String toString()
        {
            //return "<content type=\"anchor\" name=\"" + content + "\" />\n";
		StringBuffer sb = new StringBuffer();
            sb.append("<!-- anchor, name=" + content); 
		if (list_name != null)
		{
			sb.append(", list_name=" + list_name +
			    ", narrative=" + narrative);
		}
		return sb.toString() + " -->";
        } // end tostring

	void performSink() throws Exception
	{
		throw new Exception("performSink for Anchor not implemented");
	}
    } // end Anchor
    
        public class ImageReference extends Item
    {
        /*
         * 7 or 8 entries
         *
         * 1) main image filename
         * 2) thumbnail image filename
         * 3) pdfscale
         * 4) htmlwidth
         * 5) pdfuse
         * 6) caption ISSUE HERE BECAUSE we have multi-line captions, using
         *     the :: as a marker, grump! Do we require AUTHOR to use something
         *     else, and we convert them to :: before creating the XML tag?
         *     seems best to use doubles, so use ?? which is not commonly seen (?!)
         *     ALSO either escape or remove double quotes from caption
         * 7) anchor (optional)
         */
        String the_caption;
        String the_image;
        String the_thumb;
        String the_pdfscale;
        String the_htmlwidth;
        String the_pdfuse;
        String the_anchor = null;
        
        public ImageReference(String c) throws Exception
        {
            /*
             * PARSE and ensure that there is a destination and text
             */
            String[] result = c.split("::");
            if (result.length < 6)
            {
                throw new Exception ("Image Reference parse failed, missing 6 items: " + c);
            }
            the_image = result[0];
            the_thumb = result[1];
            the_pdfscale = result[2];
            the_htmlwidth = result[3];
            the_pdfuse = result[4];
            the_caption = result[5];
            if (result.length >= 7)
            {
                the_anchor = result[6]; // otherwise null to mean no anchor
            }
            the_type = IMAGE;
        }
        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append("<content type=\"image\" full=\"");
            buf.append(the_image);
            buf.append("\" thumb=\"");
            buf.append(the_thumb);
            buf.append("\" pdfscale=\"");
            buf.append(the_pdfscale);
            buf.append("\" htmlwidth=\"");
            buf.append(the_htmlwidth);
            buf.append("\" pdfuse=\"");
            buf.append(the_pdfuse);
            String fixed_caption = the_caption.replace("??","::"); // caption markers changed from question to colon
            
            buf.append("\" caption=\"");
            buf.append(fixed_caption.replace("\"","'") + "\"");  // change double quotes to single and add a finishing quote
            // if anchor include it
            if (the_anchor != null)
            {
                buf.append(" anchor=\"");
                buf.append(the_anchor.trim() + "\""); // include ending quote and dont allow any leading or trailing blanks
            }
            return buf.toString() + "/>\n"; // end end of tag
        } // end tostring
	void performSink() throws Exception
	{
		throw new Exception("performSink for ImageReference not implemented");
	}
    } // end ImageReference (old)
    
        public class ListMarker
    {
        /* 
        * dummy item used to tell dumper that the following content are
         * parts of a bulleted list
         */
	void performSink() throws Exception
	{
		throw new Exception("performSink for ListMarker not implemented");
	}
    }

    
    public class ImageReferenceNew extends Item
    {
        /*
         * supports remote images (used by some sinks)
         * 
         * We support 4 different arrangements (yes, with the same
         * text entry. Kluge? yes)
         *
         * 6 entries are the ORIGINAL for the facilities project
         * this arrangement is much too complex for ordinary use
         * (and many fields were NOT used) these are ALWAYS local
         *
         * 1) main image filename
         * 2) thumbnail image filename
         * 3) pdfscale
         * 4) htmlwidth
         * 5) pdfuse
         * 6) anchor
         *
         * 3 entries work for shortened LOCAL
         *
         * 1) main image filename
         * 2) thumb image filename
         * 3) anchor
         *
         * 2 entries work for shortened LOCAL
         *
         * 1) main image filename AND thumb image filename (same!)
         * 2) anchor
         *
         * 3 entries work for REMOTE
         *
         * 1) main image url (uses main filename)
         * 2) alt text (uses thumb field)
         * 3) anchor
         *
         *
	 * FOR ALL THESE TYPES, the CAPTION is considered to be the 
	 * next input line and is added after instantiation
         */
        String the_caption;
        String the_image;
        String the_thumb;
        String the_pdfscale;
        String the_htmlwidth;
        String the_pdfuse;
        String the_anchor = null;
	boolean remote = false; // only remote when RIM: used
        
        public ImageReferenceNew(String c) throws Exception
        {
            /*
             * PARSE and ensure that there is a destination and text
             */
            String[] result = c.split("::");
		/*
		 * check for various entry count, including the SHORTENED system
		 */
            switch (result.length)
		{
			case 2:
			{
			    the_image = result[0];
			    the_thumb = result[0]; // thumb SAME as primary
				the_anchor = result[1]; // yes, required
				break;
			}
			case 3:
			{
			    the_image = result[0];
			    the_thumb = result[1];
				the_anchor = result[2]; // yes, required
				break;
			}
			case 6:
			{
				// the old fashioned LONG one
			    the_image = result[0];
			    the_thumb = result[1];
			    the_pdfscale = result[2];
			    the_htmlwidth = result[3];
			    the_pdfuse = result[4];
			    the_caption = "NULL"; // fill in after instantiation
				the_anchor = result[5]; // required
			    the_type = IMAGE;
				break; // done
			} // end long one
			default:
			{
				// cannot use this count
				throw new Exception("Image marker (IM: or RIM:) fields are not 2, 3, or 6 items in length\n  Count Seen: " + result.length + "\n   String: " + c);
			}
		} // end switch on count of items
		remote = false; // default, someone else will override
        } // usual constructor (one string)
        
        public ImageReferenceNew(String c,boolean rremote) throws Exception
        {
            /*
             * let the other method PARSE
		*/
		this(c);
		remote = rremote; // override anything set by normal parse
	}
		
	public void setRemote(boolean x)
	{
		remote = x;
	}
		
	public void setCaption(String x)
	{
		the_caption = x;
	}

        public String toString()
        {
		
            StringBuffer buf = new StringBuffer();
	if(remote)
	{
            buf.append("<content type=\"image\" src=\"");
            buf.append(the_image);
            buf.append("\" alt=\"");
            buf.append(the_thumb);
                buf.append(" anchor=\"");
                buf.append(the_anchor.trim() + "\""); // include ending quote and dont allow any leading or trailing blanks
	}
	else
	{
		// local image
            buf.append("<content type=\"image\" full=\"");
            buf.append(the_image);
            buf.append("\" thumb=\"");
            buf.append(the_thumb);
            buf.append("\" pdfscale=\"");
            buf.append(the_pdfscale);
            buf.append("\" htmlwidth=\"");
            buf.append(the_htmlwidth);
            buf.append("\" pdfuse=\"");
            buf.append(the_pdfuse);
            String fixed_caption = the_caption.replace("??","::"); // caption markers changed from question to colon
            
            buf.append("\" caption=\"");
            buf.append(fixed_caption.replace("\"","'") + "\"");  // change double quotes to single and add a finishing quote
            // if anchor include it
            if (the_anchor != null)
            {
                buf.append(" anchor=\"");
                buf.append(the_anchor.trim() + "\""); // include ending quote and dont allow any leading or trailing blanks
            }
	} // end local
            return buf.toString() + "/>\n"; // end end of tag
        } // end tostring
	void performSink() throws Exception
	{
		throw new Exception("performSink for ImageReferenceNew not implemented");
	}
    } // end ImageReferenceNew
    
	/*
	 * base class
	 */
        public abstract class BaseSection extends Item
    {
// content is a base name, usually the filename (HTML)
        String primary;
        String secondary;
        
	abstract void performSink() throws Exception;

	// override, so that children must implement

	public String toString()
	{
		// invoke child ONLY
		return childToString();
	}
	// children must implement
	abstract String childToString();
    } // end BaseSection base class
    
    /*
     * State is a kinda section, but used for
     * facilities book
     */
    public class StateSection extends BaseSection
    {
            // primary is the_short_title;
            // secondary is the_title;

       public StateSection(String c) throws Exception
        {
            /*
             * PARSE and ensure that there are enough entries
             */
            String[] result = c.split("::");
            if (result.length < 2)
            {
                throw new Exception ("State parse failed, missing two (minimum) items: " + c + "\ncount: " + result.length);
            }
            content = result[0]; // state ABBREVIATION is used as name/link as in references and html file output, etc
            primary = result[1]; // long title of state 
		secondary = null; // if missing 
		if (result.length > 2)
		{
		    secondary = result[2]; // short title of state
		}
            the_type = STATESECTION;
        }
    	/*
    	 * a bit of a dummy entry, but may help debug
    	 */
       public String childToString()
        {
            return "<state type=\"state\" abbrev=\""  + 
                content + "\" title=\"" +
                primary + "\" short_title=\"" +
                secondary +
                "\">";
        }

	void performSink() throws Exception
	{
		throw new Exception("performSink for StateSection not implemented");
	}    
} // end StateSection
    
    /*
     * AppendixSection is a kinda section, but used for
     * facilities book
     */
    public class AppendixSection extends BaseSection
    {

       public AppendixSection(String c) throws Exception
        {
            /*
             * PARSE and ensure that there are enough entries
             */
            String[] result = c.split("::");
            if (result.length < 3)
            {
                throw new Exception ("AppendixSection parse failed, missing three items: " + c);
            }
            content = result[0]; // appendix filename is used as name/link as in references and html file output, etc
            secondary = result[2]; // short title of appendix
            primary = result[1]; // long title of appendix 
            the_type = APPENDIXSECTION;
        }
    	/*
    	 * a bit of a dummy entry, but may help debug
    	 */
       public String childToString()
        {
            return "<section type=\"appendix\" abbrev=\""  + 
                content + "\" title=\"" +
                primary + "\" short_title=\"" +
                secondary +
                "\">";
        }

	void performSink() throws Exception
	{
		throw new Exception("performSink for AppendixSection not implemented");
	}    
} // end AppendixSection
    
    /*
     * AbbreviationSection is a kinda section, but used for
     * facilities book
     */
    public class AbbreviationSection extends BaseSection
    {

       public AbbreviationSection(String c) throws Exception
        {
            /*
             * PARSE and ensure that there are enough entries
             */
            String[] result = c.split("::");
            if (result.length < 3)
            {
                throw new Exception ("AbbreviationSection parse failed, missing three items: " + c);
            }
            content = result[0]; // section name ABBREVIATION is used as name/link as in references and html file output, etc
            secondary = result[2]; // short title of abbreviation section
            primary = result[1]; // long title of abbreviation section 
            the_type = ABBREVIATIONSECTION;
        }
    	/*
    	 * a bit of a dummy entry, but may help debug
    	 */
       public String childToString()
        {
            return "<section type=\"abbreviation\" abbrev=\""  + 
                content + "\" title=\"" +
                primary + "\" short_title=\"" +
                secondary +
                "\">";
        }

	void performSink() throws Exception
	{
		throw new Exception("performSink for AbbreviationSection not implemented");
	}    
} // end AbbreviationSection
    
    /*
     * GeneralSection is used everywhere
     */
    public class GeneralSection extends BaseSection
    {
	public AuxiliaryMetadata meta = null;

       public GeneralSection(String c) throws Exception
        {
            /*
             * PARSE and ensure that there are enough entries
             */
            String[] result = c.split("::");
            if (result.length < 3)
            {
                throw new Exception ("GeneralSection parse failed, missing three items: " + c);
            }
            content = result[0]; // section name is used as name/link as in references and html file output, etc
            primary = result[1]; // long title of section 
		/*
		 * short title (3rd item) is NOW overloaded with
		 * various other uses, such as transmitting additional
		 * metadata to the sinks (HTML comes to mind)
		 * 
		 * identification of aux metadata will be a two-letter 
		 * prefix on the string. Types of these will be 
		 * dealt with for now by hard coding. An object factory
		 * for auxiliary data should be designed.
		 */
            secondary = result[2]; // short title of section
		if (secondary.startsWith("PD:"))
		{
			meta = new AuxiliaryMetadata(secondary.substring(3),
			    primary // long title becomes title string
			); // bypass prefix
		}
            the_type = GENERALSECTION;
        }
    	/*
    	 * a bit of a dummy entry, but may help debug
    	 */
       public String childToString()
        {
            return "<section type=\"general\" abbrev=\""  + 
                content + "\" title=\"" +
                primary + "\" short_title=\"" +
                secondary +
                "\">";
        }

	void performSink() throws Exception
	{
		throw new Exception("performSink for GeneralSection not implemented");
	}    
} // end GeneralSection
    
    /*
     * City is used in facility book
     */
    public class City extends Item
    {
	public String city_name;
	public String state_name;

       public City(String c) throws Exception
        {
            /*
             * PARSE and ensure that there are enough entries
		* example:  CITY:AL::Alabama::Anniston
             */
            String[] result = c.split("::");
            if (result.length < 3)
            {
                throw new Exception ("City parse failed, missing three items: " + c);
            }
            content = result[0]; // state name is used as name/link as in references and html file output, etc
            state_name = result[1]; // state name
            city_name = result[2]; // city name
            the_type = CITY;
        }
    	/*
    	 * a bit of a dummy entry, but may help debug
    	 */
       public String toString()
        {
            return "<city  abbrev=\""  + 
                content + "\" city_name=\"" +
                city_name + "\" state_name=\"" +
                state_name +
                "\">";
        }

	void performSink() throws Exception
	{
		/*
		 * SIDE EFFECT!
		 *
		 * when we process a City, we also put that city
		 * into the index maps, both global and for current state
		 */
			dumpMap(g_city_index_map,"g_city_index_map: ");
                putIntoMap(g_city_index_map,
                           city_name,
                           new ExactReference(content,state_name,city_name),
"City object all cities"); // appears in master indexes
			dumpMap(g_current_city_index_map,"g_current_city_index_map:");
                putIntoMap(g_current_city_index_map,
                           city_name,
                           new ExactReference(content,state_name,city_name),
"City object current_cities"); // appears in state page
                g_sink.startACity(content,
                                  state_name,
                                  city_name,
                                  null, // no index or anchor
                                  null);
	}    
} // end City
    
    /*
     * Facility is used in facility book
     */
    public class Facility extends Item
    {
	public String city_name;
	public String state_name;
	public String facility_name;
	public boolean nopost;
	public String	    source;
	public String open;
	public String personnel;
	public String hosp;
	public String close;

       public Facility(String c) throws Exception
        {
            /*
             * PARSE and ensure that there are enough entries
	* example: FAC:AL::Alabama::Anniston::Artillery Machine Gun Camp::::::::::::false
             */
            String[] result = c.split("::");
            if (result.length < 10)
            {
                throw new Exception ("Facility parse failed, missing ten items: " + c);
            }
            content = result[0]; // state name is used as name/link as in references and html file output, etc
            state_name = result[1]; // state name
            city_name = result[2]; // city name
            facility_name = result[3]; // facility name
		// any of following can be empty, which implies null
	    source = BookUtils.returnContentsOrNull(result[4]); 
	    open = BookUtils.returnContentsOrNull(result[5]); 
	    personnel = BookUtils.returnContentsOrNull(result[6]); 
	    hosp = BookUtils.returnContentsOrNull(result[7]); 
	    close = BookUtils.returnContentsOrNull(result[8]); 

		// this item is boolean
                String nopost_string = result[9];
                nopost = false;
                if (nopost_string == null)
                {
                    // not mentioned, then there is postal history
                }
                else
                {
                    if (nopost_string.equalsIgnoreCase("true"))
                    {
                        nopost = true;
                    }
                    // if someother value, we assume false
                }
            the_type = FACILITY;
        }
    	/*
    	 * a bit of a dummy entry, but may help debug
    	 */
       public String toString()
        {
            return "<facility  abbrev=\""  + 
                content + "\" facility_name=\"" +
                facility_name + "\" city_name=\"" +
                city_name + "\" state_name=\"" +
                state_name +
                "\">";
        }

	void performSink() throws Exception
	{
		/*
		 * SIDE EFFECT!
		 *
		 * when we process a Facility, we also put that facility
		 * into the index maps, both global and for current state
		 */
			dumpMap(g_fac_index_map,"g_fac_index_map: ");
     		putIntoMap(g_fac_index_map,new FacilityReference(facility_name),
                           new ExactReference(content,state_name,city_name,nopost), // nopost flag is HERE
                           "Facility object all facilities");  // global index
			dumpMap(g_current_fac_index_map,"g_current_fac_index_map:");
                putIntoMap(g_current_fac_index_map,
                           new FacilityReference(facility_name),   //,nopost),
                           new ExactReference(content,state_name,city_name),
                           "Facility object state facilities");  // state-only index
           			
                g_sink.startAFacility(content,
                                  state_name,
                                  city_name,
                                  facility_name,
                    source, // any of these may be null
                    open,
                    personnel,
                    hosp,
                    close,
                    nopost,
                                  null, // no index or anchor
                                  null);
	} // end performsink method   
} // end Facility
    
    
    public class Text extends Item
    {
        public Text(String c)
        {
            content = c;
            the_type = TEXT;
        }
	void performSink() throws Exception
	{
		throw new Exception("performSink for Text not implemented");
	}
    } // end text
    
    public class IndexEntry extends Item
    {
	public List index_items; // split into IndexRef items

        // must receive the ExactReference location
        public IndexEntry(String c, ExactReference ref) throws Exception
        {
            // index entry item can contain multiples, separated by : 
            content = c;
		// list will contain IndexRef items
            index_items  = breakOutIndexEntry(content,
			ref.state_abbrev,
			ref.state_name,
			ref.city_name,
                                                   "process an index entry IN:xxxx"); 
	    g_sink.setIndexLocation(index_items); // should not pass a null
            the_type = INDEX;
//		System.out.println("<!-- processed index: " + index_items
//		+ " -->");
        }
        public String toString()
        {
            //return "<content type=\"index\" keyword=\"" + content + "\" />\n";
		Iterator inner = index_items.iterator();
		StringBuffer sb = new StringBuffer();
		while (inner.hasNext())
		{
            		sb.append("<!-- index, keyword=" + inner.next() + " -->\n");
		}
            //return "<!-- index, keyword=" + content + " -->\n";
		return sb.toString();
        }
	void performSink() throws Exception
	{
		throw new Exception("performSink for IndexEntry not implemented");
	}
    } // end indexentry

    public class Bold extends Item
    {
        // looks the same otherwise
        public Bold(String c)
        {
            content = c;
            the_type = BOLD;
        }
        public String toString()
        {
            //return "<content type=\"emphasis\">" + content + "</content>\n";
            return "<!-- bold=" + content + " -->\n";
        }
	void performSink() throws Exception
	{
	    g_sink.insertEmphasizedText(content);
	}
    } // end bold

    public class Italics extends Item
    {
        // looks the same otherwise
        public Italics(String c)
        {
            content = c;
            the_type = ITALIC;
        }
        public String toString()
        {
            //return "<content type=\"cite\">" + content + "</content>\n";
            return "<!-- cite=" + content + " -->\n";
        }
	void performSink() throws Exception
	{
	    g_sink.insertCitedText(content);
	}
    } // end italics

    public class Heading extends Item
    {
        int the_level = 3;
        
        // looks the same otherwise
        public Heading(String c, int level)
        {
            content = c;
            the_level = level;
            the_type = HEADING;
        }
        public String toString()
        {
            return "<content type=\"heading" + the_level + "\">" + content + "</content>\n";
        }
	void performSink() throws Exception
	{
		throw new Exception("performSink for Heading not implemented");
	}
    } // end heading

    public class SeeAlso extends Item
    {
        /*
         * two entries, first is destination, which is an anchor
         * second is text to be inside "see also"
         */
        String the_text;
        
        public SeeAlso(String c) throws Exception
        {
            /*
             * PARSE and ensure that there is a destination and text
             */
            String[] result = c.split("::");
            if (result.length < 2)
            {
                throw new Exception ("See Also parse failed, missing two items: " + c);
            }
            content = result[0];
            the_text = result[1]; // IGNORING ANY ADDITIONAL INFORMATION!
            the_type = SEE_ALSO;
        }
        public String toString()
        {
            //return "<content type=\"see_also\" link=\""  + content +  "\">"  + the_text + "</content>";
            return "<!-- see also, destination: " + content +  ", text: " + the_text + " -->";
        }
	void performSink() throws Exception
	{
		/*
		 * content is link, which is the
		 * anchor. it is in the lookup
		 *
		 * SPECIAL CASE
		 * we need to allow references to the "general index".
		 * this destination position is not entered
		 * in the early passes, it is made by the SINK
		 * code when the index(s) are created. We make
		 * THE ASSUMPTION THAT THERE IS ALWAYS a General
		 * Index with the name "_general_index"
		 * 
		 * Above is no longer really true. All indexes are OPTIONAL 
		 * we will leave this code in place for now...
		 */
		boolean valid_destination = false;
		if (content.equals("_general_index"))
		{
			valid_destination = true;
		}
		else
		{
			// not special case, make sure this is a known destination
			if (g_anchor_lookup_map.containsKey(content))
			{
				valid_destination = true;
			}
		} // end not special case, requires lookup
		if (valid_destination)
		{
			String see_also_filename = (String)
				g_anchor_lookup_map.get(content);
                    g_sink.insertSeeAlso(
                        see_also_filename, // just looked it up
                        content, // link
                        the_text, // text that triggers link
                        null, // see_also_middle,  null
                        null // see_also_final, null
				); 
		} // end valid destination
		else
		{
			// not valid and not special case, blow up
			throw new Exception("See Also could not find destination: " + content);
		}
	}
    } // end see also

    public class Link extends Item
    {
        /*
         * two entries, first is destination, which is a URL
         * second is text to be inside "linkage"
         *
         * IT SEEMS that URL's can have double colons, so we will
         * force the user to specify ?? and we convert
         */
        String the_text;
        
        public Link(String c) throws Exception
        {
            /*
             * PARSE and ensure that there is a destination and text
             */
            String[] result = c.split("::");
            if (result.length < 2)
            {
                throw new Exception ("Link parse failed, missing two items: " + c);
            }
            content = result[0];
            the_text = result[1]; // IGNORING ANY ADDITIONAL INFORMATION!
            the_type = LINK;
        }
        public String toString()
        {
            /* return "<content type=\"link\" href=\""  + 
                content.replace("??","::") +   // URL changed from question to colon
                "\">"  + 
                the_text.replace("??","::") +   // text changed from question to colon
"</content>";
*/
            return "<!-- link href=" + 
                content.replace("??","::") +   // URL changed from question to colon
                ", text="  + 
                the_text.replace("??","::") +   // text changed from question to colon
"-->";
        }
	void performSink() throws Exception
	{
		g_sink.insertLink(content,the_text);
	}
    } // end Link

    
	/*
	 * a preformatted group contains a list
	 * of various text items. They may be
	 * plain text, bold entries, etc, etc.
	 *
	 * when processed, each item in the list
	 * becomes a separate line in the sink
	 * processing, rather than having all the
	 * text run together, as would be in a normal
	 * paragraph flow. This is a bit like the <pre>
	 * tag in HTML. Users can then create text
	 * that follows THEIR line break desires, rather
	 * than having the formatter do it
	 */
    public class PreformattedTextGroup extends Item
    {
	public List text_items; // contains Text, Bold, etc

	/*
	 * constructor gets the first item
	 */
        public PreformattedTextGroup(Item start) throws Exception
        {
		text_items = new ArrayList();
		text_items.add(start);
		    the_type = PREFORMATTED;
        }

	/*
	 * simple constructor
	 */
        public PreformattedTextGroup() throws Exception
        {
		text_items = new ArrayList(); // initially empty
		    the_type = PREFORMATTED;
        }
        public String toString()
        {
		Iterator inner = text_items.iterator();
		StringBuffer sb = new StringBuffer();
		while (inner.hasNext())
		{
            		sb.append("<!-- preformatted text, content=" + inner.next() + " -->\n");
		}
		return sb.toString();
        }
	/*
	 * the Sink will be what we want, when time
	 * to dump this out
	 */
	void performSink() throws Exception
	{
		// dump each item, which will be Text, Bold or Italics, as intermediate text
		Object oo = null;
		String st = null;
		/*
		 * we need to end each entity with a break, BUT not at the end
		 */
		 int the_length = text_items.size();
		 int the_lengthm1 = the_length - 1;
//		Iterator ii = text_items.iterator();
		// EVERYONE starts with textfront, and ends with textend, because every break is an intermediate break
          g_sink.startPREText("",null); // empty textfront for PREFORMATTED
          for (int inner = 0 ; inner < the_length ; inner++)
		// while (ii.hasNext())
		{ 
//		   oo = ii.next();
			oo = text_items.get(inner);
		   if (oo instanceof Text)
		   {
			   st = ((Text)oo).content; // content of simpletext
		  //      System.out.println("<content>" +   st + "</content>\n");
               // g_sink.insertSimpleText(st);
                  g_sink.insertIntermediateText(st); // text first, then break
                  if (inner != the_lengthm1)
                  {
                 	 g_sink.insertIntermediateBreak(); // any but last
                 }
		   } // end Text
		   else
		   {
			   if (oo instanceof Bold)
			   {
				   st = ((Bold)oo).content; // content of boldtext
//                    g_sink.startText("",null); // empty textfront
			
				    g_sink.insertEmphasizedText(st);
                  if (inner != the_lengthm1)
                  {
                 	 g_sink.insertIntermediateBreak(); // any but last
                 }
		 	  } // end Bold
		 	  else
		 	  {
				 if (oo instanceof Italics)
				 {
					   st = ((Italics)oo).content; // content of italicstext
              //      g_sink.startText("",null); // empty textfront
					    g_sink.insertCitedText(st);
                  if (inner != the_lengthm1)
                  {
                 	 g_sink.insertIntermediateBreak(); // any but last
                 }
//				    g_sink.endText("");
		 	  	} // end Italics
		 	  	else
		 	  	{
		 	  		// who is this?
		            throw new Exception("performSink for PreformattedTextGroup not yet implemented for: " +
		            oo.toString());
		 	  	} // end not anything we understand
		 	  } // end not bold or plain
		   } // end else not plain text
		} // end loop on all items within preformatted group
		
		// ALL CONTENT ends with endtext, as everything was intermediate text or breaks
		g_sink.endText("");

//		throw new Exception("performSink for PreformattedTextGroup not yet implemented");
	} // end perform sink for preformatted group
    } // end preformattedtextgroup
        
} // end generic book create
