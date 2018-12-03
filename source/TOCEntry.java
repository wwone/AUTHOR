
/*
 *
 * last edited May 29, 2017
 *
 * Make TOC items (manifests, actually) into
 * an object that anyone can get to, not just HTML
 * or HTMLKindle  sink.
 */

   public class TOCEntry
    {
        public String link;
        public String toc_title;
        
        public TOCEntry(String l, String t)
        {
            link = l;
            toc_title = t;
        }
	public String toString()
	{
		return "TOC: Link: " + link + ", Title: " +
			toc_title;
	}
    } // end TOCEntry (manifest, really)

