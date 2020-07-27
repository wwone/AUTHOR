// Javascript for general projects using AUTHOR (by Bob Swanson)
//   NOTE: this set of methods supports use of Vanilla for HTML layout
//   and therefore the currently used Vanilla CSS is referenced.
//   If the Vanilla CSS filename changes, it MUST be changed here!
//
//

var image_window_height=768;
var image_window_width=1024;

var menu_window_height=500;
var menu_window_width=300;

var info_window_height=300;
var info_window_width=800;



         function write_tf(doc,value)
         {
           if (value == 'true')
         {
           doc.write("X");
         }
         else
         {
           doc.write("&nbsp;");
         }
         }

         function write_empty(doc,value)
         {
         if (value == '')
         {
            doc.write("&nbsp;");
         }
         else
         {
            doc.write(value);
         }
         }
         
         function pop_up_image(image_name,caps,page_title)
         {
             // page_title is used in the HTML header as page title
             // 
             // caps is array of captions. if null,
             //   no printed title for caption
             //  otherwise, print all captions in array
             //
             var generator=window.open('','image','height=' + 
                                       image_window_height + ',width=' +
                                       image_window_width + ',resizable=yes,scrollbars=yes,toolbar=yes');
             //
             // caps is an array of captions (or null)
             //
             if (page_title == '--')
             {
                 generator.document.write('<html><head><title>Untitled Image</title>');
             }
             else
             {
                 generator.document.write('<html><head><title>' + page_title + '</title>');
             }
             generator.document.write('<link rel="stylesheet" href="vanilla-framework-version-2.0.1.min.css">');
             generator.document.write('<meta name="viewport" content="width=device-width, initial-scale=1"/>');
             generator.document.write('</head><body>');
		// use Vanilla div for dark background, light type
             generator.document.write('<section class="p-strip--accent"><div class="row"><div class="col-12">');

             if (caps == null)
             {
                 generator.document.write('<h2>Untitled Image</h2>');
                 // no more captions at all
             }
             else
             {
                 // one or more caption lines
                 //
                 generator.document.write('<h2>');
                 cap_len = caps.length;
                 for (i = 0 ; i < cap_len ; i++)
                 {
                     generator.document.write('<br/>' + caps[i] );
                 }
                 generator.document.write('<br/></h2>');
             } // some captions, make them into heads
             
             generator.document.write('<img src="pics/' + image_name + '" alt="image"/>');
		// swanson light orange for the close popup prompt
             generator.document.write('<p><br/><br/><a style="color:#f77;" href="javascript:self.close()">Close</a> the popup.</p>');
             
		// swanson added div for text and background color
             generator.document.write('</div> <!-- wide columnn --> </div> <!-- row --></section>');
             generator.document.write('</body></html>');
             generator.document.close();
         }
         
         //
         // pop up window and let it control the parent's url
         // used for indexes that stay up and alter the main windows
         //
         function pop_up_controlling_window(url)
         {
             //
             var childW=window.open('','child control','height=' + 
                                       menu_window_height + ',width=' +
                                       menu_window_width + ',resizable=yes,scrollbars=yes,toolbar=yes');
             
             //
             childW.document.location = url;  // load the desired html content
             if (childW.opener == null)
                 childW.opener == self;
             childW.document.close();
         }
         //
         // change url that the parent is displaying
         // used by the popup window that contains an index
         //
         function change_parent(url)
         {
             //
             self.opener.location.assign(url);  // make parent load something else
         }

var toc_visible=true;
var toc_collapsed=true;
var toc_height=10000;

  function removeAllChildren(xxx)
        {
            while (xxx.hasChildNodes()) 
            {
                xxx.removeChild(xxx.firstChild);
            }
        }


	// OFFICIAL design, we remove table and rebuild when clicked
         function reverse_toc()
         {
		   tocc = document.getElementById('TOC_TOP');
		//alert("TOCC: "  + tocc);
		if (toc_collapsed)
		{
			// create toc
			toc_collapsed = false;
			  table_top = document.createElement("table");
			    tocc.appendChild(table_top); // add table to div
			// TOCArray contains pairs, url then text
		     for (i = 0 ; i < TOCArray.length - 1 ; i += 4)
			{
			  table_row = document.createElement("tr");
				table_top.appendChild(table_row);
			  table_cell = document.createElement("td"); // leftmost
			   table_cell.setAttribute("width", "50%"); 
				table_row.appendChild(table_cell);
			  link = document.createElement("a");
			    newtext = document.createTextNode(TOCArray[i+1]);
			    link.appendChild(newtext); // add text to anchor
			   link.setAttribute("href", TOCArray[i]); 
				table_cell.appendChild(link);
			   if (TOCArray[i+2].undefined) 
				{
					// undefined, odd number of menu items
					// no last rightmost cell
				}
				else
				{
				  table_cell = document.createElement("td"); // rightmost
					table_row.appendChild(table_cell);
				  link = document.createElement("a");
				    newtext = document.createTextNode(TOCArray[i+3]);
				    link.appendChild(newtext); // add text to anchor
				   link.setAttribute("href", TOCArray[i+2]); 
					table_cell.appendChild(link);
				}
			} // end loop all in array
		} // end if collapsed (we make)
		else
		{
			// destroy toc
			// remove all children
			  removeAllChildren(tocc);
			toc_collapsed = true;
		} // end built, we destroy

		//alert("in reverse_toc");
         } // reverse_toc

