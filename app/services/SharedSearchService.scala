package services

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.{SharedSearch, SharedSearches, StoreAdmin}
import payloads.SharedSearchPayload
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._

object SharedSearchService {

  def get(code: String)(implicit ec: ExecutionContext, db: Database): Result[SharedSearch] =
    mustFindActiveByCode(code).run()

  def create(admin: StoreAdmin, payload: SharedSearchPayload)
    (implicit ec: ExecutionContext, db: Database): Result[SharedSearch] = (for {
    search ← * <~ SharedSearches.create(SharedSearch.byAdmin(admin, payload))
  } yield search).runT()

  def update(admin: StoreAdmin, code: String, payload: SharedSearchPayload)
    (implicit ec: ExecutionContext, db: Database): Result[SharedSearch] = (for {
    search  ← * <~ mustFindActiveByCode(code)
    updated ← * <~ SharedSearches.update(search, search.copy(title = payload.title, query = payload.query))
  } yield updated).runT()

  def delete(admin: StoreAdmin, code: String)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = (for {
    search ← * <~ mustFindActiveByCode(code)
    _      ← * <~ SharedSearches.update(search, search.copy(deletedAt = Some(Instant.now)))
  } yield ()).runT()

  private def mustFindActiveByCode(code: String)(implicit ec: ExecutionContext): DbResult[SharedSearch] =
    SharedSearches.findActiveByCode(code).mustFindOr(NotFoundFailure404(SharedSearch, code))
}
