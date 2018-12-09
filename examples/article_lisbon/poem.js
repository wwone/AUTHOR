//
// updated 5/23/2015 for phones
//
        var the_size = 35; // default
        var font_position_default = 4; // default
        //
        // if on narrow window (phone), use smaller font
        // THIS IS DEPENDENT on the CSS no longer
        // using a 
        //
        if (window.innerWidth <= 700)
        {
            the_size = 23.1; // from array that follows
            font_position_default = 3; // down one size
        }
        var font_position = font_position_default; // start with default, will change
        var fontArray = new Array(5,10,15.25,23.1,35,46.5,62,82.3,109.5,145.6);
        var len = fontArray.length; // max, min is zero
        var plus_button = null;
        var minus_button = null;
        
        
        function final_cleanup()
        {
        // work with items that are now present
        //
        plus_button = document.getElementById('plus');
        minus_button = document.getElementById('minus');
  
        mod = document.getElementById('mod');
        op = document.getElementById('op');
// set fontsize as determined earlier in the Javascript
        mod.style.fontSize = the_size + 'px';

        plus.onclick = function () {
       // alert("plus: " + font_position);
        font_position++;  // make bigger maybe
        if (font_position >= len)
        {
        font_position = len - 1;  // top at max
        }
        the_size = fontArray[font_position];
        mod.style.fontSize = the_size + 'px';
        op.innerHTML = the_size + 'px';
        }
    
        minus.onclick = function () {
       // alert("minus: " + font_position);
        font_position--;  // make smaller maybe
        if (font_position < 0)
        {
        font_position = 0;  // min is zero position
        }
        the_size = fontArray[font_position];
        mod.style.fontSize = the_size + 'px';
        op.innerHTML = the_size + 'px';
        }
    
        reset.onclick = function () {
        // alert("reset: " + font_position);
        font_position = font_position_default;
        the_size = fontArray[font_position];
        mod.style.fontSize = the_size + 'px';
        op.innerHTML = the_size + 'px';
        }
} // end final cleanup

function makea(href,text)
{
    return '<a href="' + href + '">' + text + '</a>';
}


        function write_controls1(extra_items)
        {
// 
// write the first control HTML at the start of the page
            // OPTIONALLY add any HTML just after the buttons.
            // This allows table of contents, or something else
            // that lives with the buttons
//
        document.write('<div style="align=' + "'center'; margin='0';" + '">');

          document.write('<p>Font size: <span id="op">' + the_size + 'px</span></p>');
          document.write('<p>');
          
            document.write('<input type="button"  value="SMALLER - " id="minus"/>');
            document.write('<input type="button"  value="DEFAULT " id="reset"/>');
            document.write('<input type="button"  value="LARGER + " id="plus"/>');
          if (extra_items)
          {
              document.write(extra_items);
          }
            document.write('</p>');

        document.write('</div>');
            document.write('<div class="Component" id="mod">');

            // title follows (let it be HTML for search engine purposes)

        } // end write controls at top of page, part 1
        
        function write_controls2(ddate)
        {
            // finish writing the control HTML at the start of the page
            //
            // AFTER TITLE has been coded in HTML
            //

            document.write('<p><span style="font-size: 80%; font-weight: bold;">[Use the buttons above to make information larger  or smaller]</span>.');

            document.write('</p><p><span style="font-size: 30%">' + ddate + '</span><br/><br/></p>');
//
            // we figure that the date is of no information for search engines

        } // end write controls at top of page, part 2
        
