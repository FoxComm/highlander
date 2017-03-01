import cats.implicits._
import failures.NotFoundFailure404
import failures.ProductFailures.DuplicatedOptionValueForVariant
import models.objects.{ObjectFullSchemas, ObjectSchemas, ProductOptionLinks, ProductVariantLinks}
import models.product.{Product, Products}
import payloads.ProductPayloads._
import responses.ProductOptionResponses.ProductOptionResponse
import responses.ProductResponses.ProductResponse.Root
import testutils._
import testutils.apis.{PhoenixAdminApi, PhoenixStorefrontApi}
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._
import testutils.fixtures.api.products._

class ProductIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixStorefrontApi
    with AutomaticAuth
    with BakedFixtures
    with ApiFixtures
    with ApiFixtureHelpers
    with TaxonomySeeds {

  "GET v1/products/:context" - {
    "returns assigned taxonomies" in new ProductAndSkus_Baked with FlatTaxons_Baked {
      taxonsApi(taxons.head.formId).assignProduct(simpleProduct.formId).mustBeOk()
      val product = productsApi(simpleProduct.formId).get().as[Root]
      product.taxons.flatMap(_.taxons.map(_.id)) must contain(taxons.head.formId)
    }
  }

  val singleOptionCfg = ProductOptionCfg("snow", Seq("white")) // yellow if you're naughty

  "GET v1/products/:context" - {
    // TODO: returns variants and options?
  }

  "GET v1/my/products/:ref/baked" - {
    "404 for archived products" in new ProductVariant_ApiFixture {
      productsApi(product.id).archive().mustBeOk()

      storefrontProductsApi(product.slug)
        .get()
        .mustFailWith404(NotFoundFailure404(Product, product.slug))
    }

    "404 for inactive products" in {
      val inactive: CreateProductPayload = {
        val active = InvariantProductPayloadBuilder().createPayload
        active.copy(attributes = active.attributes - "activeFrom")
      }
      val slug = productsApi.create(inactive).as[Root].slug

      storefrontProductsApi(slug).get().mustFailWith404(NotFoundFailure404(Product, slug))
    }

    "404 if all variants are archived" in new ProductVariant_ApiFixture {
      productVariantsApi(productVariant.id).archive().mustBeOk()

      storefrontProductsApi(product.slug)
        .get()
        .mustFailWith404(NotFoundFailure404(Product, product.slug))
    }

    "404 if all variants are inactive" in {
      val newProduct: CreateProductPayload = {
        val payloadBuilder = InvariantProductPayloadBuilder()
        val activeVariant  = payloadBuilder.variantPayload
        val inactiveVariant =
          activeVariant.copy(attributes = activeVariant.attributes - "activeFrom")
        payloadBuilder.createPayload.copy(variants = Seq(inactiveVariant))
      }

      val slug = productsApi.create(newProduct).as[Root].slug

      storefrontProductsApi(slug).get().mustFailWith404(NotFoundFailure404(Product, slug))
    }
  }

  "POST v1/products/:context" - {

    "Creates a product with" - {

      "a new variant with option successfully" in {
        val payloadBuilder = OneOptionProductPayloadBuilder(singleOptionCfg, AllVariantsCfg)
        val createPayload  = payloadBuilder.createProductPayload

        val product = productsApi.create(createPayload).as[Root]

        val variantCode   = payloadBuilder.variantCodes.onlyElement
        val variantOption = product.variants.onlyElement.options.onlyElement
        product.variants.onlyElement.attributes.code must === (variantCode)
        variantOption mustBe a[ProductOptionResponse.Partial]
        variantOption.values.onlyElement.name must === (singleOptionCfg.values.onlyElement)
        product.options.onlyElement.values.onlyElement.variantIds.value must have size 1
      }

      "an existing variant with options successfully" in {
        // Create product 1 with color and size options
        val fixture1 = new Product_ColorSizeOptions_ApiFixture {}

        val product2PayloadBuilder =
          OneOptionProductPayloadBuilder(ProductOptionCfg("Color", fixture1.colors.all),
                                         OneOptionVariantsCfg(fixture1.colors.all),
                                         OneOptionVariantsCfg(fixture1.colors.all),
                                         fixture1.price,
                                         randomProductName)
        // Create product with only color option, but reuse variant codes from product 1
        val createPayload = product2PayloadBuilder.createProductPayload

        val product2 = productsApi.create(createPayload).as[Root]

        val product2OptionValues = product2.options.onlyElement.values
        product2OptionValues.map(_.name) must === (fixture1.colors.all)

        val variantOptionValues = for {
          variant     ← product2.variants
          option      ← variant.options
          optionValue ← option.values
        } yield optionValue.name
        variantOptionValues must contain theSameElementsAs product2PayloadBuilder.optionCfg.values

        val optionValueVariantIds = product2OptionValues.flatMap(_.variantIds.value)
        optionValueVariantIds must contain theSameElementsAs product2.variants.map(_.id)
        optionValueVariantIds.length must === (product2PayloadBuilder.variantCodes.length)
      }

      "empty productOption successfully" in {
        val createPayload =
          OneOptionProductPayloadBuilder(optionCfg = singleOptionCfg,
                                         variantCfg = AllVariantsCfg,
                                         optionVariantCfg = NoneVariantsCfg).createProductPayload

        import slick.driver.PostgresDriver.api._
        println(ObjectSchemas.map(_.name).result.gimme)
        println(ObjectFullSchemas.map(_.name).result.gimme)

        val product = productsApi.create(createPayload).as[Root]
        // FIXME: Some(empty sequence) is just a terrible API design
        product.options.onlyElement.values.onlyElement.variantIds.value mustBe empty
        product.variants mustBe empty
      }
    }

    "Throws an error if" - {

      "no variant exists for product options" in {
        val createPayload =
          OneOptionProductPayloadBuilder(singleOptionCfg, NoneVariantsCfg).createProductPayload
        productsApi.create(createPayload).mustFailWithMessage("Product variants must not be empty")
      }

      "variant values in payload are duplicated" in {
        val payloadBuilder = OneOptionProductPayloadBuilder(
            ProductOptionCfg("foo", Seq("bar", "baz", "quux", "baz", "bar")))

        val expectedFailures = payloadBuilder.variantCodes
          .filterNot(_.contains("quux"))
          .map(DuplicatedOptionValueForVariant(_))

        productsApi
          .create(payloadBuilder.createProductPayload)
          .mustFailWith400(expectedFailures: _*)
      }
    }
  }

  "PATCH v1/products/:context/:id" - {

    "Keeps everything if update payload is empty" in new Product_ColorSizeOptions_ApiFixture {
      val updatePayload = UpdateProductPayload(attributes = Map.empty,
                                               variants = None,
                                               options = None,
                                               albums = None)

      productsApi(product.id).update(updatePayload).as[Root] must === (product)
    }

    // WTF ALERT! -- @anna
    // Apparently, if product has options and option-driven variants, it IGNORES a new stray variant
    // MAKE IT FAIL if we can't add a new variant!
    "Updates the variants on a product successfully" in new Product_ColorSizeOptions_ApiFixture {
      val updatePayload = UpdateProductPayload(attributes = Map.empty,
                                               variants = Seq(buildVariantPayload()).some,
                                               options = None,
                                               albums = None)

      val updatedProduct = productsApi(product.id).update(updatePayload).as[Root]
      updatedProduct.variants.length must === (variantsQty)
      updatedProduct.options.length must === (optionsQty)
    }

    "Removes some variants from product" in new Product_ColorSizeOptions_ApiFixture {
      productsApi(product.id).get.as[Root].variants must have size 4

      val remaningVariantCodes =
        payloadBuilder.filterCodes(Seq((colors.red, sizes.large), (sizes.small, colors.green)))
      remaningVariantCodes must have size 2

      productsApi(product.id)
        .update(payloadBuilder.updateToVariants(remaningVariantCodes))
        .as[Root]
        .variants
        .map(_.attributes.code) must contain theSameElementsAs remaningVariantCodes
    }

    // WTF ALERT #2
    // What kind of test is this? Manually removing links?..
    // Also, same shit as above: must return an error instead of ignoring the variant!
    "Replaces variants on a product if options are Some(Seq.empty)" in new Product_ColorSizeOptions_ApiFixture {
      import slick.driver.PostgresDriver.api._
      val productModel = Products.filter(_.formId === product.id).result.head.gimme
      ProductOptionLinks.filterLeft(productModel).delete.gimme
      ProductVariantLinks.filterLeft(productModel).delete.gimme

      val newVariant = buildVariantPayload(code = "XXX")
      val updatePayload = UpdateProductPayload(attributes = Map.empty,
                                               variants = Seq(newVariant).some,
                                               albums = None,
                                               options = Seq.empty.some)

      val updated = productsApi(product.id).update(updatePayload).as[Root]
      updated.variants.onlyElement.attributes.code must === ("XXX")
      updated.options mustBe empty
    }

    "Updates the options" - {
      "Remove all options successfully" in new Product_ColorSizeOptions_ApiFixture {
        val payload = UpdateProductPayload(attributes = Map.empty,
                                           variants = None,
                                           options = Some(Seq()),
                                           albums = None)

        val response = productsApi(product.id).update(payload).as[Root]
        response.variants mustBe empty
        response.options mustBe empty
      }

      "Add new option with new variant successfully" in new Product_ColorSizeOptions_ApiFixture {
        val metalBuilder =
          OneOptionProductPayloadBuilder(ProductOptionCfg("Metal", Seq("gold", "silver")),
                                         variantCfg = OneOptionVariantsCfg(Seq("gold")))

        val combinedOptions = for {
          colorSizes ← payloadBuilder.optionsPayload
          metals     ← metalBuilder.optionsPayload
        } yield colorSizes ++ metals

        val updatePayload = UpdateProductPayload(attributes = Map.empty,
                                                 variants = metalBuilder.variantsPayload.some,
                                                 albums = None,
                                                 options = combinedOptions)

        val updated = productsApi(product.id).update(updatePayload).as[Root]
        updated.variants.length must === (variantsQty + 1) // gold
        updated.options.length must === (combinedOptions.value.length)
        updated.variants.map(_.attributes.code) must contain(metalBuilder.variantCodes.onlyElement)
      }
    }

    "Throws an error" - {
      "if updating adds too many SKUs" in new Product_ColorSizeOptions_ApiFixture {
        val bastardVariant  = buildVariantPayload("Derp", 666)
        val tooManyVariants = payloadBuilder.createProductPayload.variants :+ bastardVariant

        val updatePayload = UpdateProductPayload(
            attributes = Map.empty,
            variants = tooManyVariants.some,
            options = None,
            albums = None
        )

        productsApi(product.id)
          .update(updatePayload)
          .mustFailWithMessage(
              s"number of product variants for given options got ${variantsQty + 1}, expected $variantsQty or less")
      }
    }
  }

  "DELETE v1/products/:context/:id" - {

    "Product variants must be unlinked" in new Product_ColorSizeOptions_ApiFixture {
      productsApi(product.id).archive().as[Root].variants mustBe empty
    }

    "Product options must be unlinked" in new Product_ColorSizeOptions_ApiFixture {
      productsApi(product.id).archive().as[Root].options mustBe empty
    }

    "Albums must be unlinked" in new Product_ColorSizeOptions_ApiFixture {
      productsApi(product.id).archive().as[Root].albums mustBe empty
    }
  }
}
