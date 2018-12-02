    /*
     * container object used to hold an abbreviation as a key
     *
     * "abbreviation" sections are not used in most AUTHOR publications
     *
     * the comparison for these keys is rather involved
     */
    public  class AbbreviationKey implements Comparable
    {
        public String the_abbreviation;
        public String comparison_key;  // after upper/lower case and paren removal

        public AbbreviationKey(String kk)
        {
            the_abbreviation = kk;
            /*
             * when we compare abbreviations, we remove parenthesis from
             * front and end
             * then we convert to lower case, so the comparison is case-insensitive
             *
             * when done, the comparison key will remain and be reused
             * during Comparable processing
             */
            comparison_key = kk;
            if (comparison_key.indexOf("(") == 0)
            {
                // remove first and last parens
                //
                comparison_key = comparison_key.substring(1,comparison_key.length() - 1);
            }
            // everyone to lower case for case-insensitive test
            //
            comparison_key = comparison_key.toLowerCase();
        } // end constructor
    
        public String toString()
        {
            return "Abbreviation : " + BookUtils.eT(the_abbreviation) + "  Compare Key: " +
                BookUtils.eT(comparison_key);
        }
        
        public int compareTo(Object o)
        {
            if (o instanceof AbbreviationKey)
            {
                AbbreviationKey       them = (AbbreviationKey)o;  // cast it
//                System.out.println("ours: " + this_key);
  // debugg              System.out.println("theirs: " + their_key);
                return this.comparison_key.compareTo(them.comparison_key);
            }
            else
            {
                // wrong kind of compare!
                return -1;  // causes trouble
            }
        } // end compare to test
        
        public boolean equals(Object o)
        {
            if (o instanceof AbbreviationKey)
            {
                AbbreviationKey       them = (AbbreviationKey)o;  // cast it
                return this.comparison_key.equals(them.comparison_key);
            }
            else
            {
                // wrong kind of compare!
                return false;  // causes trouble
            }
        } // end equals test
  
    } // end abbreviation key
