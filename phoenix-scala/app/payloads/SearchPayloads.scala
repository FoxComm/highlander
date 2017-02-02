package payloads

import models.search.SearchField

object SearchPayloads {

  case class FieldDefinition(name: String, analyzer: SearchField.Analyzer) {
    def toModel(indexId: Int): SearchField = {
      SearchField(name = name, analyzer = analyzer, indexId = indexId)
    }
  }

  case class CreateSearchIndexPayload(name: String, fields: Seq[FieldDefinition])

}
