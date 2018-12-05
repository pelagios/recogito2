package org.pelagios.recogito.plugins.ner.mock

import java.util.{Arrays, ArrayList, List}
import org.pelagios.recogito.sdk.ner._
import scala.collection.JavaConverters._

class DummyNERPlugin extends NERPlugin {

  override val getName = "Dummy NER Plugin"

  override val getOrganization = "Pelagios Commons"

  override val getDescription = "Just for testing the selection UI"

  override val getVersion = "0.0.1"

  override val getSupportedLanguages = Seq("en").asJava

  override def parse(text: String): List[Entity] = Seq.empty[Entity].asJava
  
}