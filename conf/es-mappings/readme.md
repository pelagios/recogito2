# ElasticSearch Mapping - Notes & Open Issues

## Annotations

* What about comments on the annotation (current solution) vs. comments on a specific body ("This
  transcription is a transliteration", "This transcription is according to Parsons")?
  * Damien seems to need both versions comments on bodies
  * Most common use case seems to be "doubt" (Cat - on image transcriptions; but might be on
    georesolutions, too?)
* ~~Which bodies are allowed to be multi-valued? Which ones unique? (Note: QUOTE unique? Everything else - PLACE, PERSON, TAG,
  TRANSCRIPTION - multivalued?)~~
* ~~'Bodies' can be very different in nature (commentary vs. semantic tag vs. combinations). The
  ElasticSearch mapping has to be a superset to all of those. No way around that though (and
  probably not a big deal anyway)~~
* ~~How do we avoid document IDs getting out of sync between Postgres and ES?~~
* ~~How do we deal with status? Specical property of the annotation, or regular part?
  (Note that we'll want to track *who* set the status can be != creator!)~~
* ~~The annotation 'top level' represents just the 'anchored selection'. Everything that's
  the actual annotation - place or person tag, freeform tag, commentary, transcription - is
  attached as a list (since there can be multiple). How do we name the things in that list?
  'Parts'? 'Bodies'? ...?~~
* ~~What are the possible types of bodies? - COMMENT, TRANSCRIPTION, TAG, PLACE, PERSON (we want
  specific types, not just 'NAMED_ENTITY')~~
* ~~Which ones are (can?) be the result of a user intervention, i.e. need their provenance
  tracked?~~

## Annotation History

Basically just keeps a record of all versions of an annotation. The modified annotation
is stored as a serialized, non-indexed JSON string. For search/restore, all we need to
know is who created each version, and when.

Q: does this really cover all our needs? Scenarios we want to support are:

* Revert to a number of annotations (e.g. all on a document or filepart) to a state at
  a specific date.
* On a specific document (or filepart) roll back all edits made by a specific user.
* Other scenarios?
