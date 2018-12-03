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
 * created 6/17/2018
 * 
 * creates instances of Sink objects, as requested 
 */
public  class SinkFactory
{
    
    public static GenericSink getSinkInstance(String field) throws Exception
    {
	return (GenericSink) Class.forName(
                            field + "Sink")
                            .newInstance();
    } 
    
}
