# AUTHOR
AUTHOR is a system for creating multiple-format electronic documents from a single text input. 

The simplest book can be created without special markup. For more complex books, such as those containing multiple images, additional markup is needed. 

In addition to the single text input, the document creator must  eventually  add metadata to be embedded into the electronic document output.

The AUTHOR system is written in Java and runs on any operating system that supports Java. No special new Java features are required.

In its current state, the execution scripts are written as Linux/Unix shell scripts.

See the "dependencies.txt" file for the files and applications on which successful creation of e-documents depends. For instance, to convert the output to certain formats, the user should have a running copy of the "Calibre" program installed on their computer. See https://calibre-ebook.com/

Creation of PDF output requires the installation and use of the Apache FOP processor. See https://xmlgraphics.apache.org/fop/  .

Metadata input is entered through JSON files, the use of which requires some care in setting up. Examples are provided.

This system has been successfully used to publish e-books, some of which are over 1,000 pages in length with many hundreds of illustrations. The system can also be used to create on-line presentations, which use the "reveal.js" web system. Such presentations run much like PowerPoint, but with only a browser. 

## Output from this system is available as:

 - Interactive PDF (creation is with Apache FOP)

 - Print PDF (creation is with Apache FOP)

 - Kindle (AZW3, converted using Calibre)

 - EPUB (converted  using Calibre)

 - HTML, both as a single HTML file, or multiple HTML files ( one for each SECTION    of the document). The created HTML uses the Vanilla layout system by default (see: https://vanillaframework.io/) , and contains layout  code that supports the POEM and Skeleton layouts instead.)

 - "reveal.js" online presentation system (HTML) See: http://lab.hakim.se/reveal-js/#/


Most formatting material is embedded in JSON files, so it is possible to customize most output. For more intricate work, a knowledge of Java is
required.

Source is provided here on GITHub as Java files, compilation scripts, documentation, and 4 examples of e-document creation.
