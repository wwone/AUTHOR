// Javascript for  Bob Swanson's book
//
// updated 6/8/2017
//
// layout simplified
//
// when popping up windows, set a default height and width
//
// separate sizes for images, general information, and popup menus
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
         
        

         function pop_up_imageprevious(image_name,cap1,cap2,cap3,cap4,cap5,cap6,cap7,cap8,cap9,cap10,page_title)
         {
             // page_title is used in the HTML header as page title
             // 
             // each "cap.." is a displayed title
             //   first == "--", no printed title for caption
             //   first not = "--", print it, then check rest:
             //     any subsequent "--", don't print that line at all
             // NO MORE than 10 caption lines (controlled by code that creates
             //   the Javascript call to this function...)
             //
             var generator=window.open('','image','height=' + 
                                       image_window_height + ',width=' +
                                       image_window_width + ',resizable=yes,scrollbars=yes,toolbar=yes');
             //
             // TITLE PASSED AS PARAMETER will contain:
             // '--' to mean empty
             //
             // Can have "::" to delineate line breaks
             // CREATE Javascript that replaces :: with <br> tags
             //
             if (page_title == '--')
             {
                 generator.document.write('<html><head><title>Untitled Image</title>');
             }
             else
             {
                 generator.document.write('<html><head><title>' + page_title + '</title>');
             }
             generator.document.write('<link rel="stylesheet" href="wwi.css">');
             generator.document.write('</head><body>');
             if (cap1 == '--')
             {
                 generator.document.write('<h1 class="heading1">Untitled Image</h1>');
                 // no more captions at all
             }
             else
             {
                 generator.document.write('<h1 class="heading1">' + cap1);
                 if (cap2 != '--')
                 {
                     generator.document.write('<br/>' + cap2 );
                 }
                 if (cap3 != '--')
                 {
                     generator.document.write('<br/>' + cap3 );
                 }
                 if (cap4 != '--')
                 {
                     generator.document.write('<br/>' + cap4 );
                 }
                 if (cap5 != '--')
                 {
                     generator.document.write('<br/>' + cap5 );
                 }
                 if (cap6 != '--')
                 {
                     generator.document.write('<br/>' + cap6 );
                 }
                 if (cap7 != '--')
                 {
                     generator.document.write('<br/>' + cap7 );
                 }
                 if (cap8 != '--')
                 {
                     generator.document.write('<br/>' + cap8 );
                 }
                 if (cap9 != '--')
                 {
                     generator.document.write('<br/>' + cap9 );
                 }
                 if (cap10 != '--')
                 {
                     generator.document.write('<br/>' + cap10 );
                 }
                 generator.document.write('</h1>');
             } // some captions, make them into heads
             
             generator.document.write('<img src="pics/' + image_name + '"/>');
             generator.document.write('<p><br/><br/><a href="javascript:self.close()">Close</a> the popup.</p>');
             generator.document.write('</body></html>');
             generator.document.close();
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
             generator.document.write('<!-- <link rel="stylesheet" href="reset.css"> placeholder -->');
             
             // full-width page
             generator.document.write('</head><body>');
             if (caps == null)
             {
                 generator.document.write('<h1>Untitled Image</h1>');
                 // no more captions at all
             }
             else
             {
                 // one or more caption lines
                 //
                 generator.document.write('<h1>');
                 len = caps.length;
                 for (i = 0 ; i < len ; i++)
                 {
                     generator.document.write('<br/>' + caps[i] );
                 }
                 generator.document.write('<br/></h1>');
             } // some captions, make them into heads
             
             generator.document.write('<img src="pics/' + image_name + '" alt="image"/>');
             generator.document.write('<p><br/><br/><a href="javascript:self.close()">Close</a> the popup.</p>');
             
             generator.document.write('</body></html>');
             generator.document.close();
         }
         
         function pop_up_image_prev(image_name,caps,page_title)
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
             generator.document.write('<link rel="stylesheet" href="reset.css">');
             generator.document.write('<link rel="stylesheet" href="text.css">');
             generator.document.write('<link rel="stylesheet" href="960.css">');
             generator.document.write('<link rel="stylesheet" href="wwi.css">');
             // full-width page
             generator.document.write('</head><body><div class="container_16"><div class="grid_14 prefix_1 suffix_1">');
             if (caps == null)
             {
                 generator.document.write('<h1 class="heading1">Untitled Image</h1>');
                 // no more captions at all
             }
             else
             {
                 // one or more caption lines
                 //
                 generator.document.write('<h1 class="heading1">');
                 len = caps.length;
                 for (i = 0 ; i < len ; i++)
                 {
                     generator.document.write('<br/>' + caps[i] );
                 }
                 generator.document.write('<br/></h1>');
             } // some captions, make them into heads
             
             generator.document.write('<img src="pics/' + image_name + '"/>');
             generator.document.write('<p><br/><br/><a href="javascript:self.close()">Close</a> the popup.</p>');
             // clear and done
             generator.document.write('</div><div class="clear"></div></div>');  // end all divs
             
             generator.document.write('</body></html>');
             generator.document.close();
         }
        
        
         //
         // used to pop up windows with abbreviation information
         //
         function pop_up_window(abbrev,def)
         {
             //
             var generator=window.open('','info','height=' + 
                                       info_window_height + ',width=' +
                                       info_window_width + ',resizable=yes,scrollbars=yes,toolbar=yes');
             //
             //
             generator.document.write('<html><head><title>Abbreviation: ' + abbrev + '</title>');
             generator.document.write('<link rel="stylesheet" href="reset.css">');
             generator.document.write('<link rel="stylesheet" href="text.css">');
             generator.document.write('<link rel="stylesheet" href="960.css">');
             generator.document.write('<link rel="stylesheet" href="wwi.css">');
             generator.document.write('</head><body>');
             generator.document.write('<div class="container_16"><div class="grid_14 prefix_1 suffix_1">');
             
             generator.document.write('<h1 class="heading1">Abbreviation: ' + abbrev + '</h1>');
             generator.document.write('<p class="body">Abbreviation: ' +
                                      abbrev + '<br class="x"/>  Definition: ' + def + '</p>');
             generator.document.write('<p><a href="javascript:self.close()">Close</a> the popup.</p>');
             generator.document.write('</div><div class="clear"></div></div>');  // end all divs
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

