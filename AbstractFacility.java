    /*
     * container object used to hold a facility name as a key
     * we will use these as keys (only) in TreeMaps
     *
     * Note that "facility" objects are not used in most documents created for AUTHOR
     *
     * last edited October 31, 2012
     */
    public abstract class AbstractFacility implements Comparable
    {
        public String facility_name;
        public int facility_number;
        public int facility_type;  // 0 - unnumbered 1 - numbered like General Hospital
      //  public boolean no_postal_history; // true=none seen  false=something seen
    } // end abstract facility container
