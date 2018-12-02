    /*
     * container object used to hold city/state/state_abbrev/facility info for conversion to HTML links
     * this version implements Comparable, so it can be a key in
     * a TreeMap
     */
       
    public class ExactReferenceComparable extends ExactReference implements Comparable
    {        
        public ExactReferenceComparable(String ab, String na, String ct)
        {
            super(ab,na,ct);
        }
        
        public ExactReferenceComparable(ExactReference ex)
        {
            super(ex.state_abbrev,ex.state_name,ex.city_name);
        }
        
        public int compareTo(Object ob)
        {
            int result = 0;

            if (ob instanceof ExactReferenceComparable)
            {
                ExactReferenceComparable them = (ExactReferenceComparable)ob;  // cast it
                if ( (this.city_name != null) && (them.city_name != null) )
                {
                    // city names not null, so we can test them for greaterthan/lessthan/equal
                    //System.err.println("comparing this city: " + this.city_name + "   them: " + them.city_name);
                    result = this.city_name.compareTo(them.city_name);
                    if (result != 0)
                    {
                        return result;   // name did not equal, we're outta here
                    }
                    //
                    // hmmm, city name same, compare state name (never null)
                    //
                    //System.err.println("comparing this state: " + this.state_name + "   them: " + them.state_name);
                    return this.state_name.compareTo(them.state_name);
                }
                //
                // fell through, so one or both of the city names is null
                //
                if ( (this.city_name == null) && (them.city_name == null) )
                {
                    // no cities involved, compare state names
                    //
                    //System.err.println("comparing this: " + this + "   them: " + them);
                    //Sy/stem.err.println("comparing this state: " + this.state_name + "   them: " + them.state_name);
                    return this.state_name.compareTo(them.state_name);
                }
                //
                // both are neither null nor non-null, so one is and one isnt
                //
                //System.err.println("comparing this city: " + this.city_name + "   them: " + them.city_name);
                if (this.city_name == null)
                {
                    return -1; // them is "larger"
                }
                return 1; // we are "larger"
            } // end if proper object
            return -1; // wrong object, make mess
        } // end comparitor

        public boolean equals(Object o)
        {
            boolean result = false;

            if (o instanceof ExactReferenceComparable)
            {
                ExactReferenceComparable them = (ExactReferenceComparable)o;  // cast it
                if ( (this.city_name != null) && (them.city_name != null) )
                {
                    result = this.city_name.equals(them.city_name); // that is it
                    if (!result)
                    {
                        return result;   // name did not equal, we're outta here
                    }
                    //
                    // hmmm, city name same, compare state name (never null)
                    //
                    return this.state_name.equals(them.state_name);
                }
                //
                // fell through, so one or both of the city names is null
                //
                if ( (this.city_name == null) && (them.city_name == null) )
                {
                    // no cities involved, compare state names
                    //
                    return this.state_name.equals(them.state_name);
                }
                //
                // both are neither null nor non-null, so one is and one isnt
                //
                return false; // cant match
            } // end if proper object
            return false; // not right object, make trouble
        } // end equals
        
    } // end exactreferenceComparable
