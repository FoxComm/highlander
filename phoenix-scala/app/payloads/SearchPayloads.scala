package payloads

import models.search.SearchField

object SearchPayloads {

  case class FieldDefinition(name: String,
                             analyzer: SearchField.Analyzer,
                             `type`: SearchField.FieldType)

  case class SearchIndexPayload(name: String, fields: Seq[FieldDefinition])

}
