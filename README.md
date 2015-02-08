# IGV-CRAM
An adaptation of IGV to work with the new CRAM format. 

CRAM Format
===========

Many people are experimenting with the new CRAM format which offers dramatically decreased 
storage requirements for short read data that is aligned to a reference (as much as 10x smaller storage 
requirements depending on your requirements). Already there are 
drop-in replacement libraries that handle it and there is enough tool support that it 
is possible to begin using CRAM in a range of pracitical scenarios.

One fly in the ointment is that the most popular tool for viewing HTS alignments, IGV, does
not support CRAM yet. Support is coming soon, but is not there yet. This fork of IGV is
intended purely as a **short term** workaround for those needing CRAM support **right now**.

This is a totally unofficial fork, and will not be maintained. It has not been created in a
a way that is conducive to on-going tracking of the main branch of IGV and will be deleted
as soon as IGV itself supports CRAM.

Building
--------

You need at least ant 1.9.x.  Copy the libraries in the "ant" folder to your ~/.ant/lib folder. Eg:

    mkdir -p ~/.ant/lib
    cp ant/*.jar ~/.ant/lib

Then simply run:

    ant -Dinclude.libs=true build-all

Running
-------

The build will create "igv.jar". The way CRAM support is patched into the legacy
Picard / Samtools API, you need to specify your reference so that CRAM files can be
decoded. This is done using a Java system property. Thus to launch IGV with CRAM 
capability, you must specify your reference like so:

    java -Dreference=<your reference.fasta>  -Xmx1g -jar igv.jar &


Loading CRAM Files
------------------

CRAM files need to be indexed to be read. Although there is a CRAM specific index
format (.crai), the Java CRAM libraries do not read this. So you actually need to create
a ".bai" reference for your CRAM files. This is done using cramtools like so:

    java -Xmx1g -jar ./lib/cramtools-2.1.jar  index  -R <your reference.fasta>  -l INFO --bam-style-index -I file.cram

Note in the above, the reference is supplied (this may not be necessary for indexing) and 
the --bam-style-index flag makes a ".bai" file instead of a ".crai".


Problems and Troubleshooting
----------------------------

Do **not** contact the IGV mailing list or forum with problems with this build. This build is unofficial and
they should not be burdened with problems that may be created by my egregious hacking of their code.
