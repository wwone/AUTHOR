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
 * modified 10/9/2019
 * 
 * add stubs for TABLES 
 * 
 * remove dependency on old XMLUtils 
 *
 * 5/26/18, add auxiliary meta information
 *
 * 4/14/17, several bugs fixed
 *
 * another bug fixed where "preceeding" text for
 * boilerplate was not positioned in a preceeding
 * manner!
 * 
 * 
 * The code here is the sink for creating
 * an "Author" file. This is a simple text
 * file that an author can use to create documents
 * in this system. "Author" files are simple text.
 * They SHOULD be the current input to the book
 * creation program. This Sink is provided to help
 * port older XML books to this newer system.
 * *) Version 1 -- first cut, trying to create
 *    the "Author" language
 *
 * *) Author language is much more mature 3/23/2017
 *    will try to convert Great Lakes book into Author
 *    correctly, and then try newest BookCreate code
 *    to read it back and re-create the original XML
 *    for this book.
 *
 *
 */
public class AuthorSink extends GenericSink
{
    /*
     * globals 
     */

    public String[] BOILERPLATE = BookUtils.AUTHOR_BOILERPLATE; 

    
    public String g_file_extension; // will be .txt
    public String g_file_extensionq = ""; // NOT USED

    public ArrayList g_appendixes = null; // links and names
    
    public String g_current_state; // used for postfix processing of a state
    
    public String g_tag_terminator;  // needed to finish "some" XML:FO tags
    
    public   int g_index_flags = 0;  // or'ed together
    public final static int INDEX_STATES  = 1;
    public final static int INDEX_CITIES  = 2;
    public final static int INDEX_FACILITIES  = 4;
    public final static int INDEX_GENERAL  = 8;
    public final static int INDEX_NO_POSTAL_HISTORY  = 16;
    public final static int INDEX_ABBREVIATIONS  = 32;

    public static int image_index  = 1;
    
    
    public AuthorSink() // NO PARAM
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
    public AuthorSink(GenericSource sou)
    {   
    //    super(sou);
    }
    
// can it work?    public void startDocument(TitleCreator tit) throws Exception
 public void   startDocument(SpecialContentCreator tit) throws Exception
    {
        /*
         * MAIN text will be written using a global
         */
        g_pr =  new PrintWriter(new File("author_content" + g_file_extension)); // write to this file only
        g_state_pr = g_pr; // used by some code
    
        /*
         * invoke the object that makes the desired title matter, including:
         * 1) cover
         * 2) title page
         * 3) front matter
         * 4) preface for PDF/paper users
         */
	//System.out.println("Author title creator object is: " + tit);
        if (tit != null)
        {
// was            tit.createTitlePage(g_pr);
            tit.createTitlePage(g_pr,null);
		g_pr.println("!:book line here");
        }
        else
        {
            // hmm, no object for making title page, put in dummy info
			g_pr.println("MISSING TITLE PAGE/FRONT MATTER");
        }
       } // end start doc
    
// public void   startDocument(SpecialContentCreator tit)
 //{ // HERE HERE, this is the new interface used, must conform in order to work
 //}
 
    public void endDocument(
        TreeMap  state_index_map,
        TreeMap  abbrev_map,
        TreeMap  city_index_map,
        TreeMap  fac_index_map,
        TreeMap  general_index_map,
        TitleCreator tit) throws Exception
    {
	g_pr.close();
        }
        
   public void     endDocument(TreeMap a,TreeMap b,TreeMap c,
   TreeMap d,TreeMap e) 
 {
	g_pr.close();
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
	g_pr.println("!:STARTING STATES");
        // PrintWriter (global) has already been set up
    } // start states grouping
    
    public void endStates() 
    {
    } // end states grouping
    
    public void startMainSection(TitleCreator tit,
				String short_title) throws Exception
    {
		// we just start working with main, g_pr.println("START MAIN: " + short_title);
    }  // end start up main section

public void    
    startMainSection(String x)
    {
    }
    
    public void endMainSection(TitleCreator tit)
    {
    } // end terminate main section
    
    public void endMainSection()
    {
    } // end terminate main section

    public void startAbbreviationSection(String app_name,
                                         String app_title,
					TitleCreator tit,
					String short_title) throws Exception
    {
		g_pr.println("!:START ABBREVIATIONS");
    }
    
public void    startAbbreviationSection(String a,String b,String c) 
    {
//		g_pr.println("!:START ABBREVIATIONS");
		startAppendixSection(a,b,c); // for now, treat like any other appendix
    }
    
    public void endAbbreviationSection(TreeMap abbrev_map,
                                       TreeMap state_index_map,
                                       TreeMap city_index_map,
                                       TreeMap fac_index_map,
                                       TreeMap general_index_map,
				TitleCreator tit) throws Exception
    {
    } // end abbreviation section

public void endAbbreviationSection(TreeMap a,TreeMap b,TreeMap c,
TreeMap d,TreeMap e)
{
	}
	
    
    public void startAppendixSection(String appendix_name,
                                     String appendix_title,
				TitleCreator tit,
					String short_title) throws Exception
    {
//		g_pr.println("START APPENDIX: " + appendix_name + ", " +
//			appendix_title);
		g_pr.println("SECTION:" + appendix_name + // filename (section name?)
		"::" + appendix_title + "::" + // title twice, one long one short
		appendix_title);  
    }
 public void   startAppendixSection(String appendix_name,
 String appendix_title,
 String appendix_short)
 {
 		g_pr.println("SECTION:" + appendix_name + // filename (section name?)
		"::" + appendix_title + "::" + // title twice, one long one short
		appendix_title);  
 } 
    
    public void endAppendixSection(TreeMap state_index_map,
                                   TreeMap abbrev_map,
                                   TreeMap city_index_map,
                                   TreeMap fac_index_map,
                                   TreeMap general_index_map,
				TitleCreator tit) throws Exception
    {
    } // end appendix section
public void    endAppendixSection(TreeMap a,TreeMap b,TreeMap c,TreeMap d,TreeMap e)
{
}

    public void startGeneralSection(String general_name,
                                     String general_title,
		// why is this here?		TitleCreator tit,
				String short_title,
			AuxiliaryInformation aux) throws Exception
    {
//		g_pr.println("START SECTION: " + general_name + ", " +
//			general_title);
		g_pr.println("SECTION:" + general_name +
		"::" + general_title +
		"::" + general_title);
		if (aux != null)
		{
			g_pr.println("!: contains auxiliary information: " +  aux);
		}
		   
    }

public void     startGeneralSection(String x,String y,String z)
{
} 
    
    public void endGeneralSection(TreeMap state_index_map,
                                   TreeMap abbrev_map,
                                   TreeMap city_index_map,
                                   TreeMap fac_index_map,
                                   TreeMap general_index_map,
				TitleCreator tit) throws Exception
    {
    } // end general section
 /*
 * create some stuff that Author input can use
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
//		g_pr.println("START STATE: " + x + ", " +
//			y);

// if there is an anchor, make a separate entry for it	
		printAuthorAnchorName(anchor_name); // will print A: if an anchor name
		setIndexLocation(index_array); // will print IN: if any items
		
			g_pr.print("STATE:" + the_abbrev + "::" +
			the_state + "::");
			printNotNull(short_title);
			g_pr.println(); // terminate	
}

	void printNotNull(String xx)
	{
		if (xx != null)
		{
			g_pr.print(xx);
		}
	}
	
	void printAuthorAnchorName(String anchor_name)
	{
		if (anchor_name != null)
		{
			g_pr.println("A:" + anchor_name);
		}
	}

public void
   endGeneralSection(TreeMap x,TreeMap y,TreeMap z,TreeMap a,TreeMap b)
   {
   } 
   // obsolete, I think
    public void startAState(String the_abbrev, 
                            String the_state,
                            String anchor_name,
                            List index_array,
                            TreeMap state_index_map,
                            TreeMap current_city_index_map,
                            TreeMap current_fac_index_map,
				TitleCreator tit,
				String short_title) throws Exception
    {
		g_pr.println("START STATE: " + the_abbrev + ", " +
			the_state);
    }
    
    

    public void makeTableOfContents(PrintWriter pr,
                                    TreeMap state_index_map,
                                    TreeMap abbrev_map,
                                    TreeMap city_index_map,
                                    TreeMap fac_index_map,
                                    TreeMap general_index_map,
                                    TitleCreator tit) throws Exception
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
        // anything to be done?
    }

    
    
    /*
     * make city headings and link references for access
     */
    public void startACity(String the_abbrev, 
                            String the_state,
                           String the_city,
                            String anchor_name,
                            List index_array)
    {
//		g_pr.println("START CITY: " + the_abbrev + ", " +
//			the_state + ", " + the_city);
		printAuthorAnchorName(anchor_name); // if anything there
		setIndexLocation(index_array); // will print IN: if any items

		g_pr.println("CITY:" + the_abbrev +
			"::" + the_state +
			"::" + the_city);
    }

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
                               List index_array)
    {
		//	g_pr.println("START FACILITY: " + the_abbrev + ", " +
		//		the_state + ", " + the_city + ", " +
		//		the_facility);
		printAuthorAnchorName(anchor_name); // if anything there
		setIndexLocation(index_array); // will print IN: if any items

		g_pr.print("FAC:" + the_abbrev +
			"::" + the_state +
			"::" + the_city + "::" +
			the_facility + "::");
		printNotNull(source_page);
		g_pr.print("::"); // separator
          printNotNull(open_date);
		g_pr.print("::"); // separator
          printNotNull(personnel_count);
		g_pr.print("::"); // separator
          printNotNull(hospital_admissions);
		g_pr.print("::"); // separator
          printNotNull(close_date);
		g_pr.println("::" + no_postal_history_flag);
    } // end start a facility


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
     * boilerplate is just text lines, which contain
     * newlines, which must be maintained
     *
     * we just insert the text from boilerplate.
     * this makes Author writer more trouble, since they
     * have to repeat the text when it appears in new
     * content. For conversion purposes, this is OK.
     */
    public void startBoilerplate(int type,
                                 String inner_text,
                                 String the_preceeding_text,
                                 boolean allow_span)
    {
	//	g_pr.println("BOILERPLATE: " + type + 
	// PRECEEDING TEXT means what is says:
	printNotNull(the_preceeding_text);	
	g_pr.print(BOILERPLATE[type]); // plain text probably contains newlines
	printNotNull(inner_text);	
	g_pr.println(); // finish, condition?			
    } // end start boilerplate text
    
    /*
	 * starting text with inserted info, such as see also inside
	 * this is a new paragraph
	 */
    public void startText(String text,
		String anchor_name) // start of sequence with inserted link, emphasis, etc
    {
		//g_pr.println("P: ");
		g_pr.println("\n"); // empty line starts paragraph
		if (anchor_name != null)
		{
			g_pr.println("A: " + anchor_name);
		}
		g_pr.println(BookUtils.rN(text));
    }
    
    public void startPREText(String text,
                          String anchor_name) // start of sequence with inserted link, emphasis, etc
    {
	/*
         * Author writer cannot handle this type
         */
        g_pr.println("!: AUTHOR creator CANNOT handle preformatted text!");
        startText(text,anchor_name);
    }
    
    public void endText(String text)  // last text in a sequence
    {
		g_pr.println(BookUtils.rN(text));
	}
    
    
    
    public void setAnchor(String name) // destination of a "go to"
    {
//		g_pr.println("SET ANCHOR: " + name);
		g_pr.println("A:" + name);  
    }

    
    public void insertSeparator(String anchor_name)
    {
//		g_pr.println("INSERT SEPARATOR");
		g_pr.println("\n- - - - - - - - - - - - - - - - - - - -\n\n");  
    }
    
    /*
     * ALL LISTS will be a formatting matter, rather than letting the
     * renderer (browser) do the work, we have to indent, add bullet at
     * front, etc, etc.
     */
    public void endList() // ends bulleted list
    {
		g_pr.println("LISTEND:");
    }
    
    public void startList(String size)  // start bulleted list
    {
		if (size != null)
		{
			g_pr.println("LIST:" + size);
		}
		else
		{
			g_pr.println("LIST:");
		}
    }

	/*
	 * start a list item. it could have inner code, such
	 * as see also. We will not work on that. the program
	 * that reads this stuff will have to decide what to do
	 * when it makes XML
	 */
    public void insertListItemStart(String text,
		String anchor_name) 
    {
//		g_pr.println("LI: " + text);
		g_pr.print("LI:" + BookUtils.rN(text));  
    }
    
	/*
	 * remaining text in list item, just print
	 */
    public void insertListItemEnd(String text)
    {
		g_pr.println(BookUtils.rN(text)); // end line here
    }
            
    public void insertListItem(String text,
		String anchor_name)  // add bullet item
    {
    	
		g_pr.println("LI: " + BookUtils.rN(text));
    }

    /*
     * this will be done by creating special blocks with formatting,
     * as we cannot rely on the renderer (browser) to do the work
     * THERE WILL BE various heading types, for testing one size fits all
     */
    public void createHeading(int type, String text,
		String anchor_name)
    {
// debugging    	System.err.println("output? " + g_pr);
		//g_pr.println("HEADING: " + type + ", " + text);
		// strip newlines, as most author items are one line in length
		// put in anchor
		printAuthorAnchorName(anchor_name); // may be null
		g_pr.print("H" + type + ":");
		// text might be null (is this needed?)
		if (text != null)
		{
				g_pr.print(BookUtils.rN(text));
			}
		g_pr.println(); 
    }
    
    /*
     * will use span
     */
    public void insertQuotedText(String text)  // text in "middle" of a sequence with italics as a quote
    {
//		g_pr.println("QUOTED TEXT: "  + text);
		insertCitedText(text);  
    }
    
    public void insertCitedText(String text)  // text in "middle" of a sequence with italics as a cite
    {
//		g_pr.println("CITED TEXT: "  + text);
		g_pr.println("I:" + BookUtils.rN(text)); // italics items only  
    }
    
    public void insertEmphasizedText(String text)  // text in "middle" of a sequence which is bolded
    {
//		g_pr.println("EMPHASIZED TEXT: "  + text);
		g_pr.println("B:" + BookUtils.rN(text));  
    }

	/*
	 * text that fits between see also, and other diversions from plain text
	 */
    public void insertIntermediateText(String text) // text in the "middle" of a sequence that is ordinary
    {
		g_pr.println(BookUtils.rN(text));
    }
    
    /*
     * will be replaced with the appropriate "link" code
     */
    public void insertLink(String href,
                           String text)
    {
    	// NO newlines allowed!
		g_pr.println("L:"  + 
			BookUtils.rN(href) + "::" + 
			BookUtils.rN(text));
    }
    
	/*
	 * simple text is always a start of paragraph,
	 * then text content
	 */
    public void insertSimpleText(String content)
    {
		//g_pr.println("P: "  + content);
		g_pr.println("\n"); // empty line will start paragraph
		g_pr.println(content); // may contain newlines, should be OK (?)
		g_pr.println("\n"); // empty line will end paragraph
    }
    
    public void insertBlockQuote(String content)
    {
		// treat as one continuous line of italics
		g_pr.println("!: blockquote"); // warn post-processor
		g_pr.println("I: "  + content);
		//g_pr.println("BQ: "  + content);
    }
    
    /*
     * will use link code
     */
    public void insertSeeAlso(
        String filename,
        String link,
        String content,
        String middle_text,
        String final_text)
    {
//			g_pr.println("SEE ALSO: "  + filename + ", " +
//			link + ", " + content + ", " + middle_text +
//			", " + final_text);
//S:future::(see future bulletins article)
		g_pr.print("S:" + link + "::" + content);
		// additional text only if desired
		printNotNull(middle_text);
		printNotNull(final_text);
		g_pr.println();

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
					String anchor_name)
    {
//		g_pr.println("IMAGE: " + thumb_image_location + ", " +
//			full_image_location + ", " + pdf_scale + ", " +
//			html_width + ", " + pdf_use + ", " + caption);
		if (remote)
		{
		g_pr.print("RIM:" + full_image_location + "::" +
			thumb_image_location + "::" +
			anchor_name);
		}
		else
		{

		g_pr.print("IM:" + full_image_location + "::" +
		full_image_location + "::" + // repeat so thumb is SAME
		pdf_scale + "::" +
		html_width + "::" +
		pdf_use + "::");
		// anchor name may be null
		if (anchor_name != null)
		{
			g_pr.print(anchor_name);
		}
		else
		{
			// must put in dummy entry
			g_pr.print("imxx" + image_index); // will advance
			image_index++;
		}
		} // end local
		g_pr.println(); // finish
//
// next line is caption
//			IM:brus1.jpg::brus1.jpg::.5::300::f::brus1
//Figure 1:  The components of the initial inquiry into the Brusilov Offensive of 1916.  The sender was Karl Friedl, a cadet serving at the K.U.K. Reserve Spital (Hospital)  in Doina, Galizien (Galicia).  He addressed his letter to Sir A. Friedl, 10th District, # 21 Alixinder Lane, Vienna.  Stryl refers to the town, as well as the river.  The cancel is that of Dolina, Gal (icia) and appears to be June or July 31, 1916.   The 32.f.w/2  marking at upper left is unknown to the author. The Austrian military censor was in the Ukranie/Russian town of Stryj. 
		g_pr.println(caption);

    } // end process inline image

    

    public void noteReferenceDuplicate(String key,
                                       String content,
                                       String context) // for debugging, this gets stored somewhere
    {
        // put into main index.html file
        
     // NOT NOW FOR FOP   g_pr.println("<!-- duplicate " + key + " " + content + "\n   from " + context + " -->");
        
    }
	
    public void setIndexLocation(List index_items)
    {
    		if (index_items == null)
    		{
    			return;
    		}
    		printAuthorIndex(index_items);
    }

	void printAuthorIndex(List index_items)
	{    
    		g_pr.print("IN:");
		int the_length = index_items.size();
		int the_lengthm1 = the_length - 1;
    		IndexRef item = null;
		for (int inner = 0 ; inner < the_length ; inner++)
    		{
    			item = (IndexRef)index_items.get(inner);
    			g_pr.print(BookUtils.rN(item.name));
    			if (inner != the_lengthm1 )
    			{
    				g_pr.print(":"); // SINGLE colon for next item
    			}
    		
    		}
    		// done
    		g_pr.println();
    	}

    public void setOptions(Options values) throws Exception
    {
	g_options = values; // we may not use these, but there they are
    }
/*
 * needed for interface, but we do not handle manifests
 */    
    public void 
addToManifest(String x,String y, int flag)
{
} 
/*
* will we see these during conversion from XML?
*/
public void insertIntermediateBreak()
{
}
    
    public void makeNewLine(PrintWriter pr)
    {
        g_pr.println("NEWLINE REQUEST");
    }

    public void startTable(String [] header_cells) throws Exception
	{
		throw new Exception ("AUTHOR output cannot handle tables");
	} // end start table
            
    public void endTable() throws Exception
	{
		throw new Exception ("AUTHOR output cannot handle tables");
	}
            
    public void insertTableRow(String [] cells) throws Exception // multiple items in a row
	{
		throw new Exception ("AUTHOR output cannot handle tables");
	} // end insert table row
    
 } // end  Author sink in bulletins
