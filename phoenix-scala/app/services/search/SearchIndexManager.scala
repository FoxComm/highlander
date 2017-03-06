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
import slick.driver.PostgresDriver.api._

object SearchIndexManager {

  implicit val formats = JsonFormatters.phoenixFormats

  case class SearchSettingsPayload(index: SearchIndex, attributes: Seq[SearchField])

  private def pushSettingsToConsul(index: SearchIndex, attributes: Seq[SearchField]): Boolean = {
    val payload     = SearchSettingsPayload(index = index, attributes = attributes)
    val jsonPayload = write(payload)
    ConsulApi.set(s"search/config/${index.name}", jsonPayload)
  }

  def create(payload: SearchIndexPayload)(implicit ec: EC,
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

  def update(id: Int, payload: SearchIndexPayload)(implicit ec: EC,
                                                   db: DB,
                                                   au: AU,
                                                   ac: AC): DbResultT[SearchIndexRoot] =
    for {
      indexFields ← * <~ getIndexWithFields(id)
      (index, fields) = indexFields
      _ ← * <~ doOrMeh(index.name != payload.name,
                       SearchIndexes.update(index, index.copy(name = payload.name)))
      _ ← * <~ SearchFields
           .filter(_.id.inSet(fields.map(_.id)))
           .deleteAll(onSuccess = DbResultT.unit, onFailure = DbResultT.unit)
      newFields ← * <~ SearchFields.createAllReturningModels(payload.fields.map { p ⇒
                   SearchField.fromPayload(p, index.id)
                 })
      _ ← * <~ failIfNot(pushSettingsToConsul(index, newFields), UnableToWriteToConsul)
    } yield SearchIndexRoot.fromModel(searchIndex = index, fields = newFields)

  def get(id: Int)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[SearchIndexRoot] = {
    getIndexWithFields(id).map {
      case (index, fields) ⇒
        SearchIndexRoot.fromModel(searchIndex = index, fields = fields)
    }
  }

  def pushToConsul(id: Int)(implicit ec: EC, db: DB): DbResultT[Unit] = {
    for {
      indexFields ← * <~ getIndexWithFields(id)
      (index, fields) = indexFields
      _ ← * <~ failIfNot(pushSettingsToConsul(index, fields), UnableToWriteToConsul)
    } yield {}
  }

  private def getIndexWithFields(id: Int)(implicit ec: EC,
                                          db: DB): DbResultT[(SearchIndex, Seq[SearchField])] =
    for {
      index  ← * <~ SearchIndexes.mustFindById404(id)
      fields ← * <~ SearchFields.filter(_.indexId === index.id).result
    } yield (index, fields)

}
