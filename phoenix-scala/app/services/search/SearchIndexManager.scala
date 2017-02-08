package services.search

import models.search._

import payloads.SearchPayloads._
import responses.SearchResponses._
import utils.db._
import utils.aliases._
import models.account.Scope
import utils.ConsulApi

import org.json4s.jackson.Serialization.write
import utils.JsonFormatters
import failures.ConsulFailures._

object SearchIndexManager {

  implicit val formats = JsonFormatters.phoenixFormats

  case class SearchSettingsPayload(index: SearchIndex, attributes: Seq[SearchField])

  private def pushSettingsToConsul(index: SearchIndex, attributes: Seq[SearchField]): Boolean = {
    val payload     = SearchSettingsPayload(index = index, attributes = attributes)
    val jsonPayload = write(payload)
    ConsulApi.set(s"search/config/${index.name}", jsonPayload)
  }

  def create(payload: CreateSearchIndexPayload)(implicit ec: EC,
                                                db: DB,
                                                au: AU,
                                                ac: AC): DbResultT[SearchIndexRoot] = {
    val index = SearchIndex(name = payload.name, scope = Scope.current)
    for {
      dbIndex ← * <~ SearchIndexes.create(index)
      dbFields ← * <~ SearchFields.createAllReturningModels(payload.fields.map { p ⇒
                  SearchField.fromPayload(p, dbIndex.id)
                })

      _ ← * <~ failIfNot(pushSettingsToConsul(dbIndex, dbFields), UnableToWriteToConsul)

    } yield SearchIndexRoot.fromModel(searchIndex = dbIndex, fields = dbFields)
  }

  def get(name: String)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[SearchIndexRoot] = {
    SearchIndexes.mustFindByNameWithFields(name).map {
      case (index, fields) ⇒
        SearchIndexRoot.fromModel(searchIndex = index, fields = fields)
    }
  }

}
