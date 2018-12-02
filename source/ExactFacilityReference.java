    /*
     * container object used to hold city/state/state_abbrev/facility info
     *
     */
       
    public class ExactFacilityReference extends ExactReference
    {
/* inherited        public String state_abbrev;
        public String state_name;
        public String city_name;
        */
        
        public String fac_name;
        
        public ExactFacilityReference(String ab, String na, String ct, String fc)
        {
            super(ab,na,ct);
            fac_name = fc;
        }
        
        public String toString()
        {
            return "facility: " + fac_name + ", abbrev: " + state_abbrev + ", name: " + BookUtils.eT(state_name) + 
", city: " + BookUtils.eT(city_name);
        }
    } // end exactfacilityreference
