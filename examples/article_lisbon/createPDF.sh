#
# from the ".fopdf" files created by AUTHOR,
# use the Apache FOP program to create
# an interactive PDF version of the article
#
# assumes that the FOP configuration file
# "fop99.xconf" is in this directory
# assumes that the Apache FOP script and
# program is accessable in this directory
#
# the script "fop" is to be executed
#
# first, create the .fo file
#
echo COPYING FO FILES for PDF

echo cat book*.fopdf into lisbon.fo

cat book*.fopdf > lisbon.fo

# the input ".fo" file is now ready to pass to
# Apache FOP
#
CONF=fop99.xconf
#
# CONFIGURATION file is NECESSARY to embed
# fonts and other stuff that is not
# necessarily in the .fo file
#
FOP_OPTS="-Xms400m -Xmx400m" 
export FOP_OPTS
echo $FOP_OPTS

echo sh fop -nocs -x -c $CONF lisbon.fo lisbon_pdf.pdf
sh fop -nocs -x -c $CONF lisbon.fo lisbon_pdf.pdf
