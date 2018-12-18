package org.pelagios.recogito.plugins.ner.stanford

class StanfordCoreNLPEnglish extends StanfordBaseWrapperPlugin(
  "en",
  "StanfordCoreNLP.properties",
  "The standard engine with the default English language model"
)

class StanfordCoreNLPFrench  extends StanfordBaseWrapperPlugin(
  "fr",
  "StanfordCoreNLP-french.properties",
  "The standard engine with the default French language model"
)

class StanfordCoreNLPGerman  extends StanfordBaseWrapperPlugin(
  "de",
  "StanfordCoreNLP-german.properties",
  "The standard engine with the default German language model" 
)

class StanfordCoreNLPSpanish extends StanfordBaseWrapperPlugin(
  "es",
  "StanfordCoreNLP-spanish.properties",
  "The standard engine with the default Spanish language model"
)
