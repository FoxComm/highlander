package services

import scala.concurrent.ExecutionContext

import models.objects._
import responses.ObjectResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.IlluminateAlgorithm
import utils.Slick.implicits._
import payloads.{CreateObjectContext, UpdateObjectContext}

import failures.ObjectFailures._
import utils.aliases._
import cats.data.NonEmptyList
import cats.implicits._
import org.json4s.JsonAST.JValue
import java.time.Instant

object ObjectManager {

  // Detailed info for SKU of each type in given warehouse
  def getForm(id: Int)
    (implicit ec: EC, db: DB): Result[ObjectFormResponse.Root] = (for {
    form       ← * <~ ObjectForms.mustFindById404(id)
  } yield ObjectFormResponse.build(form)).run()

  def getShadow(shadowId: Int)
    (implicit ec: EC, db: DB): Result[ObjectShadowResponse.Root] = (for {
    shadow  ← * <~ ObjectShadows.mustFindById404(shadowId)
  } yield ObjectShadowResponse.build(shadow)).run()

  def getIlluminatedObject(formId: Int, shadowId: Int)
    (implicit ec: EC, db: DB): Result[IlluminatedObjectResponse.Root] = (for {
    form       ← * <~ ObjectForms.mustFindById404(formId)
    shadow       ← * <~ ObjectShadows.mustFindById404(shadowId)
  } yield IlluminatedObjectResponse.build(IlluminatedObject.illuminate(form, shadow))).run()

  def getContextByName(name: String) 
    (implicit ec: EC, db: DB): Result[ObjectContextResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(name).one.
      mustFindOr(ObjectContextNotFound(name))
  } yield ObjectContextResponse.build(context)).run()

  def createContext(payload: CreateObjectContext) 
    (implicit ec: EC, db: DB): Result[ObjectContextResponse.Root] = (for {
    context ← * <~ ObjectContexts.create(ObjectContext(
      name = payload.name, attributes = payload.attributes))
  } yield ObjectContextResponse.build(context)).runTxn()

  def updateContextByName(name: String, payload: UpdateObjectContext) 
    (implicit ec: EC, db: DB): Result[ObjectContextResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(name).one.
      mustFindOr(ObjectContextNotFound(name))
    context  ← * <~ ObjectContexts.update(context, 
      context.copy(name = payload.name, attributes = payload.attributes))
  } yield ObjectContextResponse.build(context)).runTxn()

  def createCommit(previousCommitId: Int, formId: Int, shadowId: Int)
    (implicit ec: EC, db: DB): DbResultT[ObjectCommit] = for {
    newCommit  ← * <~ ObjectCommits.create(ObjectCommit(formId = formId,
      shadowId = shadowId, previousId = previousCommitId.some))
  } yield newCommit

  def updateObjectFormAndShadow(formId: Int, shadowId: Int,
    updatedForm: JValue, updatedShadow: JValue)
    (implicit ec: EC, db: DB): DbResultT[(ObjectForm, ObjectShadow, Boolean)] = for {

    oldForm     ← * <~ ObjectForms.mustFindById404(formId)
    oldShadow   ← * <~ ObjectShadows.mustFindById404(shadowId)
    newFormPair ← * <~ ObjectUtils.updateForm(oldForm.attributes, updatedForm)
    (keyMap, newFormAttributes) = newFormPair
    newShadowAttributes ← * <~ ObjectUtils.newShadow(updatedShadow, keyMap)
    result ← * <~ createIfDifferent(oldForm, oldShadow, newFormAttributes, newShadowAttributes)
  } yield result

  private def createIfDifferent(oldForm: ObjectForm, oldShadow: ObjectShadow,
    newFormAttributes: JValue, newShadowAttributes: JValue)
    (implicit ec: EC, db: DB): DbResultT[(ObjectForm, ObjectShadow, Boolean)] = {
    if(oldShadow.attributes != newShadowAttributes)
      for {
        form   ← * <~ ObjectForms.update(oldForm, oldForm.copy(attributes =
          newFormAttributes, updatedAt = Instant.now))
        shadow ← * <~ ObjectShadows.create(ObjectShadow(formId = form.id,
          attributes = newShadowAttributes))
        _    ← * <~ validateShadow(form, shadow)
      } yield (form, shadow, true)
    else DbResultT.pure((oldForm, oldShadow, false))
  }

  private def validateShadow(form: ObjectForm, shadow: ObjectShadow)
  (implicit ec: EC, db: DB) : DbResultT[Unit] = 
    IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes) match {
      case Nil ⇒ DbResultT.pure(Unit)
      case head :: tail ⇒ DbResultT.leftLift(NonEmptyList(head, tail))
    }
}
