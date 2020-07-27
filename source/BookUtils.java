import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Properties;
import java.util.Iterator;
import java.util.regex.*;
import java.io.File;
import java.io.PrintWriter;

// for JSON processing

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/*
 * various static utility methods used for book content
 * creation. 
 *
 * include method to read JSON and provide Properties
 *
 * at the start of this refactor, these are mostly HTML-related,
 * but could be anything. Added generic static methods to get
 * information from the maps and make the general index
 * structure. The format-specific objects will take this
 * information and create appropriate code for PDF, HTML, etc.
 *
 * NOTE regarding escape of non-usual characters. We need to
 * escape characters like tilde, accented chars, leading and
 * following quotes. These are a pain in the A*******s. If
 * we can handle it, we will have escaped output
 * in all the popular formats.
 *
 * NOTE, as of 12/26/2018 we will try to escape the
 * greater-than and less-than. These had not been
 * experienced before. (see eT() method)
 *
 * updated 12/26/2018
 */

public class BookUtils
{
    public final static int STATE_FACILITIES = 0;  // when making state facility cross-references
    public final static int ALL_FACILITIES = 1;  // when making all facility cross-reference
    public final static int ALL_FACILITIES_POPUP = 2;  // when making all facility popup window
    
    public final static int UNNUMBERED_FACILITY = 0;
    public final static int NUMBERED_FACILITY = 1;
    
    /*
     * used for boilerplate text, unique for each use
     *
     * IDEALLY, these strings should be entered in a
     * "json" file, so they are configuration data, NOT
     * compiled Java static data!!!
     */
    public final static String[] HTML_BOILERPLATE = {
"<i>Postmarks on Postcards</i>",
        "The U. S. Army <i>Order of Battle</i> book",
        "<i>The Postal History of the AEF</i>",
        "Appendix 2 of <i>The Postal History of the AEF</i>",
        "Appendix 3 of <i>The Postal History of the AEF</i>",
        "The book <i> The United States Marine Corps in the World War</i>",
        "The Navy <i>Yards and Docks</i> book",
        "The army <i>Report of the Surgeon General</i> book",
        "The book <i>History of Communications-Electronics in the United States Navy</i>",
        "The document <i>Report of the Surgeon General, U. S. Navy, 1919</i>",
        "The Buzzell <i>Great Lakes</i> book",
        "Professor Cunningham's website"
    };
    public final static String[] FOP_BOILERPLATE = {
"<fo:inline font-style=\"italic\">Postmarks on Postcards</fo:inline>",
        "The U. S. Army <fo:inline font-style=\"italic\">Order of Battle</fo:inline> book",
        "<fo:inline font-style=\"italic\">The Postal History of the AEF</fo:inline>",
        "Appendix 2 of <fo:inline font-style=\"italic\">The Postal History of the AEF</fo:inline>",
        "Appendix 3 of <fo:inline font-style=\"italic\">The Postal History of the AEF</fo:inline>",
        "The book <fo:inline font-style=\"italic\"> The United States Marine Corps in the World War</fo:inline>",
        "The Navy <fo:inline font-style=\"italic\">Yards and Docks</fo:inline> book",
        "The army <fo:inline font-style=\"italic\">Report of the Surgeon General</fo:inline> book",
        "The book <fo:inline font-style=\"italic\">History of Communications-Electronics in the United States Navy</fo:inline>",
        "The document <fo:inline font-style=\"italic\">Report of the Surgeon General, U. S. Navy, 1919</fo:inline>",
        "The Buzzell <fo:inline font-style=\"italic\">Great Lakes</fo:inline> book",
        "Professor Cunningham's website"
    };
    
    public final static String[] AUTHOR_BOILERPLATE = {
"I:Postmarks on Postcards",
        "The U. S. Army \nI:Order of Battle\n book",
        "I:The Postal History of the AEF",
        "Appendix 2 of \nI:The Postal History of the AEF",
        "Appendix 3 of \nI:The Postal History of the AEF",
        "The book \nI:The United States Marine Corps in the World War",
        "The Navy \nI:Yards and Docks\n book",
        "The army \nI:Report of the Surgeon General\n book",
        "The book \nI:History of Communications-Electronics in the United States Navy",
        "The document \nI:Report of the Surgeon General, U. S. Navy, 1919",
        "The Buzzell \nI:Great Lakes\n book",
        "Professor Cunningham's website"
    };    
    public final static String[] PDF_BOILERPLATE = {
"[TITLE]Postmarks on Postcards[/]",
        "The U. S. Army [TITLE]Order of Battle[/] book",
        "[TITLE]The Postal History of the AEF[/]",
        "Appendix 2 of [TITLE]The Postal History of the AEF[/]",
        "Appendix 3 of [TITLE]The Postal History of the AEF[/]",
        "The book [TITLE] The United States Marine Corps in the World War[/]",
        "The Navy [TITLE]Yards and Docks[/] book",
        "The army [TITLE]Report of the Surgeon General[/] book",
        "The book [TITLE]History of Communications-Electronics in the United States Navy[/]",
        "The document [TITLE]Report of the Surgeon General, U. S. Navy, 1919[/]",
        "The Buzzell [TITLE]Great Lakes[/] book",
        "Professor Cunningham's website"
    };
    
    // NEEDED????
    public final static int POP_TEXT = 0;
    public final static int OOB_TEXT = 1;
    public final static int AEF_TEXT = 2;
    public final static int AEF2_TEXT = 3;
    public final static int AEF3_TEXT = 4;
    public final static int MAR_TEXT = 5;
    public final static int YARD_TEXT = 6;
    public final static int MED_TEXT = 7;
    public final static int RAD_TEXT = 8;
    public final static int NMED_TEXT = 9;
    
/*
 * the LENGTH of the following array will be used for
 * searching for exact "types" of boilerplate references. Only
 * these strings (case insensitive) will be considered
 *
 * POSITION must match positions in the above strings of boilerplate
 * content. NOTE comment about all this being really
 * placed in a "json" file, since it is configuration data,
 * not hard-coded Java
 */
    public final static String[] BOILERPLATE_KEY = {
        "pop", // POP_TEXT
        "oob", // OOB_TEXT
        "aef", // AEF_TEXT
        "aef2", // AEF2_TEXT
        "aef3", // AEF3_TEXT
        "mar", // MAR_TEXT
        "yard", // YARD_TEXT
        "med", // MED_TEXT
        "rad", // RAD_TEXT
        "nmed", // NMED_TEXT
        "buzz", // BUZZ_TEXT
        "cunningham"  
    };
    
    
    public final static int FOR_URL = 0;
    public final static int FOR_HTML = 1;
    public final static int FOR_TEXT = 2;
    
    /*
     * when we write any text to the HTML output,
     * we have to be aware that & signs may appear in the
     * text.
     *
     * and signs are escaped in the original XML, which is
     * necessary to make it scan correctly. However, those
     * escapes have disappeared by the time we reach
     * the final output to the HTML files. So, we have to
     * escape THEM AGAIN (sorry....)
     *
     * IN ADDITION, the Unicode characters that were read into
     * Java no longer have any escaping. They could have been
     * read as native characters, or as escaped characters in
     * the source XML. In any case, we must escape them, so that
     * products down the line (browser, Kindle creator, PDF creator)
     * have properly escaped characters, not the native bytes.
     *
     * Tired of long names, so we use eT instead of escapeText()
     */
    public static String eT(String cont, boolean escape_quotes)
    {
        if (cont == null)
        {
            return "(null)";
        }
        // not null, so replace the ampersands with the special HTML escape code
        //
        String newer = cont.replaceAll("&","&amp;");   // escape and signs with XML-compliant stuff
        String newer2 = newer.replaceAll("<","&lt;");   // escape less-than
        String newer3 = newer2.replaceAll(">","&gt;");   // escape less-than
/*
 * convert to array of char, then check each one for more than 126, which is
 * the decimal for "normal" characters. Anything larger, gets escaped.
 */        
 	     char input_characters[] = newer3.toCharArray();
 	     StringBuffer result = new StringBuffer(); // good idea?
 	     for (int inner = 0; inner < input_characters.length ; inner++)
 	     {
		if (input_characters[inner] > '~')
		{
			// escape it!
			String escaped = "&#" + (int)input_characters[inner] + ";";
		System.err.println("escaped: " + escaped); // input_characters[inner]);
				result.append(escaped);
		}
		else
	   { 	
 	     	      result.append(input_characters[inner]);
	//System.err.println("not escaped: " + input_characters[inner]);
//System.err.println("not escaped: " + input_characters[inner]);
	 	     	}
 	     } // end for each character to be written
 	     return result.toString();
              
//        return newer;
        // WHY WHY WHY??? return escapeSingleQuotes(newer);
    } // end eT cleanup for HTML
// debugging!!
    public static String eTxxxx(String cont, boolean escape_quotes)
    {
        return cont;
    }
    /*
     * old interface with no escaping single quotes
     */
    public static String eT(String cont)
    {
        return eT(cont,false);
    }
    
    /*
     * parse the facilityreference object and create the string
     * that displays the full facility name. This includes facilities
     * that are numbered, such as General Hospitals
     */
    public static String createFacilityName(FacilityReference fac, int mode, boolean escape_quotes)
    {
        StringBuffer result = new StringBuffer(20);
        result.append(fac.facility_name); // simplest name
        switch (fac.facility_type)
        {
            case BookUtils.UNNUMBERED_FACILITY:
            {
                break; // nothing special
            }
            case BookUtils.NUMBERED_FACILITY:
            {
                result.append("#" + String.valueOf(fac.facility_number)); // assume leading spaces are already in name string
                break;
            }
        }
        if (mode == FOR_URL)
        {
            // must fully encode so there are no blanks, etc
            return eC(result.toString(),escape_quotes);
        }
        else
        {
            if (mode == FOR_HTML)
            {
                // only escape & signs
                return eT(result.toString(),escape_quotes);
            }
            else
            {
                if (mode == FOR_TEXT)
                {
                    return result.toString(); // no alteration at all
                }
            }
        }
        return null;   // something wrong
    } // create facility name

    /*
     * old interface, we don't escape single quotes
     */
    public static String createFacilityName(FacilityReference fac, int mode)
    {
        return createFacilityName( fac,  mode, false);
    }
    
    /*
     * encode anchor names and links to anchors. for instance,
     * <a name="x x x "> is illegal because of the embedded spaces.
     * similar issues with other characters, so we want them as &nn;  notation
     * and other values acceptable in a URL
     */
    public static String eCxxx(String orig, boolean escape_quotes) 
    {
        try
        {
            if (escape_quotes)
            {
                return URLEncoder.encode(escapeSingleQuotes(orig),"utf-8");   // recommended encoding
            }
            else
            {
                return URLEncoder.encode(orig,"utf-8");   // recommended encoding
            }
        }
        catch (Exception ex)
        {
            System.err.println("URL conversion error: " + ex);
            System.exit(-1); // really bad
            return null; // should not happen
        }
    }
    /*
     * new version
     *
     * turns out that URLs can have all kinds of characters,
     * but <a name= tags are very strict. In HTML, not a
     * biggie, but in Kindle, embedded special chars not
     * so good.
     *
     * soooo,
     *
     * we replace all non-letter, non-digit with underline. There should be
     * no collision, as most name= names we create are quite unique
     * THEN, we replace high-Latin characters, such as tilde with
     * the numeric code. But not as an escape, just as _num_ with
     * "num" being the decimal code
     *
     */
    public static String eC(String orig, boolean escape_quotes) 
    {
        // NOTE ignore escape_quotes at this time, because the quotes
        // will be replaced with underline
        //
	if (orig == null)
	{
		return "(null)";
	}
        char [] working = orig.toCharArray();
        StringBuffer dest = new StringBuffer();
        for (int inner = 0 ; inner < working.length ; inner++)
        {
		if (working[inner] > '~')
		{
			// high Latin, maybe tilde, accent, etc
	// escape it!
			String escaped = "_" + (int)working[inner] + "_";
		System.err.println("escaped: " + escaped); 
			dest.append(escaped);
		}
		else
		{
			// ordinary character, but check for non-digit, non-letter
		    if ( (Character.isDigit(working[inner])) ||
			 (Character.isLetter(working[inner])) )
		    {
			// letter or digit, leave alone
			dest.append(working[inner]);
		    }
		    else
		    {
			dest.append("_");  // substitute underline (it is legal)
		    }
		}
	} // end loop on each character of string
        // was return new String(dest);
		return dest.toString();
    }  // end escape strings to be used in name= and embedded in URL's
    
    /*
     * old interface before escaping single quotes
     */
    public static String eC(String orig) 
    {
        return eC(orig,false);
    }
    /**
     * receive a String and return it with all single quotes
     * replaced by backslash/single quote. This is needed to
     * make Javascript work right
     */
    public static String escapeSingleQuotes(String x)
    {
        if (x.indexOf("'") >= 0)
        {
            // debug System.out.println("Testing: " + x);
            String rep = Matcher.quoteReplacement("\\'");
            // debug System.out.println("Replacing single quote with: --" + rep + "--");
            String newer = x.replaceAll("'",rep);
            //        String newer = x.replaceAll("'","\\\\'");   // replace every single quote with backslash/single
            return newer;
        }
        else
        {
            return x; // no single quote to replace
        }
    }
// debugging!
    public static String escapeSingleQuotesxxx(String x)
    {
        return x;
    }
    
    /*
     * look at the ExactReferences that contain one or
     * more ExactReference objects. The caller extracted the
     * reference to this List from the tree associated with
     * a FacilityReference.
     *
     * If ANY entry has the "nopost" flag set to true,
     * then return true. That is, one or more are nopost
     * items, and the entire list will be checked again
     * for individual inclusion in the NOPOST index.
     *
     * If ALL entries have the "nopost" set to false, then
     * there are no entries that fit in the no postal history,
     * so return false.
     */
    public static boolean someNoPostalHistory(List the_list)
    {
        ExactReference ex_ref = null;
        Iterator inner = the_list.iterator();
        while (inner.hasNext())
        {
            ex_ref = (ExactReference)inner.next();   // each ExactReference
            if (ex_ref.nopost)
            {
                return true; // QUIT now, there is at least one usable entry
            }
        } // loop for all references
        // ALL entries were false, so we indicate that there are no useable entries 
        return false;
    }
    
    /*
     * unfortunately, some index references point to an
     * appendix only, no city or state. Most of the formatting
     * is oriented towards those pairs. This is a bit of a
     * kluge, but we will recognize "state" abbreviations that
     * indicate appendixes, and will pass them back in a readable
     * format.
     */
    public static String fixAppendixName(String s)
    {
        if (!s.startsWith("appendix"))
        {
            return s; // CANNOT DO ANYTHING
        }
        return "Appendix " + s.substring(8).toUpperCase();
    }

    public static IndexGroup createGeneralIndex(
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
                 * 
                 * We are getting duplicates. There should be at least 
                 * one name, optionally followed by a comma, with a second
                 * name. Various strange things can happen. Primarily,
                 * if there are duplicates, we show ONLY ONE name.
                 */
		// always starts with text and the paren
                StringBuffer bb = new StringBuffer(index_item + " (");
		String entry1 = ""; // fill in as needed, check before storing
		String entry2 = "";
                //if (index_ref.city_name == null)
                if (isEmpty(index_ref.city_name))
                { // dont bother with city, its null or an empty string
			// entry 1 will be an empty string
                }
                else
                {
                    if (index_ref.state_abbrev.startsWith("appendix"))
                    { // dont use the appendix as a city name, see later
                    }
                    else
                    {
                        entry1 = index_ref.city_name; // first field, may be followed with comma
                        //bb.append(index_ref.city_name + ", ");
                    }
                }
		// at this point, entry1 may be empty or a value
                if (index_ref.state_abbrev.startsWith("appendix"))
                {
                    entry2 = "Appendix " + index_ref.state_abbrev.substring(8).toUpperCase();
                    //bb.append("Appendix " + index_ref.state_abbrev.substring(8).toUpperCase());
                }
                else
                {
                    entry2 = index_ref.state_abbrev;
                    //bb.append(index_ref.state_abbrev);
                }
		/*
		 * at this point, entry1 may be empty or have a name
		 * entry2 will have something in it.
		 * 
		 * we DONT want duplicates. If two non-equal names 
		 * are present, we add them with a comma between
		 */
		if (entry1.equals(entry2))
		{
			// duplicated use JUST ONE
			bb.append(entry1 + ")"); // single name and paren
		}
		else
		{
			// two different entries, but don't use entry1 if empty
			if (isEmpty(entry1))
			{
				bb.append(entry2 + ")"); // second name ONLY and paren
			}
			else
			{
				bb.append(entry1 + "," + entry2 + ")"); // both, comma and paren
			}
		}
		// at this point "bb" is complete general entry line
                current_group.children.add(new IndexEntry(
                    "general_" + eC(index_item) + 
                    "_" + index_ref.ref_number, // ID contains escaped name and ref number
                    bb.toString(), // full name with city and state
                    //     index_item,  // text name
                    index_ref.state_abbrev // becomes target file
                    ));
            } // end for each reference
        } // end for each general index item
        return current_group;
    } // end create general index
    

    public static IndexGroup createStateIndex(
        SpecialContentCreator tit,
        Map state_index_map
        ) throws Exception
    {
        IndexGroup current_group = new IndexGroup(
        tit.gT("STATE_INDEX_ID"),
        tit.gT("STATE_INDEX_TITLE"),
        tit.gT("STATE_INDEX_TITLE"),
        false, // no crumbs
        tit.gT("STATE_INDEX_ID"),"States");
    // done by caller the_root.add(current_group); // add to list
                
        Iterator inner = state_index_map.keySet().iterator();
        String index_item = "";
        List some_items = null;
        ExactReferenceComparable the_ref = null;
                
        while (inner.hasNext())
        {
            index_item = (String)inner.next();  // state name
            // ONLY ONE STATE PER ENTRY
            some_items = (List)state_index_map.get(index_item);
            the_ref = new ExactReferenceComparable((ExactReference)some_items.get(0)); // first item only
            
            // we are only processing state items
            current_group.children.add(new IndexEntry(
                "state_" + 
                eC(the_ref.state_name),  // escape state name
                index_item,  // text name
                the_ref.state_abbrev // becomes target file
                ));
        } // end for each state index item
        return current_group;
    } // end create state index
    
    /*
     * index structure for state-only facility index.
     * this differs from the overall global facility
     * index. Probably only used for HTML, but it is OK
     * to commonize it
     */
    public static IndexGroup createStateFacilityIndex(
        SpecialContentCreator tit,
        Map current_fac_index_map
        ) throws Exception
    {
        IndexGroup current_group = new IndexGroup(
            tit.gT("FACILITY_INDEX_ID"),
            tit.gT("FACILITY_INDEX_TITLE"),
            tit.gT("FACILITY_INDEX_TITLE"),
            false, // we will NOT treat these items as breadcrumbs in the renderer
            tit.gT("FACILITY_INDEX_ID"),"Facilities"  // link for return to top
            );
                
        if (current_fac_index_map.size() > 0)
        {
            // will be done by caller the_root.add(current_group); // add top to the list
            Iterator facs_it = current_fac_index_map.keySet().iterator();
            FacilityReference fac_ref = null;
            Iterator inner2  = null;
            ExactReference ref2 = null;
            String a_title = "";
            
            while (facs_it.hasNext())
            {
                fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
                inner2 = ((List)current_fac_index_map.get(fac_ref)).iterator();
                while (inner2.hasNext())
                {
                    ref2 = (ExactReference)inner2.next();
                    a_title = createFacilityName(fac_ref,
                                                           BookUtils.FOR_HTML,  // must escape HEREHERHERE format_dependent???
                                                           
                                                           false) + 
", " + ref2.state_abbrev;
                    current_group.children.add(new IndexEntry("state_" +
                                                              eC(ref2.state_name) +  // escape state name
                                                              "_city_" +
                                                              eC(ref2.city_name) +   // escape city name
                                                              "_fac_" + createFacilityName(fac_ref,
                                                                                           BookUtils.FOR_URL), // HERE HERE HERE is this dependent on final format???
                                                              a_title,
                                                              ref2.state_abbrev // becomes target file
                                                              ));
                } // end for each facility with same name
            } // end for each facility index item
        } // end if any facilities
        return current_group;
    } // end create state facility index (limited)
    
    /*
     * overall (global) facility index. The state-only index
     * is simpler
     */
    public static IndexGroup createOverallFacilityIndex(
        SpecialContentCreator tit,
        Map fac_index_map
        ) throws Exception
    {
        IndexGroup current_group = new IndexGroup(
            tit.gT("FACILITY_INDEX_ID"),
            tit.gT("FACILITY_INDEX_TITLE"),
            tit.gT("FACILITY_INDEX_TITLE"),
            true, // we will treat these items as breadcrumbs in the renderer
            tit.gT("FACILITY_INDEX_ID"),"Facilities"  // link for return to top
            );
        
        // caller will do this            the_root.add(current_group); // add top to the list
        IndexGroup letter_group = null; // one more level are the lettered groups
        
        // FACILITY INDEXES (One for each initial letter)
        
        Iterator inner2 = null;
        ExactReference ref2 = null;
        ExactFacilityReferenceComparable the_ref2 = null;
        String a_title = "";
        
        boolean first_time = true;
        
        
        String initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
        String current_initial_letter = "ZZZZZ";  
        
        Iterator facs_it = fac_index_map.keySet().iterator();
        FacilityReference fac_ref = null;
        
        /*
         * in order to make a set of bookmarks with the alphabet letters (easier navigation
         * to alphabet headings), we must gather them up for later placement
         */
        Map facility_links = new TreeMap();        
        while (facs_it.hasNext())
        {
            fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
            initial_letter = fac_ref.facility_name.substring(0,1);
            if (!initial_letter.equals(current_initial_letter))
            {
                /*
                 * New letter, we make a new bookmark grouping
                 */
                facility_links.put(initial_letter,"'" + initial_letter + "'"); // index name "'A'" etc
                //                        facility_links.put(initial_letter,"Facilities '" + initial_letter + "'"); // index name "Facilities A" etc
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
                /*
                 * must build a reference that has facility in it, to
                 * use as a key to the page location to go to
                 */
                the_ref2 = new ExactFacilityReferenceComparable(
                    ref2.state_abbrev,ref2.state_name,ref2.city_name,
                    createFacilityName(fac_ref,BookUtils.FOR_TEXT,false)); // HEREHERE is the format different for different media??
                
                // we are only processing facility items
                
                
                a_title = createFacilityName(fac_ref,
                                                       BookUtils.FOR_HTML,  // must escape HEREHERE special for HTML or other formats??
                                             false) + 
", " + ref2.state_abbrev;
                letter_group.children.add(new IndexEntry("state_" +
                                                         eC(the_ref2.state_name) +  // escape state name
                                                         "_city_" +
                                                         eC(the_ref2.city_name) +   // escape city name
                                                         "_fac_" +
                                                         eC(the_ref2.fac_name),   // escape facility name
                                                         a_title,
                                                         the_ref2.state_abbrev // becomes target file
                                                         ));
            } // end for each facility with same name
        } // end for each facility index item
        
        return current_group;
    } // end create overall (full) facility index 
    
    /*
     * overall (global) facility index. The state-only index
     * is simpler
     */
    public static IndexGroup createOverallNOFacilityIndex(
        SpecialContentCreator tit,
        Map fac_index_map
        ) throws Exception
    {
        IndexGroup current_group = new IndexGroup(
            tit.gT("FACILITY_NO_INDEX_ID"),
            tit.gT("FACILITY_NO_INDEX_TITLE"),
            tit.gT("FACILITY_NO_INDEX_TITLE"),
            true, // we will treat these items as breadcrumbs in the renderer
            tit.gT("FACILITY_NO_INDEX_ID"),"Facilities No "  // link for return to top
            );
        // done by caller the_root.add(current_group); // add top to the list
        IndexGroup letter_group = null; // one more level are the lettered groups
        
        // FACILITY (NO POSTAL HISTORY) INDEXES (One for each initial letter)
        
        boolean first_time = true;
        ExactReference ref2 = null;
        String a_title = "";
        ExactFacilityReferenceComparable the_ref2 = null;
        Iterator inner2 = null;
                
        String initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
        String current_initial_letter = "ZZZZZ";  
                
        Iterator facs_it = fac_index_map.keySet().iterator();
        FacilityReference fac_ref = null;
                
        /*
         * in order to make a set of bookmarks with the alphabet letters (easier navigation
         * to alphabet headings), we must gather them up for later placement
         */
        Map facility_links = new TreeMap();        
        while (facs_it.hasNext())
        {
            fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
            if (someNoPostalHistory((List)fac_index_map.get(fac_ref)))
            {
                // at least one item has no postal history, so consider adding those that are desirable
                        
                initial_letter = fac_ref.facility_name.substring(0,1);
                if (!initial_letter.equals(current_initial_letter))
                {
                    /*
                     * New letter, we make a new bookmark grouping
                     */
                    facility_links.put(initial_letter,"'" + initial_letter + "'"); // index name "Facilities (NO) A" etc
                    //                            facility_links.put(initial_letter,"Facilities (NO) '" + initial_letter + "'"); // index name "Facilities (NO) A" etc
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
            if (someNoPostalHistory((List)fac_index_map.get(fac_ref)))
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
//   seems to have been wrong all along, now fixed?                     "_facilityno_" + (String)facility_links.get(initial_letter),
                        "_facilityno_" + initial_letter,
                        (String)facility_links.get(initial_letter) + " -- ", // short name is letter only and separator
                        "Facilities (NO) -- " + (String)facility_links.get(initial_letter),
                        false, // no breadcrumb
                        "_facilityno_" + initial_letter,
                        "Facility NO " + initial_letter);
//                    "_facilityno_" + (String)facility_links.get(initial_letter),
  //                      "Facility NO " + (String)facility_links.get(initial_letter));
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
                            createFacilityName(fac_ref,BookUtils.FOR_TEXT,false)); // HEREHEREHERE format different for HTML etc???
                                
                        // we are only processing facility items
                                
                                
                        a_title = createFacilityName(fac_ref,
                                                               BookUtils.FOR_HTML,  // must escape HERE HERE format different?
                                                               false) + 
", " + ref2.state_abbrev;
                        letter_group.children.add(new IndexEntry("state_" +
                                                                 eC(the_ref2.state_name) +  // escape state name
                                                                 "_city_" +
                                                                 eC(the_ref2.city_name) +   // escape city name
                                                                 "_fac_" +
                                                                 eC(the_ref2.fac_name),   // escape facility name
                                                                 a_title,
                                                                 the_ref2.state_abbrev // becomes target file
                                                                 ));
                                
                    } // end if inner item has no postal history
                } // end for each facility (NO) with same name
            } // if at least one inner item has no postal history
        } // end for each facility (NO) index item
        
        return current_group;
    } // end create overall (full) facility with no postal history index 
    
    /*
     * index structure for state-only city index.
     * this differs from the overall global city
     * index. Probably only used for HTML, but it is OK
     * to commonize it
     */
    public static IndexGroup createStateCityIndex(
        SpecialContentCreator tit,
        Map current_city_index_map
        ) throws Exception
    {
        IndexGroup current_group = new IndexGroup(
            tit.gT("CITY_INDEX_ID"),
            tit.gT("CITY_INDEX_TITLE"),
            tit.gT("CITY_INDEX_TITLE"),
            false, // we will NOT treat these items as breadcrumbs in the renderer
            tit.gT("CITY_INDEX_ID"),"Cities");  // link for return to top
        // DONT add to the_root yet, see if there are ANY entries
        if (current_city_index_map.size() > 0)
        {
            // caller will do this    the_root.add(current_group); // add top to the list
            Iterator inner = current_city_index_map.keySet().iterator();
            String the_city = "";
            ExactReference ref = null;
            Iterator inner2 = null;
            
            while (inner.hasNext())
            {
                the_city = (String)inner.next();
                inner2 = ((List)current_city_index_map.get(the_city)).iterator();
                while (inner2.hasNext())
                {
                    ref = (ExactReference)inner2.next();
                    current_group.children.add(new IndexEntry("state_" +
                                                              eC(ref.state_name) + "_city_" +
                                                              eC(ref.city_name),
                                                              the_city +                        
                                                              "  (" + ref.state_abbrev + ")", 
                                                              ref.state_abbrev // becomes target file
                                                              ));
                } // end for each city (including state, since there are itentical city names across states
            } // end for each city index item
        } // end if any cities to list
        return current_group;
    } // end create state-only (partial) city index 
    
    /*
     * overall (global) city index. The state-only index
     * is simpler
     */
    public static IndexGroup createOverallCityIndex(
        SpecialContentCreator tit,
        Map city_index_map
        ) throws Exception
    {
        IndexGroup current_group = new IndexGroup(
            tit.gT("CITY_INDEX_ID"),
            tit.gT("CITY_INDEX_TITLE"),
            tit.gT("CITY_INDEX_TITLE"),
            true, // we will treat these items as breadcrumbs in the renderer
            tit.gT("CITY_INDEX_ID"),"Cities");  // link for return to top
       // caller will handle this the_root.add(current_group); // add top to the list
        IndexGroup letter_group = null; // one more level are the lettered groups
                
        // CITY INDEX, grouped by initial letter
                
        boolean first_time = true;
                
                
        String city_initial_letter = "ZZZZZ";   // used for making subheadings to break up long index
        String city_current_initial_letter = "ZZZZZ";  
        /*
         * in order to make a set of bookmarks with the alphabet letters (easier navigation
         * to alphabet headings), we must gather them up for later placement
         */
        TreeMap city_links = new TreeMap();        
        String index_item = "";
                
        Iterator inner = city_index_map.keySet().iterator();
                
        while (inner.hasNext())
        {
            index_item = (String)inner.next();  // city name
            city_initial_letter = index_item.substring(0,1);
            if (!city_initial_letter.equals(city_current_initial_letter))
            {
                /*
                 * New letter, we make a new bookmark grouping
                 */
                //                        city_links.put(city_initial_letter,"Cities '" + city_initial_letter + "'"); // index name "Cities A" etc
                city_links.put(city_initial_letter,"'" + city_initial_letter + "'"); // index name "'A'" etc
                city_current_initial_letter = city_initial_letter;
            }
        } // end for each item in city map
                
        /*
         * start over again. We will create each city bookmark grouping separately
         */
        inner = city_index_map.keySet().iterator();
        city_current_initial_letter = "ZZZZ";
        letter_group = null; // one more level are the lettered groups
        ExactReferenceComparable the_ref = null;
        
                
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
            List some_items = (List)city_index_map.get(index_item);
            Iterator inner2 = some_items.iterator();
            while (inner2.hasNext())
            {
                the_ref = new ExactReferenceComparable((ExactReference)inner2.next()); 
                        
                // we are only processing city items
                letter_group.children.add(new IndexEntry("state_" +
                                                         eC(the_ref.state_name) +  // escape state name
                                                         "_city_" +
                                                         eC(the_ref.city_name),   // escape city name
                                                         index_item +
                                                         "  (" + the_ref.state_abbrev + ")", 
                                                         the_ref.state_abbrev // becomes target file
                                                         ));
                        
            } // end for each city with same name
        } // end for each city index item
        return current_group;
    } // end create overall (full) city index 
    
    	/*
	 * remove newlines
	 */
    public static String rN(String orig) 
    {
        return orig.replaceAll("\n",""); 
	}

	/*
	     * StringUtils.isEmpty(null)      = true
	     * StringUtils.isEmpty("")        = true
	     * StringUtils.isEmpty(" ")       = false
	     * StringUtils.isEmpty("bob")     = false
	     * StringUtils.isEmpty("  bob  ") = false
	     *
     *
     * @param str  the String to check, may be null
     * @return <code>true</code> if the String is empty or null
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

	public static String returnContentsOrNull(String str)
	{
		if (isEmpty(str))
		{
			return null;
		}
		else
		{
			String result = str.trim(); // remove trailing or preceeding
			if (isEmpty(result))
			{
				return null; // was only spaces
			}
			else
			{
				return result;
			}
		} //end if not immediately empty
	} // end return contents or null

	public static Properties getPropertiesFromJSON(String filename,
		String data_group) throws Exception
	{
		File input = new File(filename);
		ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
		Map<String,Object> userData = mapper.readValue(input,Map.class);
        
		// userData is a Map containing the named arrays
        
		/*
		 * a List of Strings that specify
		 * options. They
		 * are pairs, keyword, then value
		 */
		List optionsx = (List)userData.get(data_group); 
		Properties options = new Properties();
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
		return options; // pass back Properties
	} // end get properties from JSON
    
	/*
	 * used for processing flags in JSON configuration files
	 */
	public static boolean getBoolean(String val)
	{
		if (val.equalsIgnoreCase("yes"))
		{
			return true;
		}
		if (val.equalsIgnoreCase("true"))
		{
			return true;
		}
		return false;
	} // end getboolean

	/*
	 * helper for many special content creators
	 * create a page of output from a list of Strings
	 */
    public static  void createAPage(String filename, 
		List page_object,
		boolean close_at_end) throws Exception
    {
        PrintWriter pr = new PrintWriter(new File(filename));
        Object someobject = null;
        Iterator ii = page_object.iterator();
        while (ii.hasNext())
        {	
            someobject =  ii.next();
            if (someobject instanceof String)
            {
                pr.println(someobject);
                continue; // done for now
            }
            //  now if there is a fall-through, the objects are somebody we don't know about
            throw new Exception("Problems with JSON inside: " + filename + ", object: " + someobject.getClass().getName());
        } // end write the content to the stream
        
	if (close_at_end)
	{
		pr.flush();
		pr.close();
	    }
    } // end create a page given filename

	/*
	 * helper for many special content creators
	 * create a page of output from a list of Strings
	 */
    public static  void createAPage(PrintWriter out,
		List page_object) throws Exception
    {
        Object someobject = null;
        Iterator ii = page_object.iterator();
        while (ii.hasNext())
        {	
            someobject =  ii.next();
            if (someobject instanceof String)
            {
                out.println(someobject);
                continue; // done for now
            }
            //  now if there is a fall-through, the objects are somebody we don't know about
            throw new Exception("Problems with JSON object: " + someobject.getClass().getName());
        } // end write the content to the stream
    } // end create a page given open PrintWriter

	/*
	 * read JSON, using jackson, given the object
	 * name to use to create filename desired
	 */
	public static Map<String,Object> readJSON(String object_name,
		boolean debug_it) throws Exception
	{
		String filename = object_name  + ".json"; // filename to read
		if (debug_it)
		{
			System.out.println("Getting JSON from: " + filename);
		}
		File input = new File(filename);
		ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
		Map<String,Object> userData = mapper.readValue(input,Map.class);
		return userData;
	} // end read JSON given object name

	/*
	 * read JSON, using jackson, given the File
	 * instance of the desired input filename
	 */
	public static Map<String,Object> readJSON(File the_file,
		boolean debug_it) throws Exception
	{
		if (debug_it)
		{
			System.out.println("Getting JSON from: " + the_file);
		}
		ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
		Map<String,Object> userData = mapper.readValue(the_file,Map.class);
		return userData;
	} // end read JSON given File object

    public static TreeMap makeNOFChecklist(
        SpecialContentCreator tit,
        Map fac_index_map
        ) throws Exception
    {
	/*
	 * We will create an INDEX that is ordered by state and city
	 */
        ExactReference ref2 = null;
        FacilityReference fac_ref = null;
	TreeMap nof_by_city = new TreeMap();
	/*
	 * this is ordered by facility name, and can contain
	 * multiple entries
	 */
        Iterator facs_it = fac_index_map.keySet().iterator();
	Iterator inner2 = null;
        ExactFacilityReferenceComparableByCity the_ref2 = null;
        while (facs_it.hasNext())
        {
            fac_ref = (FacilityReference)facs_it.next();  // the key which is a FacilityReference
            if (someNoPostalHistory((List)fac_index_map.get(fac_ref)))
            {
                /*
		 * at least one item has no postal history, so 
		 * consider adding those that are desirable
		 */

                //fac_ref.facility_name   is name that will be listed
                inner2 = ((List)fac_index_map.get(fac_ref)).iterator();
                while (inner2.hasNext())
                {
                    ref2 = (ExactReference)inner2.next();
                    if (ref2.nopost)
                    {
                        // ONLY include inner items that have no postal history
                        /*
                         * must build a reference that has facility in it, to
                         */
                        the_ref2 = new ExactFacilityReferenceComparableByCity(
                            ref2.state_abbrev,ref2.state_name,ref2.city_name,
                            createFacilityName(fac_ref,FOR_TEXT,false)); 
			nof_by_city.put(the_ref2,null); // key is by state,city,facility, no content
                                
                    } // end if inner item has no postal history
                } // end for each facility (NO) with same name
		} // end someone in the list has NO postal history
	} // end loop on all facilities, and gather NOF items
	/*
	 * at this point, nof_by_city is a Map containing
	 *	ExactFacilityReferenceComparableByCity
	 * objects
	 *
	 * when read back, we will have a list of these ordered by
	 * state, city and facility
	 * 
	 */
// debug System.out.println("NOF size: " + nof_by_city.size());
        Iterator city_it = nof_by_city.keySet().iterator();
                
        while (city_it.hasNext())
        {
            the_ref2 = (ExactFacilityReferenceComparableByCity)city_it.next(); 
        } // end for each facility (NO) index item ordered by state,city
	return nof_by_city;
    } // end create full facility with no postal history index ordered by state, city

} // end bookutils
