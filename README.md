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

Problems and Troubleshooting
----------------------------

Do **not** contact the IGV mailing list or forum with problems with this build.
