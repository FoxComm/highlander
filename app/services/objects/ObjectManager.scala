package services.objects

import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.objects._
import payloads.ContextPayloads._
import responses.ObjectResponses._
import services._
import utils.aliases._
import utils.db._

object ObjectManager {

  def getForm(id: Int)(implicit ec: EC, db: DB): Result[ObjectFormResponse.Root] =
    (for {
      form ← * <~ ObjectForms.mustFindById404(id)
    } yield ObjectFormResponse.build(form)).run()

  def getShadow(shadowId: Int)(implicit ec: EC, db: DB): Result[ObjectShadowResponse.Root] =
    (for {
      shadow ← * <~ ObjectShadows.mustFindById404(shadowId)
    } yield ObjectShadowResponse.build(shadow)).run()

  def getIlluminatedObject(formId: Int, shadowId: Int)(
      implicit ec: EC,
      db: DB): Result[IlluminatedObjectResponse.Root] =
    (for {
      form   ← * <~ ObjectForms.mustFindById404(formId)
      shadow ← * <~ ObjectShadows.mustFindById404(shadowId)
    } yield IlluminatedObjectResponse.build(IlluminatedObject.illuminate(form, shadow))).run()

  def getContextByName(name: String)(implicit ec: EC, db: DB): Result[ObjectContextResponse.Root] =
    (for {
      context ← * <~ mustFindByName404(name)
    } yield ObjectContextResponse.build(context)).run()

  def createContext(payload: CreateObjectContext)(implicit ec: EC,
                                                  db: DB): Result[ObjectContextResponse.Root] =
    (for {
      context ← * <~ ObjectContexts.create(
                   ObjectContext(name = payload.name, attributes = payload.attributes))
    } yield ObjectContextResponse.build(context)).runTxn()

  def updateContextByName(name: String, payload: UpdateObjectContext)(
      implicit ec: EC,
      db: DB): Result[ObjectContextResponse.Root] =
    (for {
      context ← * <~ mustFindByName404(name)
      update ← * <~ ObjectContexts.update(
                  context,
                  context.copy(name = payload.name, attributes = payload.attributes))
    } yield ObjectContextResponse.build(update)).runTxn()

  def mustFindByName404(name: String)(implicit ec: EC): DbResultT[ObjectContext] =
    ObjectContexts.filterByName(name).mustFindOneOr(ObjectContextNotFound(name))

  def mustFindFormById404(id: Int)(implicit ec: EC): DbResultT[ObjectForm] =
    ObjectForms.findOneById(id).mustFindOr(NotFoundFailure404(ObjectForm, id))

  def mustFindShadowById404(id: Int)(implicit ec: EC): DbResultT[ObjectShadow] =
    ObjectShadows.findOneById(id).mustFindOr(NotFoundFailure404(ObjectShadow, id))
}
