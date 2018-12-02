    /*
     * container object used to hold city/state/state_abbrev/facility info for conversion to HTML links
     *
     */
       
    public class ExactReference
    {
        public String state_abbrev;
        public String state_name;
        public String city_name;
        public boolean nopost;
        
        public ExactReference(String ab, String na, String ct)
        {
            state_abbrev = ab;
            state_name = na;
            city_name = ct;
            nopost = false; // default is that there exists postal history
        }
        
        public ExactReference(String ab, String na, String ct, boolean no)
        {
            state_abbrev = ab;
            state_name = na;
            city_name = ct;
            nopost = no;
        }
        
        public String toString()
        {
            return "abbrev: " + state_abbrev + ", name: " + BookUtils.eT(state_name) + 
", city: " + BookUtils.eT(city_name) + ", nopost: " + nopost;
        }
    } // end exactreference
