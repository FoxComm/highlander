package responses

import models.search._

object SearchResponses {

  case class SearchFieldRoot(name: String, analyzer: String, `type`: String)

  case class SearchIndexRoot(name: String, fields: Seq[SearchFieldRoot])

  object SearchIndexRoot {
    def fromModel(searchIndex: SearchIndex, fields: Seq[SearchField]): SearchIndexRoot = {
      SearchIndexRoot(name = searchIndex.name, fields = fields.map { f ⇒
        SearchFieldRoot(name = f.name, analyzer = f.analyzer.toString, `type` = f.`type`.toString)
      })
    }
  }

}
