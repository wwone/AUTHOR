# 
# DO NOT USE with redirection, the status of the
# PDF creation will be placed on "temp" files for viewing
#
# basic shell script, nothing special
#
# from the FOP input (made by AUTHOR), create
# a print PDF distribution file. This file should
# work with print-on-demand systems, such as lulu.com
#
# The working directory will contain the
# following:
#
# book*.foprint   FOP input files created by AUTHOR (they
#                 must be combined to make the print PDF
#                cat book*.foprint > babb_print.fo
# babb_print.fo  (FOP input after being combined from AUTHOR output)
#
# fop99.xconf    (Configuration file for FOP, expecially note
#                information about fonts used, since this is sent
#                to a printing company)
#
# following is the pathway to the installed FOP executable
# package from the Apache FOP site. The "fop" at the
# end of the script pathway is the Linux/Unix executable
# shell script. Per my testing, no other environment variables
# need to be set, besides those below. It is assumed that the
# "java" command will execute the runtime on this sytem.
#
SCRIPT=/media/bob/DATA/work/fop/fop-2.2/fop/fop
#
CONF=fop99.xconf
#
# create the complete .fo file
#
cat book*.foprint > babb_print.fo
#
# following is the only FOP-related environment variable set for
# this FOP execution
#
FOP_OPTS="-Xms400m -Xmx400m" 
export FOP_OPTS
echo $FOP_OPTS

echo sh $SCRIPT -nocs -x -c $CONF babb_print.fo babbage_book_print.pdf
sh $SCRIPT -nocs -x -c $CONF babb_print.fo babbage_book_print.pdf >ppdf_create_temp1.txt 2>ppdf_create_temp2.txt

tail ppdf*temp*txt
