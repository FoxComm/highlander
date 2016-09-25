package services.objects

import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.objects._
import utils.aliases._
import utils.db._

object ObjectSchemasManager {

  def getSchema(name: String)(implicit ec: EC, db: DB): DbResultT[Json] =
    for {
      schema ← * <~ ObjectFullSchemas
                .findByName(name)
                .mustFindOr(NotFoundFailure404(ObjectFullSchema, name))
    } yield schema.schema

}
