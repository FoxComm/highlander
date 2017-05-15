package services.objects

import failures.NotFoundFailure404
import models.objects._
import payloads.ObjectSchemaPayloads._
import responses.ObjectResponses.ObjectSchemaResponse._
import slick.jdbc.PostgresProfile.api._
import utils.db._

object ObjectSchemasManager {

  def getSchema(name: String)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      schema ← * <~ ObjectFullSchemas
                .findOneByName(name)
                .mustFindOr(NotFoundFailure404(ObjectFullSchema, name))
    } yield build(schema)

  def getSchemasForKind(kind: String)(implicit ec: EC, db: DB): DbResultT[Seq[Root]] =
    for {
      schemas ← * <~ ObjectFullSchemas.filterByKind(kind).result
    } yield schemas.map(build)

  def getAllSchemas()(implicit ec: EC, db: DB): DbResultT[Seq[Root]] =
    for {
      schemas ← * <~ ObjectFullSchemas.result
    } yield schemas.map(build)

  private def mustGetEmptySchema()(implicit ec: EC): DbResultT[ObjectFullSchema] =
    ObjectFullSchemas
      .filter(_.name === "empty")
      .mustFindOneOr(failures.NotFoundFailure404(ObjectFullSchema, "empty"))

  def getSchemaByOptNameOrKind(schema: Option[String], kind: String)(
      implicit ec: EC): DbResultT[Option[ObjectFullSchema]] = {
    schema.fold {
      ObjectFullSchemas.filterByKind(kind).one
    } { schemaName ⇒
      ObjectFullSchemas.findOneByName(schemaName)
    }.dbresult
  }

  def update(name: String, payload: UpdateObjectSchema)(implicit ec: EC,
                                                        db: DB): DbResultT[ObjectSchema] =
    for {
      objectSchema ← * <~ ObjectSchemas
                      .findOneByName(name)
                      .mustFindOr(NotFoundFailure404(ObjectSchema, name))
      updated ← * <~ ObjectSchemas.update(
                   objectSchema,
                   objectSchema.copy(schema = payload.schema,
                                     dependencies =
                                       payload.dependencies.getOrElse(objectSchema.dependencies)))
    } yield updated

}
