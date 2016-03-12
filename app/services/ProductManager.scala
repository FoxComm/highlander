package services

import scala.concurrent.ExecutionContext

import models.inventory._
import models.product._
import models.objects._
import responses.ProductResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import payloads.{CreateProductForm, UpdateProductForm, CreateProductShadow, 
  UpdateProductShadow, CreateObjectContext, UpdateObjectContext,
  CreateFullProductForm, UpdateFullProductForm, CreateFullProductShadow, 
  UpdateFullProductShadow, CreateFullSkuForm, UpdateFullSkuForm, CreateSkuForm, 
  CreateSkuShadow, UpdateFullSkuShadow, CreateFullProduct, UpdateFullProduct}

import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}
import org.json4s.jackson.JsonMethods._

import ProductFailure._
import utils.aliases._
import utils.IlluminateAlgorithm
import cats.data.NonEmptyList
import cats.implicits._

import java.time.Instant

object ProductManager {

  def getForm(id: Int)
    (implicit ec: EC, db: DB): Result[ProductFormResponse.Root] = (for {
    form       ← * <~ ObjectForms.mustFindById404(id)
    product    ← * <~ Products.filter(_.formId === id).one.mustFindOr(
      ProductFormNotFound(id))
  } yield ProductFormResponse.build(product, form)).run()

  def getShadow(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[ProductShadowResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product ← * <~ Products.filter(_.contextId === context.id).
      filter(_.formId === id).one.
      mustFindOr(ProductNotFoundForContext(id, context.id))
    shadow  ← * <~ ObjectShadows.mustFindById404(product.shadowId)
  } yield ProductShadowResponse.build(shadow)).run()

  def getShadow(id: Int)
    (implicit ec: EC, db: DB): Result[ProductFormResponse.Root] = (for {
    form       ← * <~ ObjectForms.mustFindById404(id)
    product    ← * <~ Products.filter(_.formId === id).one.mustFindOr(
      ProductFormNotFound(id))
  } yield ProductFormResponse.build(product, form)).run()

  def getIlluminatedProduct(productId: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedProductResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product       ← * <~ Products.filter(_.contextId === context.id).
      filter(_.id === productId).one.
        mustFindOr(ProductNotFoundForContext(productId, context.id)) 
    form       ← * <~ ObjectForms.mustFindById404(product.formId)
    shadow       ← * <~ ObjectShadows.mustFindById404(product.shadowId)
  } yield IlluminatedProductResponse.build(
    IlluminatedProduct.illuminate(context, product, form, shadow))).run()

  def getFullProduct(productId: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product       ← * <~ Products.filter(_.contextId === context.id).
      filter(_.formId === productId).one.mustFindOr(
        ProductNotFoundForContext(productId, context.id)) 
    productForm    ← * <~ ObjectForms.mustFindById404(product.formId)
    productShadow  ← * <~ ObjectShadows.mustFindById404(product.shadowId)
    skuData       ← * <~ getSkuData(productShadow.id)
  } yield FullProductResponse.build(product, productForm, productShadow, skuData)).run()

  def createFullProduct(payload: CreateFullProduct, contextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {

    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    productForm ← * <~ ObjectForms.create(ObjectForm(kind = Product.kind,
      attributes = payload.form.product.attributes))
    productShadow ← * <~ ObjectShadows.create(ObjectShadow(formId = productForm.id,
      attributes = payload.shadow.product.attributes))
    productCommit  ← * <~ ObjectCommits.create(ObjectCommit(formId = productForm.id, 
      shadowId = productShadow.id))
    _    ← * <~ validateShadow(productForm, productShadow)
    product     ← * <~ Products.create(Product(contextId = context.id, 
      formId = productForm.id, shadowId = productShadow.id, commitId = productCommit.id))
    skuData ← * <~ createSkuData(context, productShadow.id, 
      payload.form.skus, payload.shadow.skus)

  } yield FullProductResponse.build(product, productForm, productShadow, skuData)).runTxn()

  def updateFullProduct(productId: Int, payload: UpdateFullProduct, contextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {

    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product       ← * <~ Products.filter(_.contextId === context.id).
      filter(_.formId === productId).one.mustFindOr(
        ProductNotFoundForContext(productId, context.id)) 
    updatedFormShadow ← * <~ updateObjectFormAndShadow(product.formId, 
      product.shadowId, payload.form.product.attributes, 
      payload.shadow.product.attributes)
    (productForm, productShadow, productChanged)  = updatedFormShadow
    updatedSkuData ← * <~ updateSkuData(context, productShadow.id, 
      payload.form.skus, payload.shadow.skus, productChanged)
    skusChanged ← * <~ anyChanged(updatedSkuData.map(_._4))
    skuData  ← * <~ updatedSkuData.map( t ⇒ (t._1, t._2, t._3))
    product ← * <~ commitProduct(product, productForm, productShadow, productChanged || skusChanged)
  } yield FullProductResponse.build(product, productForm, productShadow, skuData)).runTxn()


  def getIlluminatedFullProduct(productId: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedFullProductResponse.Root] = (for {

    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    product       ← * <~ Products.filter(_.contextId === context.id).
      filter(_.formId === productId).one.mustFindOr(
        ProductNotFoundForContext(productId, context.id)) 
      
    productForm   ← * <~ ObjectForms.mustFindById404(product.formId)
    productShadow  ← * <~ ObjectShadows.mustFindById404(product.shadowId)
    skuData ← * <~ getSkuData(productShadow.id)

  } yield IlluminatedFullProductResponse.build(
    IlluminatedProduct.illuminate(context, product, productForm, productShadow),
    skuData.map { 
      case (s, f, sh) ⇒ IlluminatedSku.illuminate(context, s, f, sh)
    })).run()

  private def anyChanged(changes: Seq[Boolean]) : Boolean = 
    changes.contains(true)

  private def commitProduct(product: Product, productForm: ObjectForm, 
    productShadow: ObjectShadow, shouldCommit: Boolean) 
    (implicit ec: EC, db: DB): DbResultT[Product] = 
      if(shouldCommit) for { 
        newCommit  ← * <~ commit(product.commitId, productForm.id, productShadow.id)
        product     ← * <~ Products.update(product, product.copy(
          shadowId = productShadow.id, commitId = newCommit.id))
      } yield product
    else DbResultT.pure(product)

  private def commitSku(sku: Sku, skuForm: ObjectForm, 
    skuShadow: ObjectShadow, shouldCommit: Boolean) 
    (implicit ec: EC, db: DB): DbResultT[Sku] = 
      if(shouldCommit) for { 
        newCommit  ← * <~ commit(sku.commitId, skuForm.id, skuShadow.id)
        product     ← * <~ Skus.update(sku, sku.copy(
          shadowId = skuShadow.id, commitId = newCommit.id))
      } yield product
    else DbResultT.pure(sku)

  private def commit(previousCommitId: Int, formId: Int, shadowId: Int)
    (implicit ec: EC, db: DB): DbResultT[ObjectCommit] = for {
        previousCommit ← * <~ ObjectCommits.mustFindById404(previousCommitId)
        newCommit  ← * <~ ObjectCommits.create(ObjectCommit(formId = formId, 
          shadowId = shadowId, previousId = previousCommit.id.some))
    } yield newCommit

  private def failIfErrors(errors: Seq[Failure])
  (implicit ec: EC, db: DB) : DbResultT[Unit] =  {
    errors match {
      case Nil ⇒ DbResultT.pure(Unit)
      case head ::tail ⇒ DbResultT.leftLift(NonEmptyList(head, tail))
    }
  }

  private def validateSkuPayload(skuGroup : Seq[(CreateFullSkuForm, CreateSkuShadow)]) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] =
    failIfErrors(skuGroup.flatMap { case (f, s) ⇒ 
      if(f.code === s.code) Seq.empty
      else Seq(SkuShadowNotFoundInPayload(f.code))
    })

  private def validateSkuPayload2(skuGroup : Seq[(UpdateFullSkuForm, UpdateFullSkuShadow)]) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] =
    failIfErrors(skuGroup.flatMap { case (f, s) ⇒ 
      if(f.code === s.code) Seq.empty
      else Seq(SkuShadowNotFoundInPayload(f.code))
    })

  private def validateShadow(form: ObjectForm, shadow: ObjectShadow) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] = 
    failIfErrors(IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes))

  private def validateShadow(product: Product, form: ObjectForm, shadow: ObjectShadow) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] = 
    failIfErrors(ProductValidator.validate(product, form, shadow))

  private def createSku(context: ObjectContext, productShadowId: Int, 
    formPayload: CreateFullSkuForm, shadowPayload: CreateSkuShadow)
  (implicit ec: EC, db: DB) = { 
    require(formPayload.code == shadowPayload.code)

    for {
      form    ← * <~ ObjectForms.create(ObjectForm(kind = Sku.kind, 
        attributes = formPayload.attributes))
      shadow  ← * <~ ObjectShadows.create(
        ObjectShadow(formId = form.id, attributes = shadowPayload.attributes))
      _    ← * <~ SkuManager.validateShadow(form, shadow)
      _    ← * <~ ObjectLinks.create(ObjectLink(leftId = productShadowId, 
        rightId = shadow.id))
      commit  ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      sku ← * <~ Skus.create(Sku(contextId = context.id, code = formPayload.code, 
        formId = form.id, shadowId = shadow.id, commitId = commit.id))
    } yield (sku, form, shadow)
  }
    
  private def getSkuData(productShadowId: Int)(implicit ec: EC, db: DB) : 
    DbResultT[Seq[(Sku, ObjectForm, ObjectShadow)]] = for {

    skuLinks     ← * <~ ObjectLinks.filter(_.leftId === productShadowId).result
    skuShadowIds ← * <~ skuLinks.map(_.rightId)
    skuShadows   ← * <~ ObjectShadows.filter(_.id.inSet(skuShadowIds)).result
    skuFormIds   ← * <~ skuShadows.map(_.formId)
    skuForms     ← * <~ ObjectForms.filter(_.id.inSet(skuFormIds)).result
    skus         ← * <~ Skus.filter(_.formId.inSet(skuFormIds)).result

  } yield (skus.zip(skuForms.zip(skuShadows))).map{ case (a, b) ⇒ (a, b._1, b._2)}

  private def createSkuData(context: ObjectContext, productShadowId: Int, 
    formPayloads: Seq[CreateFullSkuForm], shadowPayloads: Seq[CreateSkuShadow])
  (implicit ec: EC, db: DB) : DbResultT[Seq[(Sku, ObjectForm, ObjectShadow)]] = for {

    skuGroup    ← * <~ groupSkuFormsAndShadows(formPayloads, shadowPayloads)
    _            ← * <~ validateSkuPayload(skuGroup) 
    skuData ← * <~ DbResultT.sequence( 
      skuGroup map { case (f, sh) ⇒  createSku(context, productShadowId, f, sh)}
    )
  } yield skuData

  private def updateSkuData(context: ObjectContext, productShadowId: Int, 
    formPayloads: Seq[UpdateFullSkuForm], shadowPayloads: Seq[UpdateFullSkuShadow], productChanged: Boolean)
  (implicit ec: EC, db: DB) : DbResultT[Seq[(Sku, ObjectForm, ObjectShadow, Boolean)]] = for {

    skuGroup    ← * <~ groupSkuFormsAndShadows2(formPayloads, shadowPayloads)
    _            ← * <~ validateSkuPayload2(skuGroup) 
    skuData ← * <~ DbResultT.sequence( 
      skuGroup map { case (f, sh) ⇒  updateSku(context, productShadowId, f, sh, productChanged)}
    )
  } yield skuData

  private def updateSku(context: ObjectContext, productShadowId: Int, 
    formPayload: UpdateFullSkuForm, shadowPayload: UpdateFullSkuShadow, productChanged: Boolean)
  (implicit ec: EC, db: DB): DbResultT[(Sku, ObjectForm, ObjectShadow, Boolean)] = {
    require(formPayload.code == shadowPayload.code)
    for {
      sku   ← * <~ Skus.filter(_.contextId === context.id).
      filter(_.code === formPayload.code).one.mustFindOr(
        SkuNotFoundForContext(formPayload.code, context.name)) 
      updatedFormShadow  ← * <~ updateObjectFormAndShadow(
        sku.formId, sku.shadowId, formPayload.attributes, shadowPayload.attributes)
      (form, shadow, skuChanged) = updatedFormShadow
      _ ← * <~ createLink(productShadowId, shadow.id, productChanged || skuChanged)
      sku  ← * <~ commitSku(sku, form, shadow, skuChanged)
    } yield (sku, form, shadow, skuChanged)
  }

  private def createLink(productShadowId: Int, skuShadowId: Int, create: Boolean)
  (implicit ec: EC, db: DB): DbResultT[Unit] = 
  if(create) for {
      _ ← * <~ ObjectLinks.create(ObjectLink(leftId = productShadowId, rightId = skuShadowId)) 
  } yield Unit 
  else DbResultT.pure(Unit)

  private def groupSkuFormsAndShadows(forms: Seq[CreateFullSkuForm], shadows: Seq[CreateSkuShadow]) = {
    val sortedForms = forms.sortBy(_.code)
    val sortedShadows = shadows.sortBy(_.code)
    sortedForms zip sortedShadows
  }

  private def groupSkuFormsAndShadows2(forms: Seq[UpdateFullSkuForm], shadows: Seq[UpdateFullSkuShadow]) = {
    val sortedForms = forms.sortBy(_.code)
    val sortedShadows = shadows.sortBy(_.code)
    sortedForms zip sortedShadows
  }

  private def updateObjectFormAndShadow(formId: Int, shadowId: Int, 
    updatedForm: JValue, updatedShadow: JValue)
  (implicit ec: EC, db: DB): DbResultT[(ObjectForm, ObjectShadow, Boolean)] = for {

    oldForm     ← * <~ ObjectForms.mustFindById404(formId)
    oldShadow   ← * <~ ObjectShadows.mustFindById404(shadowId)
    newFormPair ← * <~ ObjectUtils.updateForm(oldForm.attributes, updatedForm)
    (keyMap, newFormAttributes) = newFormPair
    newShadowAttributes ← * <~ ObjectUtils.newShadow(oldShadow.attributes, keyMap)
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


}
