import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.io.PrintWriter;
import java.io.File;

// for JSON processing

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/*
* Updated 3/24/2018
*
* This container object has the information
* about the project output desired by the user.
* 
* Option values are present, and can be set and
* retrieved.
* 
* The source of this information is the GENERAL settings
* in the "options.json" file. Defaults can be set by 
* specification of the final document type, such as BOOK:
*
* Note that format-specific options are also specified
* in the options.json file. Those are only used by
* the Sinks that need them. This object does not
* deal with those values.
*
*/

public class Options
{

	// types of products we can make
	public static final int BOOK_TYPE = 0;
	public static final int PRESENTATION_TYPE = 1;
	public static final int ARTICLE_TYPE = 2;
	public static final int NEWSLETTER_TYPE = 3;

	// flags and strings that specify them

	public int[] found_types = {
		BOOK_TYPE,
		PRESENTATION_TYPE,
		ARTICLE_TYPE,
		NEWSLETTER_TYPE
	};

	public String[] found_strings = {
		"BOOK",
		"PRESENTATION",
		"ARTICLE",
		"NEWSLETTER"
	};

	// types of indexes we can append to the product
	// public final static int INDEX_NONE  = 0; EMPTY value means no index
	public final static int INDEX_STATES  = 1;
	public final static int INDEX_CITIES  = 2;
	public final static int INDEX_FACILITIES  = 4;
	public final static int INDEX_GENERAL  = 8;
	public final static int INDEX_NO_POSTAL_HISTORY  = 16;
	public final static int INDEX_ABBREVIATIONS  = 32;
	public final static int INDEX_ALL  = 127; // all possible index types

	/*
	 * variables containing flags for the options
	 */
	public int project_type = BOOK_TYPE; // default book

	public boolean set_introduction_first = true; // does user want first text to be Introduction?
	public boolean want_TOC = true; // does user want a TOC?
	public boolean want_cover_page = true; // does user want a cover page?
	public boolean want_title_page = true; // does user want a title page?
	public boolean want_front_material_page = true; // does user want a front matter page?
	public boolean want_preface_page = true; // does user want a preface (format-specific) page?
	public int index_type = 0; // start empty, will fill in depending on constructor

	/*
	 * repository of options that drive this object
	 * created in the getMetaData method AFTER instantiation
	 */
	Properties options = null;

	/*
	 * constructor given a project type
	 * default values will be used!
	 */
	public Options(int the_type) throws Exception
	{
		project_type = the_type; // make global
		switch (the_type)
		{
			case BOOK_TYPE:
			{
				set_introduction_first = true;
				want_TOC = true;
				want_cover_page = true;
				want_title_page = true;
				want_front_material_page = true;
				want_preface_page = true;
				index_type = INDEX_ALL;
			}
			case PRESENTATION_TYPE:
			{
				set_introduction_first = false;
				want_TOC = false; // created separately using boilerplate
				want_cover_page = false;
				want_title_page = false; // created by user
				want_front_material_page = false;
				want_preface_page = false;
				index_type = 0; // no index
			}
			case ARTICLE_TYPE:
			{
				set_introduction_first = false;
				want_TOC = false; // not for article
				want_cover_page = true; // user must supply
				want_title_page = false; 
				want_front_material_page = false;
				want_preface_page = false;
				index_type = INDEX_GENERAL; // only general index
			}
			case NEWSLETTER_TYPE:
			{
				set_introduction_first = true;
				want_TOC = true; // point to all articles in newsletter
				want_cover_page = false;
				want_title_page = false; 
				want_front_material_page = false;
				want_preface_page = false;
				index_type = INDEX_GENERAL; // only general index
			}
			default:
			{
				throw new Exception("Options initiated with bad product type: " + the_type);
			}
		} // end switch on product
	} // end constructor using generic product type

	public Options(String the_file) throws Exception
	{
		options = new Properties();
		System.out.println("Getting JSON from: " + the_file);
		File input = new File(the_file);
		ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
		Map<String,Object> userData = mapper.readValue(input,Map.class);
        
		// userData is a Map containing the named arrays
        
		List general_options = (List)userData.get("general_options"); 
	
		/* 
		 * the array must be even, each odd item is key, even
		 * value is the value. These are stored in the Map
		 */
		Object someobject = null;
		String key = "";
		List arr = (List)general_options;
		//System.out.println("Boilerplate strings:" + arr.size());
		if (arr.size() %2 != 0)
		{
		    throw new Exception("Problems with JSON, general options list is not even sized: " + 
					arr.size());
		}
		for (int ii = 0 ; ii < arr.size() ; ii += 2)
		{
		    someobject = arr.get(ii);
		    if (!(someobject instanceof String))
		    {
			throw new Exception("Problem with general_options at: " + ii + 
					    " not string: " + someobject);
		    }
		    key = (String)someobject; // will be key
		    someobject = arr.get(ii + 1);
		    if (!(someobject instanceof String))
		    {
			throw new Exception("Problem with general_options at: " + (ii+1) + 
					    " not string: " + someobject);
		    }
		    options.setProperty(key,(String)someobject); // populate the key value pairs
		} // end loop on the strings in general_options
	//        System.out.println("KeyValues: " + key_values);  // debugging

		/*
		 * now that options are entered, we set the internal
		 * flags, depending on the string key/value pairs. 
		 * If any key is not specified in the JSON, the 
		 * DEFAULT is allowed to remain as set above. 
		 */

		// product type first
		String typex = options.getProperty("DOCUMENT_TYPE", "BOOK");

		boolean found_something = false;

		for (int inner = 0 ; inner < found_types.length ; inner++)
		{
			if (typex.equals(found_strings[inner]))
			{
				project_type = found_types[inner]; // MATCH
				found_something = true;
				break;
			}
		} // end loop on all possible products
		if (!found_something)
		{
			throw new Exception("Bad Product name in JSON: " +
				typex);
		}

		// now TOC

		String tocx = options.getProperty("TOC", "yes");
		want_TOC = BookUtils.getBoolean(tocx);

		// now title page flag

		String titlex = options.getProperty("title_page", "yes");
		want_title_page = BookUtils.getBoolean(titlex);

		// now cover page

		String coverx = options.getProperty("cover_page", "yes");
		want_cover_page = BookUtils.getBoolean(coverx);

		// now front material page

		String frontx = options.getProperty("front_page", "yes");
		want_front_material_page = BookUtils.getBoolean(frontx);

		// now preface page

		String prefx = options.getProperty("preface_page", "yes");
		want_preface_page = BookUtils.getBoolean(prefx);

		// now want to start by default with Introduction section

		String introx = options.getProperty("start_with_introduction", "yes");
		set_introduction_first = BookUtils.getBoolean(introx);

		/*
		 * Index flags are special processing
		 */
		String indexx = options.getProperty("index", "all");

		// first split out the values by ":"
		String xx[] = indexx.split(":"); // 1 (one) colon delimiter
		for (int inner  = 0 ; inner < xx.length ; inner++)
		{
			System.out.println("Index type: " + index_type);
			System.out.println("Index request: " + xx[inner]);
		    if (xx[inner].equalsIgnoreCase("none"))
		    {
			index_type = 0;
			break; // out of loop, ALL other values specified are ignored
		    }
		    if (xx[inner].equalsIgnoreCase("all"))
		    {
			index_type =  127; // set all the dumb way
			break; // out of loop, ALL other values specified are ignored
		    }
		    if (xx[inner].equalsIgnoreCase("general"))
		    {
			index_type |= INDEX_GENERAL;
			continue;
		    }
		    if (xx[inner].equalsIgnoreCase("states"))
		    {
			index_type |= INDEX_STATES;
			continue;
		    }
		    if (xx[inner].equalsIgnoreCase("cities"))
		    {
			index_type |= INDEX_CITIES;
			continue;
		    }
		    if (xx[inner].equalsIgnoreCase("facilities"))
		    {
			index_type |= INDEX_FACILITIES;
			continue;
		    }
		    if (xx[inner].equalsIgnoreCase("nopostalhistory"))
		    {
			index_type |= INDEX_NO_POSTAL_HISTORY;
			continue;
		    }
		    if (xx[inner].equalsIgnoreCase("abbreviations"))
		    {
			index_type |= INDEX_ABBREVIATIONS;
			continue;
		    }
		    /*
		     * flag not recognized, sorry
		     */
		    throw new Exception("Index Flag Value Not Recognized: " + xx[inner]);
		} // end loop all flag values requested
    
	} // end constructor that reads JSON file

	public boolean wantAnyIndex()
	{
		return index_type != 0; // false if zero, indicating no index
	}

	public boolean wantStateIndex()
	{
		return (index_type & INDEX_STATES) != 0; 
	}

	public boolean wantCityIndex()
	{
		return (index_type & INDEX_CITIES) != 0; 
	}

	public boolean wantFacilityIndex()
	{
		return (index_type & INDEX_FACILITIES) != 0; 
	}

	// special case if General only (commonly for article, newsletter, simple books)
	public boolean wantGeneralIndexONLY()
	{
		return (index_type == INDEX_GENERAL);
	}

	public boolean wantGeneralIndex()
	{
		return (index_type & INDEX_GENERAL) != 0; 
	}

	public boolean wantNoPostalHistoryIndex()
	{
		return (index_type & INDEX_NO_POSTAL_HISTORY) != 0; 
	}

	public boolean wantAbbreviationIndex()
	{
		return (index_type & INDEX_ABBREVIATIONS) != 0; 
	}

	public boolean wantIntroductionByDefault()
	{
		return set_introduction_first;
	}

	public boolean wantTOC()
	{
		return want_TOC;
	}

	public boolean wantCoverPage()
	{
		return want_cover_page;
	}

	public boolean wantTitlePage()
	{
		return want_title_page; 
	}

	public boolean wantFrontMaterialPage()
	{
		return want_front_material_page;
	}

	public boolean wantPrefacePage()
	{
		return want_preface_page;
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("\nOptions for project: ");
		switch (project_type)
		{
			case BOOK_TYPE:
			{
				sb.append("BOOK");
				break;
			}
			case PRESENTATION_TYPE:
			{
				sb.append("PRESENTATION");
				break;
			}
			case ARTICLE_TYPE:
			{
				sb.append("ARTICLE");
				break;
			}
			case NEWSLETTER_TYPE:
			{
				sb.append("NEWSLETTER");
				break;
			}
		} 
		sb.append(":\n\n");
		sb.append("First Text Default to Introduction?: ");
		sb.append(String.valueOf(set_introduction_first));
		sb.append("\nTOC Included?: ");
		sb.append(String.valueOf(want_TOC));
		sb.append("\nCover Page Included?: ");
		sb.append(String.valueOf(want_cover_page));
		sb.append("\nTitle Page Included?: ");
		sb.append(String.valueOf(want_title_page));
		sb.append("\nFront Material Page Included?: ");
		sb.append(String.valueOf(want_front_material_page));
		sb.append("\nFront Preface Page Included?: ");
		sb.append(String.valueOf(want_preface_page));
		sb.append("\n");
		if (index_type == 0)
		{
			sb.append("NO Indexes Included\n");
		}
		else
		{
			//sb.append("Include Index Types: ");
			sb.append("Include Index Types (" +
				index_type + "): ");
			if ((index_type & INDEX_GENERAL) != 0)
			{
				sb.append("General, ");
			}
			if ((index_type & INDEX_STATES) != 0)
			{
				sb.append("States, ");
			}
			if ((index_type & INDEX_CITIES) != 0)
			{
				sb.append("Cities, ");
			}
			if ((index_type & INDEX_FACILITIES) != 0)
			{
				sb.append("Facilities, ");
			}
			if ((index_type & INDEX_NO_POSTAL_HISTORY) != 0)
			{
				sb.append("No Postal History Facilities, ");
			}
			if ((index_type & INDEX_ABBREVIATIONS) != 0)
			{
				sb.append("Abbreviations, ");
			}
			sb.append("\n");
		}
		return sb.toString();
	} // end to string
    
	/*
	 * HERE HERE WANT TO ADD SETTERS????
	 */
} // end options
