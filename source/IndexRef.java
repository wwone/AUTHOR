    /*
     * container object used to hold an index reference
     */
    public  class IndexRef
    {
        public String name; // for the index entry
        public int position; // position in the global index list

        public IndexRef(String nam,
                        int pos)
        {
            name = nam;
            position = pos;
        }
        
    
        public String toString()
        {
            return "IndexEntry: " + name + ",  Position: " + String.valueOf(position);
        }
        
        /*
         * format the general reference number (uniqueness id)
         * as decimal with leading zeroes 4 digits
         */
        public String getRefNumber()
        {
            return(String.format("%04d",position));
        }
  
    } // end index entry
