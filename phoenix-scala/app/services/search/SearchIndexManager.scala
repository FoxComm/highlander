package services.search

import models.search._

import payloads.SearchPayloads._
import responses.SearchResponses._
import utils.db._
import utils.aliases._
import models.account.Scope

object SearchIndexManager {

  def create(payload: CreateSearchIndexPayload)(implicit ec: EC,
                                                db: DB,
                                                au: AU,
                                                ac: AC): DbResultT[SearchIndexRoot] = {
    val index = SearchIndex(name = payload.name, scope = Scope.current)
    for {
      dbIndex ← * <~ SearchIndexes.create(index)
      dbFields ← * <~ SearchFields.createAllReturningModels(
                    payload.fields.map(_.toModel(dbIndex.id)))
    } yield SearchIndexRoot.fromModel(searchIndex = dbIndex, fields = dbFields)
  }

  def get(name: String)(implicit ec: EC, db: DB, au: AU, ac: AC): DbResultT[SearchIndexRoot] = {
    SearchIndexes.mustFindByNameWithFields(name).map {
      case (index, fields) ⇒
        SearchIndexRoot.fromModel(searchIndex = index, fields = fields)
    }
  }

}
