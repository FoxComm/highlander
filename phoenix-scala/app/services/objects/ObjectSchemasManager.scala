package services.objects

import failures.NotFoundFailure404
import failures.ObjectFailures._
import payloads.ObjectSchemaPayloads._
import responses.ObjectResponses.ObjectSchemaResponse._
import models.objects._
import utils.aliases._
import utils.db._
import slick.driver.PostgresDriver.api._

object ObjectSchemasManager {

  def getSchema(kind: String)(implicit ec: EC, db: DB, oc: OC): DbResultT[Root] =
    for {
      schema ← * <~ ObjectFullSchemas
                .filterByKind(kind)
                .filter(_.contextId === oc.id)
                .mustFindOneOr(NotFoundFailure404(ObjectFullSchema, "kind", kind))
    } yield build(schema)

  def createSchema(
      payload: CreateSchemaPayload)(implicit ec: EC, db: DB, oc: OC): DbResultT[Root] =
    for {
      _          ← * <~ ObjectSchemas.create(ObjectSchema.fromCreatePayload(payload))
      fullSchema ← * <~ getSchema(payload.kind)
    } yield fullSchema

  def updateSchema(name: String, payload: UpdateSchemaPayload)(implicit ec: EC, db: DB, oc: OC) =
    for {
      schema     ← * <~ ObjectSchemas.mustFindByName404(name)
      toUpdate   ← * <~ ObjectSchema.fromUpdatePayload(schema, payload)
      _          ← * <~ ObjectSchemas.update(schema, toUpdate)
      fullSchema ← * <~ getSchema(toUpdate.kind)
    } yield fullSchema

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

}
