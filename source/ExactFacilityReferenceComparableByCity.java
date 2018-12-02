    /*
     * container object used to hold city/state/state_abbrev/facility info
     * this version implements Comparable, so it can be a key in
     * a TreeMap
     */
       
    public class ExactFacilityReferenceComparableByCity
	extends  ExactFacilityReferenceComparable implements Comparable
    {        
        public ExactFacilityReferenceComparableByCity(String ab, String na, String ct, String fc)
        {
            super(ab,na,ct,fc);
        }
        
        public ExactFacilityReferenceComparableByCity(ExactFacilityReference ex)
        {
            super(ex.state_abbrev,ex.state_name,ex.city_name,ex.fac_name);
        }
        
	/*
	 * This compares by state, city, and facility
	 */
        public int compareTo(Object ob)
        {
            int result = 0;

            if (ob instanceof ExactFacilityReferenceComparableByCity)
            {
                ExactFacilityReferenceComparableByCity them = (ExactFacilityReferenceComparableByCity)ob;  // cast it
                if ( (this.state_name != null) && (them.state_name != null) )
                {
                    // state names not null, so we can test them for greaterthan/lessthan/equal
                    //System.err.println("comparing this state: " + this.state_name + "   them: " + them.state_name);
                    result = this.state_name.compareTo(them.state_name);
                    if (result != 0)
                    {
                        return result;   // name did not equal, we're outta here
                    }
                    //
                    // hmmm, state name same, compare city name (could be null?)
                    //
                    result = this.city_name.compareTo(them.city_name);
                    if (result != 0)
                    {
                        return result;   // name did not equal, we're outta here
                    }
                    //
                    // city/state names match, so we now check on facility name
                    //
                    //System.err.println("comparing this city: " + this.city_name + "   them: " + them.city_name);
                    return this.fac_name.compareTo(them.fac_name);
                } // end state names are nonnull (should ALWAYS be)
                //
                // fell through, so one or both of the state names is null
                //
                if ( (this.state_name == null) && (them.state_name == null) )
                {
                    // no states involved, compare city names
                    //
                    result = this.city_name.compareTo(them.city_name);
                    if (result != 0)
                    {
                        return result;   // name did not equal, we're outta here
                    }
                    //
                    // city(null)/state names match, so we now check on facility name
                    //
                    //System.err.println("comparing this state: " + this.state_name + "   them: " + them.state_name);
                    return this.fac_name.compareTo(them.fac_name);
                }
                //
                // both are neither null nor non-null, so one is and one isnt
                //
                //System.err.println("comparing this city: " + this.city_name + "   them: " + them.city_name);
                if (this.state_name == null)
                {
                    return -1; // them is "larger"
                }
                return 1; // we are "larger"
            } // end if proper object
            return -1; // wrong object, make mess
        } // end comparitor

	/*
	 * equals() test is the SAME as parent class
	 */
        
    } // end exactfacilityreferenceComparableByCity
