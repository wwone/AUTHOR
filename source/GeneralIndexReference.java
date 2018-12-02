    public class GeneralIndexReference extends ExactReference
    {
        /*
         * the unique reference number should be sortable, so
         * we will make it an int with leading 0's and 4 digits
         */
        public String ref_number;
        
        public GeneralIndexReference(String ab, String na, String ct, int ref)
        {
            super(ab,na,ct);
//            ref_number = String.valueOf(ref);
            ref_number = String.format("%04d",ref);
        }
        
        public String toStringolder()
        {
            return "abbrev: " + state_abbrev + ", name: " + BookUtils.eT(state_name) + 
", city: " + BookUtils.eT(city_name) +
                ", ref: " + ref_number;
        }
        
        public String toString()
        {
            return "abbrev: " + state_abbrev + ", name: " + state_name + 
		", city: " + city_name +
                ", ref: " + ref_number;
        }
    } // end general index reference container
    
