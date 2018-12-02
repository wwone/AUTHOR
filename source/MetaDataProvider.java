public interface MetaDataProvider
{
    /*
     * Updated 5/29/2018
     *
     * Now that all project-agnostic, format-specific
     * objects are made generic, we need to
     * inject metadata into the boilerplate that
     * is specific to each project at runtime (not during
     * compilation, or embedded in a special format- and project-specific
     * JSON file).
     *
     * Methods:
     *
     *  getMetaData -- passes key to project-specific
     *     metadata JSON file, returns a pointer to the
     *     JSON objects containing that information.
     *     This is needed to get things started, and the
     *     return value may be of interest to the SINK code
     *     because it may inject project-specific metadata
     *     on its own (example: EPUB content.opf file)
     *
     * modifyMetaData -- actually invokes modification
     *    of the existing boilerplate inside the format-specific
     *    object, so that it will be project-specific
     */
    
    public void modifyMetaData() throws Exception;
    
    public Object getMetaData(String json_file) throws Exception;
    
    	public String specialReplacementProcessing(ReplacementString rval);

	/*
	 * allow a Sink (or its helpers) to get a particular 
	 * PROJECT key value
	 */
	public ReplacementString getProjectKeyValue(String key) throws Exception;

    
} // end MetaDataProvider interface
