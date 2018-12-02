Scripts reside in this folder.

They will include:

1) Compilation script for Java source (not usng Ant at this time...)
    "compile.sh" performs this action. Read the script to note some
    of the dependencies, such as "Jackson" JAR files. Some objects are
    explicitly compiled, since they are dynamically loaded at runtime

2) Execution script prototype (examples will include AUTHOR source, images, and execution script)

3) Scripts used to execute  programs that create the final destination files, such as "kindlegen" for Kindle, "fop" for FOP (PDF)
