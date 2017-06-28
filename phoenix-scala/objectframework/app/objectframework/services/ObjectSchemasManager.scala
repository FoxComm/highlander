package objectframework.services

import core.db._
import core.failures.NotFoundFailure404
import objectframework.ObjectResponses.ObjectSchemaResponse
import objectframework.ObjectResponses.ObjectSchemaResponse._
import objectframework.models._
import objectframework.payloads.ObjectSchemaPayloads._
import slick.jdbc.PostgresProfile.api._

object ObjectSchemasManager {

  def getSchema(name: String)(implicit ec: EC, db: DB): DbResultT[ObjectSchemaResponse] =
    for {
      schema ← * <~ ObjectFullSchemas
                .findOneByName(name)
                .mustFindOr(NotFoundFailure404(ObjectFullSchema, name))
    } yield build(schema)

  def getSchemasForKind(kind: String)(implicit ec: EC, db: DB): DbResultT[Seq[ObjectSchemaResponse]] =
    for {
      schemas ← * <~ ObjectFullSchemas.filterByKind(kind).result
    } yield schemas.map(build)

  def getAllSchemas()(implicit ec: EC, db: DB): DbResultT[Seq[ObjectSchemaResponse]] =
    for {
      schemas ← * <~ ObjectFullSchemas.result
    } yield schemas.map(build)

  private def mustGetEmptySchema()(implicit ec: EC): DbResultT[ObjectFullSchema] =
    ObjectFullSchemas
      .filter(_.name === "empty")
      .mustFindOneOr(NotFoundFailure404(ObjectFullSchema, "empty"))

  def getSchemaByOptNameOrKind(schema: Option[String], kind: String)(
      implicit ec: EC): DbResultT[Option[ObjectFullSchema]] =
    schema.fold {
      ObjectFullSchemas.filterByKind(kind).one
    } { schemaName ⇒
      ObjectFullSchemas.findOneByName(schemaName)
    }.dbresult

  def update(name: String, payload: UpdateObjectSchema)(implicit ec: EC, db: DB): DbResultT[ObjectSchema] =
    for {
      objectSchema ← * <~ ObjectSchemas
                      .findOneByName(name)
                      .mustFindOr(NotFoundFailure404(ObjectSchema, name))
      updated ← * <~ ObjectSchemas.update(
                 objectSchema,
                 objectSchema.copy(schema = payload.schema,
                                   dependencies = payload.dependencies.getOrElse(objectSchema.dependencies)))
    } yield updated

}
