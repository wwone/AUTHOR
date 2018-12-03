import java.util.Map;
import java.util.List;
import java.io.PrintWriter;

public interface TOCCreator
{
    /*
     * Updated 6/24/2014
     *
     * This interface is for the special content
     * creator object. It will offer a method to 
     * create Table of Contents pages
     *
     * The List that is passed is a key/content
     * map. The key is the type of index represented.
     * We have some already in the ongoing book creator
     * design, such as a map of general index items, or
     * a map of states. This design allows for any
     * type of map, but such maps are very implementation-
     * dependent!
     *
     */
    
    public void createTOC(PrintWriter pr, 
                          Map all_maps
                     //  examples in Map = "STATE",   TreeMap state_index_map,
                       //   "ABBREVIATIONS", TreeMap abbrev_map,
                         // TreeMap city_index_map,
             //             TreeMap fac_index_map,
               //           TreeMap general_index_map
                          , 
                          List appendexes,
                          Map index_flags
                          ) throws Exception;
}
