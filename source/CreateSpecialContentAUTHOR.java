import java.io.PrintWriter;
import java.io.File;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Date;

import java.text.DateFormat;



/*
 * updated 6/17/2018
 * Project-Agnostic, Format-Specific  created 4/22/2017
 * this is a dummy as most of AUTHOR output is unrelated to metadata  
 */
public  class CreateSpecialContentAUTHOR extends SpecialContentCreator
{
    
    
    public CreateSpecialContentAUTHOR() throws Exception
    {
    } // end instantiation
    
    public void createTitlePage(Object out) throws Exception
    {
	// passed the PrintWriter

	PrintWriter pr = (PrintWriter) out; // cast
// object name will change when using newer refactor
	pr.println("BOOK:Title Here is ignored::CreateTitlePageGreatLakes::resources_name=Bluejacket Mail::general");
	Date today = new Date(); // use date/time within second
	DateFormat form = DateFormat.getDateTimeInstance(
			DateFormat.SHORT,
			DateFormat.SHORT);
	String printed_date = form.format(today);
	pr.println("!: updated " + printed_date);


    } // end create title page
    public void createTitlePage(Object out, Object x) throws Exception
    {
createTitlePage(out);
}

    public void createTableOfContents() throws Exception
    {
    } // end create table of contents page
       
    
    public void createMetadata(Object out) throws Exception
    {
    } // end create metadata
    
    public void createStaticHeaders(Object out) throws Exception
    {
    } // end make static headers
public void    createStaticHeaders(Object y,Object x) 
        {
		createStaticHeaders(y,x,null);
        }
public void    createStaticHeaders(Object y,Object x,AuxiliaryInformation aux) 
        {
        }
	public String specialReplacementProcessing(ReplacementString rval)
	{
		return null;
	}
 public Object getMetaData(String json_file) 
	{
		return null;
	}
public void modifyMetaData()
	{
	}

public void renderBookmarks(PrintWriter c,List l) 
{
}
public void
renderIndex(PrintWriter p,List l,int i)
{
}

public void createTOC(PrintWriter p,Map m,List l,Map mm) 
{
}

public void startFlow(PrintWriter p,String s) 
{
}

public void endPageSeq(PrintWriter x) 
{
}

    public void renderNOFChecklist(PrintWriter pr, Map nof_by_city) throws Exception
    {
}

    public ReplacementString getProjectKeyValue(String xx) throws Exception
    {
		throw new Exception("AUTHOR sink does not use project values");
}

} // end special output AUTHOR
