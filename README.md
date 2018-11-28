# AUTHOR
AUTHOR is a system for creating multiple-format electronic documents from a simple text input. The simplest book can be created without special markup. For more complex books, such as those containing multiple images, additional markup is needed. In addition to the simple text input, the author eventually needs to add metadata to be embedded into the electronic products.

The AUTHOR system is written in Java and runs on any operating system that supports Java. No special new
Java features are required.

In its current state, the execution scripts are written as Linux/Unix shell scripts.

To convert the output to certain formats, the user should have a copy of the "Calibre" program
running on their computer.

Metadata input is done through JSON files, which requires a little care in setting up. Examples
are provided.

This system has been successfully used to publish e-books, some of which are over 1,000 pages
in length with hundreds of illustrations. The system can also be used to create
on-line presentations, which use the "reveal.js" web system. Such presentations run much
like PowerPoint, but with only a browser. 

Output from this system is available as:

Interactive PDF

Print PDF

Kindle (AZW3)

EPUB (converted from Kindle using Calibre)

HTML

"reveal.js" online presentation

Most formatting material is embedded in JSON files, so it is possible to
customize most output. For more intricate work, a knowledge of Java is
required.

Source will be provided here on GITHub as Java files, compilation scripts,
JSON example files, and execution scripts.
