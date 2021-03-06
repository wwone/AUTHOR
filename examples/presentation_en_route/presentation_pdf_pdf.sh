# 
# from the FOP input (made by AUTHOR), create
# an interactive PDF distribution file
#
# The working directory will contain the
# following:
#
# presentation_pdf.fo     (FOP previously combined from AUTHOR output)
#                one way to combine the FOP files is the
#                following command:
#                cat book*.fopdf > presentation_pdf.fo
#
# Arial*.ttf     (Arial fonts used for this example, any other
#                 may be used. However note that it will be 
#                 embedded. see the configuration file "fop99.xconf")
#                 (These files are stored in the other example folder)
# fop99.xconf    (Configuration file for FOP, expecially note
#                information about fonts used)
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
# following is the only FOP-related environment variable set for
# this FOP execution
#
FOP_OPTS="-Xms400m -Xmx400m" 
export FOP_OPTS
echo $FOP_OPTS


echo sh $SCRIPT -nocs -x -c $CONF presentation_pdf.fo presentation_pdf_pdf.pdf
sh $SCRIPT -nocs -x -c $CONF presentation_pdf.fo presentation_pdf_pdf.pdf

