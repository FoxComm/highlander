package services

import models.cord.lineitems._
import payloads.LineItemPayloads.{UpdateLineItemsPayload ⇒ Payload}
import responses.ProductVariantResponses.ProductVariantResponse
import testutils._
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._
import utils.MockedApis

class LineItemUpdaterTest
    extends IntegrationTestBase
    with TestObjectContext
    with TestActivityContext.AdminAC
    with MockedApis
    with ApiFixtures
    with ApiFixtureHelpers
    with BakedFixtures {

  def createProducts(num: Int): Seq[ProductVariantResponse.Root] = {
    (1 to num).map { _ ⇒
      new ProductVariant_ApiFixture {}.productVariant
    }
  }

  "LineItemUpdater" - {

    "Adds line items when the sku doesn't exist in cart" in new Fixture {
      val variants = createProducts(2)

      val payload = Seq[Payload](
          Payload(productVariantId = variants(0).id, quantity = 3),
          Payload(productVariantId = variants(1).id, quantity = 0)
      )

      val skus = LineItemUpdater
        .updateQuantitiesOnCart(storeAdmin, cartRef, payload)
        .gimme
        .result
        .lineItems
        .skus

      skus.map(_.sku) must contain theSameElementsAs Seq(variants(0).attributes.code)
      // TODO: check if *variant* IDs match?
      skus.find(_.sku === variants(0).attributes.code).map(_.quantity) must be(Some(3))
      skus.map(_.quantity).sum must === (CartLineItems.size.gimme)
    }

    "Updates line items when the ProductVariant already is in cart" in new Fixture {
      val variants = createProducts(3)

      val lineItemPayload = {
        val variantIds = Seq.fill(variants(0).id)(6) ++ Seq(variants(1).id) ++ Seq.fill(
              variants(2).id)(2)
        variantIds.map { variantId ⇒
          Payload(productVariantId = variantId, quantity = 1)
        }
      }
      cartsApi(cartRef).lineItems.add(lineItemPayload).mustBeOk()

      val payload = Seq[Payload](
          Payload(productVariantId = variants(0).id, quantity = 3),
          Payload(productVariantId = variants(1).id, quantity = 0),
          Payload(productVariantId = variants(2).id, quantity = 1)
      )

      val skus = LineItemUpdater
        .updateQuantitiesOnCart(storeAdmin, cartRef, payload)
        .gimme
        .result
        .lineItems
        .skus

      skus.map(_.sku) must contain theSameElementsAs Seq(variants(0), variants(2))
        .map(_.attributes.code)
      // TODO: check if *variant* IDs match?
      skus.find(_.sku === variants(0).attributes.code).map(_.quantity) must be(Some(3))
      skus.map(_.quantity).sum must === (CartLineItems.gimme.size)
    }
  }

  trait Fixture extends StoreAdmin_Seed {

    val cartRef = api_newGuestCart().referenceNumber
  }
}
