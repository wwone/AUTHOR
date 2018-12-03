public interface TitleCreator
{
    /*
     * Updated 5/26/2016
     *
     * This must be pretty generic, because
     * title pages and other front material
     * can be made in many ways.
     *
     * The most common usage would be to pass some
     * writer that allows this object to send out
     * a stream of text (or XML, or whatever) for
     * inclusion as the "title page" and other
     * front material.
     *
     * In the case of HTML, this might be a
     * separate HTML file with only the title
     * page content.
     *
     * In the case of EPUB, this might be a
     * separate XHTML file with title page
     * content.
     *
     * For PDF, this object might have to
     * create a separate PDF file, that
     * would be merged later into an entire
     * document.
     *
     * FO creation would just be a stream
     * of XML that will be part of the flow
     * that later creates all the PDF content.
     *
     * Methods:
     *
     *  createTitlePage -- title page, front material, preface
     *
     *  createMetaData -- depends on the format being processed,
     *     some require special prefixes on the file, such as FO
     *
     *  createStaticHeaders -- depends on format, but usually needed
     *     for EPUB and HTML, since the headers on the front of
     *     each file are repetitive. We have now added a parameter
     *     for auxiliary information
     * 
     * And so on....
     */
    
    public void createTitlePage(Object writer, Object optional) throws Exception;
    
    public void createMetadata(Object writer) throws Exception;
    
    public void createStaticHeaders(Object writer, Object optional) throws Exception;
    public void createStaticHeaders(Object writer, Object optional,
		AuxiliaryInformation aux) throws Exception;
}
