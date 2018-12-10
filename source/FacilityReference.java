/*
 * last edited October 31, 2012
 */
public class FacilityReference extends AbstractFacility
    {
        public FacilityReference(String na, int nu)    // , boolean nopost)
        {
            // caller specified number, so we assume numbered facility
            facility_name = na;
            facility_number = nu;
            facility_type = BookUtils.NUMBERED_FACILITY;  
  //          no_postal_history = nopost;
        }

/*
public FacilityReference(String na, int nu)
        {
            this(na,nu,true);  // default is NONE SEEN
        }
        */
        
        public FacilityReference(String na)
        {
            /*
             * if facility name contains #, it is a numbered facility
             * we ASSUME that the starting string is something like "general hospital"
             * and only numerics follow the # sign
             */
            int position = na.indexOf("#");
            if (position >= 0)
            {
                facility_name = na.substring(0,position);
                facility_number = Integer.parseInt(na.substring(position + 1)); // digits following pound
                facility_type = BookUtils.NUMBERED_FACILITY;  
            }
            else
            {
                facility_name = na;
                facility_number = 0; // for now, unnumbered
                facility_type = BookUtils.UNNUMBERED_FACILITY;  
            }
//            no_postal_history = nopost;
        }
        
/*
public FacilityReference(String na)
        {
            this(na,true); // default is NONE SEEN
        }
        */
        
        public String toString()
        {
            StringBuffer result = new StringBuffer(20);
            result.append("Name: " + facility_name);
            switch (facility_type)
            {
                case BookUtils.UNNUMBERED_FACILITY:
                {
                    break; // nothing special
                }
                case BookUtils.NUMBERED_FACILITY:
                {
                    result.append("  #" + String.valueOf(facility_number));
                }
            }
        /*    if (no_postal_history)
            {
                result.append(" [NONE]");
            }
          */
            return result.toString();
        }  // end to string
    
        public int compareTo(Object o)
        {
            if (o instanceof FacilityReference)
            {
                FacilityReference them = (FacilityReference)o;  // cast it
                int result = this.facility_name.compareTo(them.facility_name);
                if (result != 0)
                {
                    return result;   // name did not equal, we're outta here
                }
                result = this.facility_type - them.facility_type;
                if (result < 0)
                {
                    return -1;   // type did not equal, we're outta here
                }
                if (result > 0)
                {
                    return 1;   // type did not equal, we're outta here
                }
                result = this.facility_number - them.facility_number;
                if (result < 0)
                {
                    return -1;   // number did not equal, we're outta here
                }
                if (result > 0)
                {
                    return 1;   // number did not equal, we're outta here
                }
                return 0; // everyone matched
            }
            else
            {
                // wrong kind of compare!
                return -1;  // causes trouble
            }
        } // end comparitor
        public boolean equals(Object o)
        {
            if (o instanceof FacilityReference)
            {
                FacilityReference them = (FacilityReference)o;  // cast it
                return (this.facility_name.equals(them.facility_name)) &&
                (this.facility_number == them.facility_number) &&
                    (this.facility_type == them.facility_type);
            }
            else
            {
                // wrong kind of compare!
                return false;  // causes trouble
            }
        } // end comparitor
  
    } // end facility reference
