package services.category

import entities._
import payloads.CategoryPayloads._
import responses.CategoryResponses.{Category ⇒ CategoryResponse}
import utils.aliases._
import utils.db._

object CategoryManager {
  def getCategory(
      id: Int)(implicit ec: EC, db: DB, ac: AC, oc: OC, au: AU): DbResultT[CategoryResponse] = {
    // TODO: Replace this with a real value.
    val taxonomyId = 1

    for {
      category ← * <~ Categories.mustFindById(id, oc.id, taxonomyId)
    } yield CategoryResponse.build(category)
  }

  def createCategory(payload: CreateCategoryPayload)(implicit ec: EC,
                                                     db: DB,
                                                     ac: AC,
                                                     oc: OC,
                                                     au: AU): DbResultT[Unit] = {
    DbResultT.unit
  }

  def updateCategory(id: Int, payload: UpdateCategoryPayload)(implicit ec: EC,
                                                              db: DB,
                                                              ac: AC,
                                                              oc: OC,
                                                              au: AU): DbResultT[Unit] = {
    DbResultT.unit
  }
}
