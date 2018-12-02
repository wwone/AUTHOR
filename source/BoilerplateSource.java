public interface BoilerplateSource
{
    /*
     * Updated 6/25/2014
     *
     * This interface is for the special content
     * creator object. It will offer a method to look
     * at the boilerplate data and supply a string,
     * when passed a string. These strings are useful
     * for formatting the output formats, such as FOP,
     * or HTML.
     *
     *
     * Methods:
     *
     *  gT -- get text, when passed a key text (this name
     *     IS VERY SIMPLE, since it has to be typed a LOT)
     * 
     */
    
    public String gT(String key) throws Exception;
}
