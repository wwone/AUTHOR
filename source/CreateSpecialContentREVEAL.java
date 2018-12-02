import java.io.PrintWriter;
import java.io.File;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayDeque;

// for JSON processing

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/*
 * Modified 5/29/2018
 *
 * WARNING NOTE, etc, this is the REVEAL formatted HTML creator
 *
 */

/*
 * Adapted from an existing Project-Agnostic, Format-Specific  object
 */


public  class CreateSpecialContentREVEAL extends HTMLContentCreator
{
    /*
     * Special creation for Reveal.js, many notes do not apply!
     *
     */
    private static final String g_file_extensionq = ".html\"";
    private static final String g_file_extension = ".html";
    
    List static_header_object = null;

    /*
     * project-specific strings that are to be replaced
     *
     * key = string to search for in boilerplate
     * 
     * value = ReplacementString object that has the
     *   replacement string, and a flag to indicate special processing
     */
    TreeMap project_keys = null; 
    
// just for HTML
    String[] special_keys = {
    	"PROJECT_FRONT_MATTER", // 0
//      "PROJECT_TITLE" should NEVER be used here
    	"PROJECT_KEYWORDS", // 1
    	"PROJECT_COPYRIGHT" // 2
    };
    
    public CreateSpecialContentREVEAL() throws Exception
    {
        /*
         * read in the data that is used by this object
         */
        Map<String,Object> userData = BookUtils.readJSON(
		this.getClass().getName(),false);
        
        // userData is a Map containing the named arrays
        
     populate_key_values ((List)userData.get("boilerplate")); 

            static_header_object = (List)userData.get("static_header");
            /*
             * JSON objects all appear to be just fine, we will unpeel
             * them when they are needed for output
             */
    } // end instantiation
    
    /*
     * nothing for now
     */
    public void createTitlePage(Object out, Object notused) throws Exception
    {
}
    
    public void createMetadata(Object out) throws Exception
    {
        // NO metadata right now, the object is null
      //  PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK
        
    } // end create metadata
    
	/*
	 * TOC content will be passed as the second
	 * object
	 */
    public void createStaticHeaders(Object out,
	Object toc_content,
	AuxiliaryInformation aux) throws Exception
    {
	createStaticHeaders(out,toc_content); // no pass through aux
	}
    public void createStaticHeaders(Object out,
	Object toc_content) throws Exception
    {
        PrintWriter pr = (PrintWriter)out; // this cast HAS TO WORK
	BookUtils.createAPage(pr,static_header_object);
	} // end make static headers
        
        public void renderIndex(PrintWriter pr, List all, int level) throws Exception
        {
// nothing for now
	}
    
        public void renderIndexList(PrintWriter pr, 
                                    List all, 
                                    boolean breadcrumbs,
                                    ArrayDeque to_top,
                                    int index_type) throws Exception
        {
}
                    
        
        public void renderIndexGroupList(PrintWriter pr, 
                                         List all, 
                                         int level,
                                         ArrayDeque to_top,
                                         int index_type) throws Exception
        {
}
    
    
        public void renderBookmarks(PrintWriter pr, List all) throws Exception
        {
        }
        
        public void createTOC(PrintWriter pr, 
                              Map all_maps,
                              List appendixes,
                              Map index_flags) throws Exception
        {
        }

   	/*     
   	 * WORK ON THIS     
   	 */     
        public void endPageSeq(
            PrintWriter pr) throws Exception
        {
            pr.print("<p >\n");
// FOLLOWING LINK SEEMS UNNEXESSARY and it doesn't go anywhere
//            pr.print("<a href=\"index" + g_file_extension + "#state_listing\">All Indexes</a></p>\n"); // back to indexes on main web page
            pr.print("<a href=\"index" + g_file_extension + "#top\">All Indexes</a></p>\n"); // back to indexes on main web page
            pr.print("<p >\n");
            pr.print("<a href=\"#top\">Table of Contents</a></p>\n");
            pr.print(gT("LINE_BREAK"));
            pr.print(gT("LINE_BREAK"));
            pr.print(gT("LINE_BREAK"));
            pr.print(gT("LINE_BREAK"));
            pr.print("</p>\n");
            /*
             * HERE HERE finish the page
             */
            pr.print("</div> <!-- skeleton container wrapper -->\n"); 
                        pr.print("</body>\n");
            pr.print("</html>\n");
        } // end endpageseq
    
        public void startFlow(
            PrintWriter pr,
            String page_number_string) throws Exception
        {
        }

	// new method uses helper in base class
	public void modifyMetaData() throws Exception
	{
		// create array of List s to be modified
		List [] to_process_list = new List[]
		{
		 static_header_object
		};
		/* 
		 * create and modify the Tree, and store
		 * the new version in our memory (not global)
		 * 
		 * the helper will also run stringReplacer on
		 * the list of List objects to be modified.
		 * 
		 */
		project_keys = processMetaData(
			special_keys,
			to_process_list,false);
	} // end modifyMetaData
	
	/*
	 * this code is unique to this format-specific object
	 * each switch() position is based on the special_key
	 * position.
	 * 
	 * return a String that will be pushed back into the
	 * JSON structure, as a complete replacement
	 * for the original string in which the special key was
	 * found
	 */
	public String specialReplacementProcessing(ReplacementString rval)
	{
		StringBuffer result = new StringBuffer();
		switch (rval.flag)
		{
			case 0:
			{
			    	// "PROJECT_FRONT_MATTER"
				result.append("<h1>"); // front is a heading
			    	String xx[] = rval.rep.split(":"); // 1 (one) colon delimiter
        			for (int inner  = 0 ; inner < xx.length ; inner++)
        			{
        				result.append(xx[inner] +
        					"<br/>" + "<br/>");
        				if (inner == 0)   // first line only
        				{
        					result.append("</h1>" +
						"<p style=\"text-align: center;\">" +
						"<br/>");

        				} // end first line only
				}  // end loop through all strings to be treated as separate lines         
				result.append("</p>");
			    	break;
			} // end 0 which is PROJECT_FRONT_MATTER
			case 1:
			{
			    	// "PROJECT_KEYWORDS"
			    	String xx[] = rval.rep.split(":"); // 1 (one) colon delimiter
        			for (int inner  = 0 ; inner < xx.length ; inner++)
        			{
        				result.append(xx[inner] +
        					","); // keywords are comma-delimited
				}  // end loop through all strings to be treated as separate lines         
				result.append("web"); // dummy for end
			    	break;
			} // end 1 which is PROJECT_KEYWORDS
			case 2:
			{
			    	// "PROJECT_COPYRIGHT"
				result.append( "<meta name=\"copyright\" content=\"" + 
			    	rval.rep + "\"/>");
			    	break;
			} // end 2 which is PROJECT_COPYRIGHT
		} // end switch on special code segments
		return result.toString();		
	}	// end specialReplacementProcessing
	
	
    public Object getMetaData(String json_file) throws Exception
    {
    		return getProjectSpecificMetaData(json_file); // use helper in base class
    }

    public ReplacementString getProjectKeyValue(String xx) throws Exception
    {
		throw new Exception("REVEAL sink does not use project values");
	}
        
}  // end create special content for reveal.js special HTML 
