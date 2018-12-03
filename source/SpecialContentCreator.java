import java.io.File;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;
import java.util.Properties;


// for JSON processing

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/*
 * base class for all special content creators. There
 * is one for each type of output and each book
 *
 * Updated 5/25/2018
 *
 * CHANGE to use Jackson, but in meantime, make many
 * methods generic (not JSON-specific)
 * AND commonize as much as possible. 
 * (some common code put in BookUtils as static methods)
 *
 * fix error in single string replace
 */

public abstract class SpecialContentCreator implements TitleCreator,
    BoilerplateSource, BookmarkRenderer, IndexRenderer, TOCCreator,
    PageSequenceHandler, MetaDataProvider, NOFChecklistRenderer
{
	public Options g_options = null; // filled by the invoking object

    public Properties key_values = null; // will be filled from JSON
    
    public List project_specific_metadata = null; // filled from outside JSON


	public void setOptions(Options op)
	{
		g_options = op; // make global for content creator to use
	}

	// populate key values from List
    public void populate_key_values(List keyvalues) throws Exception
    {
        key_values = new Properties(); // empty to start
        if (keyvalues == null)
        {
            System.out.println("Boilerplate strings do not exist.");
            return;  // return with empty treemap
        }
        if (keyvalues.size() == 0)
        {
            System.out.println("Boilerplate strings empty.");
            return;  // return with empty treemap
        }
        /* 
         * the array must be even, each odd item is key, even
         * value is the value. These are stored in the Map
         */
        Object someobject = null;
        String key = "";
        List arr = keyvalues;
        // debug System.out.println("Boilerplate strings:" + arr.size());
        if (arr.size() %2 != 0)
        {
            throw new Exception("Problems with JSON, boilerplate list is not even sized: " + 
                                arr.size());
        }
        for (int ii = 0 ; ii < arr.size() ; ii += 2)
        {
            someobject = arr.get(ii);
            if (!(someobject instanceof String))
            {
                throw new Exception("Problem with boilerplate at: " + ii + 
                                    " not string: " + someobject);
            }
            key = (String)someobject; // will be key
            someobject = arr.get(ii + 1);
            if (!(someobject instanceof String))
            {
                throw new Exception("Problem with boilerplate at: " + (ii+1) + 
                                    " not string: " + someobject);
            }
            key_values.setProperty(key,(String)someobject); // populate the key value pairs
        } // end loop on the strings in boilerplate
        // System.out.println("KeyValues: " + key_values);  // debugging
    } // end populate the key-value pairs
	
    /*
     * return a value for a key. If not found, return
     * null, let the caller figure out what they
     * did wrong
     */
    public String gT(String key) throws Exception
    {
        if (key_values == null)
        {
            throw new Exception("Boilerplate is empty!");
        }
        if (key_values.containsKey(key))
        {
            return (String)key_values.get(key);
        }
        else
        {
            // not found
            throw new Exception("Boilerplate missing key: " + key);
        }
    } // gT, general string getter
    
    /*
     * to make TOC's, a general set of maps are passed. These
     * are implementation-dependent, so they are reffered to by
     * key strings. We get the desired Map, but the key must
     * exist.
     */
    
    public Map get_a_map(String key, Map all_maps, String error) throws Exception
    {
        if (!all_maps.containsKey(key))
        {
            throw new Exception(error + " Map key missing: " + key);
        }
        return (Map)all_maps.get(key);
    }

	/* 
	 * TitleCreator interface requires two entries
	 * on the createStaticHeaders, but there are
	 * many references for only 1. So, we capture
	 * one parameter calls here and pass to the
	 * two param calls with the second entry null.
	 *
	 * if a caller specifies two, the call goes right
	 * into the implementation, not here
	 */

	public void createStaticHeaders(Object first) throws Exception
	{
		createStaticHeaders(first,null);
	}

	/* 
	 * TitleCreator interface requires two entries
	 * on the createTitlePage, but there are
	 * many references for only 1. So, we capture
	 * one parameter calls here and pass to the
	 * two param calls with the second entry null.
	 *
	 * if a caller specifies two, the call goes right
	 * into the implementation, not here
	 */

	public void createTitlePage(Object first) throws Exception
	{
		createTitlePage(first,null);
	}
    
    public List getProjectSpecificMetaData(String filename) throws Exception
    {
        File input = new File(filename);
        Map<String,Object> userData = 
		BookUtils.readJSON(input,false); // no debugging msg
        
        // userData is a Map containing the named arrays
        
            project_specific_metadata = (List)userData.get("project");
    		return project_specific_metadata;
    } // end get project specific metadata

	/*
	 * can either substitute strings (NO!) or provide debugging assistance 
	 * (yes) while looking at a JSONArray of stuff
	 * this method is recursive!!
	 *
	 * We are testing every replacement string against every
	 * boilerplate string. This could be expensive, but we
	 * don't have a lot of text in the boilerplate world...
	 */
	public void metaWalk(Object a, Map project_keys, boolean replace_it) throws Exception
	{
		if (replace_it)
		{
			throw new Exception("metaWalk cannot replace strings, don't try");
		}
		String akey = "";
		String aval = "";
		if (a instanceof List)
		{
			List arr = (List)a; // cast
			Iterator ii = arr.iterator();

			while (ii.hasNext())
			{
				metaWalk((List)ii.next(),project_keys,replace_it); // recurse
			}
		}
		else
		{
			if (a instanceof String)
			{
				String test = (String)a; // cast
				if (replace_it)
				{
					/*
					 * check and perform string replacement
					 * THIS DOES NOT WORK
					 * Strings are immutable
					 * ListIterator is the correct object, but
					 * we cannot use during recursion
					 */
					 Iterator inner = project_keys.keySet().iterator(); // all search keys
					 while (inner.hasNext())
					 {
					 	akey = (String)inner.next();
					 	if (test.indexOf(akey ) >= 0)
					 	{
					 		// HIT IT!
					 		aval = (String)project_keys.get(akey);
					 		test = test.replace(akey,aval);
					 		// debug System.out.println("Replaced: " +
					 			//test);
					 	}
					 } // end check all keys (may be more than one in a line)
				} // end replace strings
				else
				{
					// debugging, just print
					// System.out.println("String to process: " +
					//	a);
				} // end print, not replace
				return; // end recursion	
			}
			else
			{
				// hmmm not String or array
				throw new Exception("Problems with JSON structure, in project.json : " + a.getClass().getName());
			} // end not array or string
		} // end not json array
 
	} // end metawalk recursion

	// replace in List object (if not a List, cast will fail)
	public void stringReplacer(Object a, Map project_keys) throws Exception
	{
	
System.out.println("stringReplacer (obj) called in special content creator");
		String akey = "";
		ReplacementString rval = null;
		String aval = "";
		if (a instanceof List)
		{
			List arr = (List)a; // cast
			// now this object contains ONLY strings
			// we must be able to replace its contents
			// after alteration. Thus we use
			// ListIterator!
			//
			ListIterator ii = arr.listIterator();
			boolean did_something = false;

			while (ii.hasNext())
			{
				String test = (String)ii.next(); // has to be
				Iterator inner = project_keys.keySet().iterator(); // all search keys
				while (inner.hasNext())
					 {
					 	akey = (String)inner.next();
					 	if (test.indexOf(akey ) >= 0)
					 	{
					 		// HIT IT!
					 		did_something = true;
					 		rval = (ReplacementString)project_keys.get(akey);
							String result = replaceAString(test,akey,rval); // get replacement, whether normal or special							
					 		ii.set(result); // overwrites current boilerplate string
						
					 	} // end if found one of the keys
					 } // end check all keys (may be more than one in a line)
			} // end pass all strings inside the list
			if (did_something) // debugging
			{
				Iterator see = arr.iterator();
				while (see.hasNext())
				{
					// if debugging uncomment these 2 lines
					//System.out.println("R: " + 
					//	see.next());
					// if NOT debugging uncomment this
					see.next();
				}
			} // end if did something, possible debug output
		} // end if correct object
		else
		{
			throw new Exception("Problems with string_replacement, internal boilerplate  : " + a.getClass().getName());
			
		}
 
	} // end stringReplacer (Object)

	/*
	 * GENERIC string replacer. Assumes that
	 * the List passed is simply Strings, and nothing else
	 *
	 * This does NOT SIT well with certain Sinks such as HTML,
	 * which uses a different structure of keys. So, such
	 * outcasts use their own stringReplacer.
	 */
	public void stringReplacer(List a, Map project_keys) throws Exception
	{
/*
System.out.println("stringReplacer (list) called in special content creator");
System.out.println("  keys: " + project_keys);
*/
		String akey = "";
		ReplacementString rval = null;
		String aval = "";
		List arr = a;
			// now this object contains ONLY strings
			// we must be able to replace its contents
			// after alteration. Thus we use
			// ListIterator!
			//
			ListIterator ii = arr.listIterator();
			boolean did_something = false;

			while (ii.hasNext())
			{
				String test = (String)ii.next(); // has to be
//System.out.println("   testing: " + test);
				Iterator inner = project_keys.keySet().iterator(); // all search keys
				while (inner.hasNext())
					 {
					 	akey = (String)inner.next();
/*
if (test.indexOf("HTML_CSS") >= 0)
{
	System.out.println("Testing: " + test + ", against: " + akey);
}
*/
					 	if (test.indexOf(akey ) >= 0)
					 	{
//System.out.println("    matched: " + akey);
					 		// HIT IT!
					 		did_something = true;
					 		rval = (ReplacementString)project_keys.get(akey);
							String result = replaceAString(test,akey,rval); // get replacement, whether normal or special							
					 		ii.set(result); // overwrites current boilerplate string
						
					 	} // end if found one of the keys
					 } // end check all keys (may be more than one in a line)
			} // end pass all strings inside the list
			if (did_something) // debugging
			{
//System.out.println("   replacement flag seen");
				Iterator see = arr.iterator();
				while (see.hasNext())
				{
					// if debugging uncomment these 2 lines
					//System.out.println("R: " + 
					//	see.next());
					// if NOT debugging uncomment this
					see.next();
				}
			} // end if some key found
		
	} // end stringReplacer List

	/*
	 * test = full string within which we will perform replacement
	 * akey = string to be replaced
	 * rval = ReplacementString object, which contains the string
	 *    that will replace "akey", and a flag that indicates
	 *     whether special processing will occur.
	 */
	public String replaceAString(String test, String akey, ReplacementString rval)
	{
			if (rval.flag < 0)
			{
				// NORMAL replacement
				String result = test.replace(akey ,rval.rep); // simple replace
				// debug System.out.println("Replaced: " +
					// 				result);
			// caller decides to do this	ii.set(result); // overwrites current boilerplate string
			  return result; // modified string
			} // end normal string replacement
			else
			{
				// SPECIAL PROCESSING, not simple string replacement
					 			
				String res2 = specialReplacementProcessing(
					 			  rval);  // replace with returned string
			// caller does this	ii.set(res2);
				// debugSystem.out.println("Special Replaced: " +
				//	 				res2);
				return res2; // modified string
			} // end special processing
	} // end replaceastring

	/*
	 * given a single String, perform the replacement
	 * that we normally do with stringReplacer. It
	 * is designed to walk only String values inside
	 * a JSON array. This one can be called
	 * by anybody who has a String to be modified
	 */
	public String singleStringReplace(String test,
	Map project_keys)
	{
		String result = test; // working copy
		String akey = "";
		//boolean did_something = false; // keep flag, could be many replacements in a line
		Iterator inner = project_keys.keySet().iterator(); // all search keys
		while (inner.hasNext())
		{
			akey = (String)inner.next();
			if (result.indexOf(akey ) >= 0)
			{
				// HIT IT!
			//	did_something = true;
				ReplacementString rval = (ReplacementString)project_keys.get(akey);
				// replace back over for future testing
				result = replaceAString(test,akey,rval); // get replacement, whether normal or special							
			} // end if found one of the keys
		} // end check all keys (may be more than one in a line)
		return result; // either copy of original string, or modified version		
	} // end single string replace
	
	/*
	 * Get properties from the "options.json". User passes
	 * a String designating property. Child object (format-specific)
	 * provides the value (or default).
	 * 
	 * Anyone who wants to use this, OVERRIDE it
	 */
	public String getProperty(String name)
	{
		return null; // must override, if you want anything useful
	}
	/*
	 * project-specific metadata processing
	 * 
	 * This is more generic code, removing a lot of 
	 * repetition from the descendents of this base class.
	 * 
	 * NOT everyone works the same, however, so we have
	 * to allow for stopping the process partway through.
	 * 
	 * We create and RETURN a modified TreeMap of metadata 
	 * (called project_keys)
	 * 
	 * We are passed an array of String called "special_keys". These keys 
	 * are unique to each Sink format. Each must match a major key,
	 * and they cause special processing. The KEY to the special
	 * processing is the POSITION in this array. 
	 * 
	 * In addition, we are passed an array of List items. Each 
	 * item in the array is a list of Strings that can be altered by
	 * the stringReplacer() method (see this object)
	 * We will execute that method on each List object instance.
	 * NOTE, this array can be null or empty!
	 * 
	 * NOTE: we use the global List "project_specific_metadata"
	 * in this method! 
	 * 
	 */            
    public TreeMap processMetaData(
		String [] special_keys,
		List [] to_process,
		boolean debug_it)  throws Exception
    {
    		/*
		 * process strings in Map, when particular metadata
	 	 * values are seen. Example PROJECT_AUTHOR would be
	 	 * replaced with "Bob Swanson" in the boilerplate.
	 	 *
	 	 * the global: project_specific_metadata
	 	 *
  		 * contains the key-value pairs
  		 *
	 	 * To do this, we walk through all objects of boilerplate
    		 * we have, replacing metadata keys with values
    		 * per that scheme.
    		 *
    		 *  Some replacements are more complex,
    		 * so the SpecialContent object we are within, will
    		 * have to do more than simple text replaces, from time to time.
    		 */

		if(debug_it)
		{
			// NOTE the project_keys Map is not yet built!
			//metaWalk(project_specific_metadata,false); 
		}
			// create key/value pairs before processing

		/*
		 * the following is created in this method
		 * populated in this method, and returned by this method
		 */
		TreeMap project_keys = new TreeMap();

		/* 
		 * the array must be even, each odd item is key, even
		 * value is the value. These are stored in the Map
		 */
		Object someobject = null;
		String key = "";
		List arr = project_specific_metadata;
		if (debug_it)
		{
			System.out.println("Project MetaData strings:" + arr.size());
		}
       	 	if (arr.size() %2 != 0)
		{
			throw new Exception("Problems with JSON, project metadata list is not even sized: " + arr.size());
		}
		for (int ii = 0 ; ii < arr.size() ; ii += 2)
		{
		someobject = arr.get(ii);
		if (!(someobject instanceof String))
		{
                	throw new Exception("Problem with project metadata at: " + ii + 
                                    " not string: " + someobject);
		}
		key = (String)someobject; // will be key
		someobject = arr.get(ii + 1);
		if (!(someobject instanceof String))
		{
			throw new Exception("Problem with project metadata at: " + (ii+1) + 
                                    " not string: " + someobject);
		}
		String replacement_value = (String)someobject;
 	           /*
 	            * look up the key to see if it requires
 	            * special processing. each SpecialCreator
 	            * child will have its own list of special processing
 	            * items. If found, we use the position in the
 	            * list as the indicator of what code to perform.
 	            */
		ReplacementString rs = null; // no match yet
		for (int inner = 0 ; inner < special_keys.length ; inner++)
		{
		// System.out.println("Testing: --" + special_keys[inner] + "-- with --" + key + "--"); // debugging, grump
			if (special_keys[inner].equals(key))
			{
 	            		rs = new ReplacementString(replacement_value,inner); // position
 	            	}
		}
		if (rs == null)
		{
			// not special
			rs = new ReplacementString(replacement_value,-1); // normal processing
		}
            	project_keys.put(key,rs); // populate the key value pairs
		} // end loop on the strings in project metadata
		if (debug_it)
		{
        		System.out.println("ProjectKeyValues: " + project_keys);  // debugging
		}

		/*
		 * at this time, we have project_keys available for substitution
		 * 
		 * we perform stringReplacer on any List that is
		 * passed to us.
		 * 
		 * NOTE: the list can be null or empty 
		 */
		if (to_process != null)
		{
			for (int inner = 0 ; inner < to_process.length ; inner++)
			{
// debug System.err.println("Going to process item: " + inner + ", List: " + to_process[inner]);
				stringReplacer(to_process[inner],project_keys); 
			}
		} // end if something to process through stringReplacer
		return project_keys; // return populated Tree to caller
	} // end process meta data

} // end base class for all special content creators
