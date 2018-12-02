    /*
     * container object used to hold an index reference
     */
    public  class IndexRefComparable extends IndexRef implements Comparable
    {

        public IndexRefComparable(String nam,
                        int pos)
        {
            super(nam,pos);
        }
        
        public IndexRefComparable(IndexRef ir)
        {
            super(ir.name,ir.position);
        }
        
        public int compareTo(Object ob)
        {
            int result = 0;

            if (ob instanceof IndexRefComparable)
            {
                IndexRefComparable them = (IndexRefComparable)ob;  // cast it
                result = this.name.compareTo(them.name);
                if (result != 0)
                {
                    return result;   // name did not equal, we're outta here
                }
                //
                // hmmm, name same, compare the position
                //
                return this.position - them.position;
            } // end if proper object
            return -1; // wrong object, make mess
        } // end comparitor

        public boolean equals(Object o)
        {
            boolean result = false;

            if (o instanceof IndexRefComparable)
            {
                IndexRefComparable them = (IndexRefComparable)o;  // cast it
                result = this.name.equals(them.name);
                if (!result)
                {
                    return result;   // name did not equal, we're outta here
                }
                //
                // hmmm,  name same, compare position
                //
                return this.position == them.position;
            } // end if proper object
            return false; // not right object, make trouble
        } // end equals
  
    } // end index entry (comparable)
