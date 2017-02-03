package payloads

import models.search.SearchField

object SearchPayloads {

  case class FieldDefinition(name: String, analyzer: SearchField.Analyzer) {
    def toModel(indexId: Int): SearchField = {
      val field = SearchField(name = name, analyzer = analyzer, indexId = indexId)
      Console.err.println(s"field = $field")
      field
    }
  }

  case class CreateSearchIndexPayload(name: String, fields: Seq[FieldDefinition])

}
