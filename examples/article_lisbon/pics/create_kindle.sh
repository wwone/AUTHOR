#
# this script combines the necessary files, and
# then invokes the "kindlegen" program (from Amazon)
# to create a Kindle AZW3-formatted ebook.
#
# this script is located in, and is executed from, the
# "pics" directory. That is the directory that would
# normally contain the images that will be included
# in the ebook.
#
# In this example, the AUTHOR file image references specify 
# an additional level of the "pics" directory. In this case 
# the subdirectory for this "pics" directory is called "fraser". 
# Such subdirectories are used, so that multiple projects can
# be maintained in the same work area without collision
# of the image names. It is hoped that most of the other
# filenames used with AUTHOR will collide.
#
#
# BEFORE executing this script, all of the "kindle*.html"
# files created by AUTHOR must be located here and
# combined with the following command:
#
# cat kindleindex[0-9].html >kindleindex.html
#
# IN ADDITION, the following two files must be present,
# and correctly formatted. They are used by the
# "kindlegen" program.
#
# content.opf
# toc.ncx
#
# the example here contains both files, formatted
# specifically for the Fraser Lisbon article.
#
# note that when AUTHOR executes, it creates two "template" 
# files that contain nearly all of the information required by
# "kindlegen". It is the creator's responsibility to
# hand-correct the information, and ensure that the actual
# files listed above match the information created by
# AUTHOR.
#
# the template files created by AUTHOR are:
#
#  kindle_toc.ncx
#  kindle_content.opf
#
# remember, the two files listed here will NOT be used by
# "kindlegen", but their content should be checked against
# that in the files officially used (toc.ncx, content.opf),
# and corrected, if necessary.
#

# 
# following is the pathway to the "kindlegen" command
# on this system. Your pathway will not necessarily be
# the same as mine, but the definition is kept here for
# reference purposes.
#
# (The version used here is a slightly older version of
# the program, specifically ported to Linux/Unix. Generally,
# and unfortunately, the "kindlegen" program issued by
# Amazon has usually been a Windows executable. Sometimes, these
# work with "wine", and, sadly, sometimes they do not.)
#
PAT=/home/bob/bin/kindlegen/kindlegen

echo $PAT  -verbose -c2 -o article.azw3 content.opf  >temp 2>temp2
$PAT  -verbose -c2 -o article.azw3 content.opf  >temp 2>temp2

#
# the AZW3 file created is a valid Kindle ebook, and can be
# uploaded to Amazon to be published and sold. It can be
# read by the ebook reading programs provided in Calibre.
#
# In addition, Amazon offers a "Kindle Previewer" program
# which is also an operating software Kindle viewer. That
# program is supplied by Amazon to allow authors to preview
# their books and ensure that they look correctly. It can
# simulate various Kindle devices (such as EInk). The
# previewer I have used is released for Windows, but it
# will run under "wine".
#
# The AZW3 file created can be entered into Calibre. The
# benefit of that operation, is that it can then be
# converted to other formats, specifically EPUB. I have
# shed many tears, trying to create native EPUB using
# my Java code. None have worked correctly. Calibre has
# the "magic juice" and makes a valid EPUB file.
#
# Note that Calibre can create many other formats. Some
# people think that it can read PDF and make Kindle.
# Unfortunately, PDF is not the correct format in which
# to author ebooks. The conversions of this kind simply
# don't work. However, note that Calibre does quite a decent
# job of converting ebooks to HTML (HTMLZ, is their setting), 
# and this avenue could certainly be investigated by 
# budding e-publishers.
#
