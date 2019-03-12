This directory contains the images used in the 1,000 page
example e-document. The same 10 images are used repeatedly,
which should save some space. Of course, all 1,000 instances
are embedded in the final output PDF, Kindle, and EPUB
e-documents.

- - - - - - -

In addition to the images, this directory contains
the files needed to create the Kindle version of the
1,000 page e-document.

NOTE that when creating Kindle, it will be necessary
to set up a "x1000" subdirectory to this one in
your working area, and copy all of the JPG files 
currently here, into the new subdirectory.

Once all files are present, the "createKindle.sh" shell
script will invoke the "kindlegen" creator. Your
installed configuration will be DIFFERENT from mine,
so the script will have to be altered to point
to the Kindle creator pacakge (from Amazon)
that you have installed on your computer.
