defmodule TemplateBuilderSpec do
  use ESpec
  import Hyperion.Factory

  describe "submit_product_feed" do
    let list: submit_product_feed_data()
    let opts: %{purge_and_replace: true, seller_id: 123}
    let template: "<?xml version=\"1.0\" ?>\n<AmazonEnvelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"amzn- envelope.xsd\">\n<Header>\n  <DocumentVersion>1.01</DocumentVersion>\n  <MerchantIdentifier>123</MerchantIdentifier>\n</Header>\n<MessageType>Product</MessageType>\n<PurgeAndReplace>true</PurgeAndReplace>\n\n<Message>\n  <MessageID>1</MessageID>\n  <OperationType>Update</OperationType>\n    <Product>\n      <SKU>PARENTSKU123</SKU>\n      \n      <DescriptionData>\n        <Title>foo</Title>\n        <Brand></Brand>\n        <Description>bar baz</Description>\n        \n        <!-- <Manufacturer></Manufacturer>-->\n        \n          <SearchTerms>a</SearchTerms>\n        \n          <SearchTerms>b</SearchTerms>\n        \n          <SearchTerms>c</SearchTerms>\n        \n          <SearchTerms>d</SearchTerms>\n        \n        <!-- <ItemType></ItemType> -->\n        <!-- <IsGiftWrapAvailable>false</IsGiftWrapAvailable>\n        <IsGiftMessageAvailable>false</IsGiftMessageAvailable> -->\n      </DescriptionData>\n      <ProductData>\n        \n            \n        \n      </ProductData>\n    </Product>\n</Message>\n\n<Message>\n  <MessageID>2</MessageID>\n  <OperationType>Update</OperationType>\n    <Product>\n      <SKU>SKU123</SKU>\n      \n          \n              \n          \n        <ProductTaxCode></ProductTaxCode>\n        <LaunchDate>2017-03-09T02:21:07+00:00</LaunchDate>\n      \n      <DescriptionData>\n        <Title>foo</Title>\n        <Brand></Brand>\n        <Description>bar baz</Description>\n        \n        <!-- <Manufacturer></Manufacturer>-->\n        \n          <SearchTerms>a</SearchTerms>\n        \n          <SearchTerms>b</SearchTerms>\n        \n          <SearchTerms>c</SearchTerms>\n        \n          <SearchTerms>d</SearchTerms>\n        \n        <!-- <ItemType></ItemType> -->\n        <!-- <IsGiftWrapAvailable>false</IsGiftWrapAvailable>\n        <IsGiftMessageAvailable>false</IsGiftMessageAvailable> -->\n      </DescriptionData>\n      <ProductData>\n        \n            \n        \n      </ProductData>\n    </Product>\n</Message>\n\n</AmazonEnvelope>\n"

    it "should render full product template" do
      expect Hyperion.Amazon.TemplateBuilder.submit_product_feed(list(), opts())
      |> to(eq(template()))
    end
  end # submit_product_feed

  describe "submit_product_by_asin" do
    let list: [[code: "SKU", asin: "NBOEXMPL123"]]
    let opts: %{purge_and_replace: true, seller_id: 123}
    let template: "<?xml version=\"1.0\" ?>\n<AmazonEnvelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"amzn- envelope.xsd\">\n<Header>\n  <DocumentVersion>1.01</DocumentVersion>\n  <MerchantIdentifier>123</MerchantIdentifier>\n</Header>\n<MessageType>Product</MessageType>\n<PurgeAndReplace>true</PurgeAndReplace>\n<Message>\n  <MessageID>1</MessageID>\n  <OperationType>Update</OperationType>\n    \n      <Product>\n        <SKU>SKU</SKU>\n        <StandardProductID>\n          <Type>ASIN</Type>\n          <Value>NBOEXMPL123</Value>\n        </StandardProductID>\n      </Product>\n    \n</Message>\n</AmazonEnvelope>\n"

    it "should render full product template" do
      expect Hyperion.Amazon.TemplateBuilder.submit_product_by_asin(list(), opts())
      |> to(eq(template()))
    end
  end

  describe "submit_price_feed" do
    let list: [{[code: "SKU-TRL", retailprice: %{"currency" => "USD", "value" => 10500}], 1}]
    let opts: %{purge_and_replace: true, seller_id: 123}
    let template: "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<AmazonEnvelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"amzn-envelope.xsd\">\n  <Header>\n    <DocumentVersion>1.01</DocumentVersion>\n    <MerchantIdentifier>123</MerchantIdentifier>\n  </Header>\n  <MessageType>Price</MessageType>\n  \n  <Message>\n    <MessageID>1</MessageID>\n      <Price>\n        <SKU>SKU-TRL</SKU>\n        <StandardPrice currency=\"USD\">105.0</StandardPrice>\n      </Price>\n  </Message>\n  \n</AmazonEnvelope>\n"

    it "should render price template" do
      expect Hyperion.Amazon.TemplateBuilder.submit_price_feed(list(), opts())
      |> to(eq(template()))
    end
  end

  describe "submit_inventory_feed" do
    let list: [%{sku: "ARPS1", quantity: 10}, %{sku: "ARPM1", quantity: 10}]
    let opts: %{purge_and_replace: true, seller_id: 123}
    let template: "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<AmazonEnvelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"amzn-envelope.xsd\">\n  <Header>\n    <DocumentVersion>1.01</DocumentVersion>\n    <MerchantIdentifier>123</MerchantIdentifier>\n  </Header>\n  <MessageType>Inventory</MessageType>\n  \n</AmazonEnvelope>\n"

    it "should render inventory template" do
      expect Hyperion.Amazon.TemplateBuilder.submit_inventory_feed(list(), opts())
      |> to(eq(template()))
    end
  end


  describe "submit_images_feed" do
    let list: [[albums: [main: [%{"alt" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                          "id" => 7,
                          "src" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                          "title" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg"}],
                         swatches: [%{"alt" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                          "id" => 7,
                          "src" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                          "title" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg"}]],
                          code: "SKU-TRL"]]
    let opts: %{seller_id: 123}
    let template: "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n<AmazonEnvelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n                xsi:noNamespaceSchemaLocation=\"amzn-envelope.xsd\">\n  <Header>\n    <DocumentVersion>1.01</DocumentVersion>\n    <MerchantIdentifier>123</MerchantIdentifier>\n  </Header>\n  <MessageType>ProductImage</MessageType>\n  \n    <Message>\n  <MessageID>1</MessageID>\n  <OperationType>Update</OperationType>\n  <ProductImage>\n    <SKU>SKU-TRL</SKU>\n    <ImageType>Main</ImageType>\n    <ImageLocation>http://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg</ImageLocation>\n  </ProductImage>\n</Message>\n\n    <Message>\n  <MessageID>2</MessageID>\n  <OperationType>Update</OperationType>\n  <ProductImage>\n    <SKU>SKU-TRL</SKU>\n    <ImageType>Swatch</ImageType>\n    <ImageLocation>http://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg</ImageLocation>\n  </ProductImage>\n</Message>\n\n  \n</AmazonEnvelope>\n"

    it "should render images template" do
      expect Hyperion.Amazon.TemplateBuilder.submit_images_feed(list(), opts())
      |> to(eq(template()))
    end
  end

  describe "books_category" do
    let list: [author: "Gomer Simpson", bindingtypes: "paperback", language: "Esperanto"]
    let template: "<Books>\n  <ProductType>\n    <BooksMisc>\n      <Author>Gomer Simpson</Author>\n      <Binding>paperback</Binding>\n      <Language>Esperanto</Language>\n    </BooksMisc>\n  </ProductType>\n</Books>\n"

    it "should render books_category template" do
      expect Hyperion.Amazon.TemplateBuilder.books_category(list())
      |> to(eq(template()))
    end
  end

  describe "clothing_category" do
    context "when parent product passed" do
      let list: [parentage: "parent", department: "mens"]
      let template: "<ClothingAccessories>\n  <VariationData>\n      <Parentage>parent</Parentage>\n      \n    </VariationData>\n  <ClassificationData>\n    <Department>mens</Department>\n  </ClassificationData>\n</ClothingAccessories>\n"

      it "should render clothing_category template" do
        expect Hyperion.Amazon.TemplateBuilder.clothing_category(list())
        |> to(eq(template()))
      end
    end # when parent product passed

    context "when child product passed" do
      let list: [parentage: "child", department: "mens", size: "S", color: "Black"]
      let template: "<ClothingAccessories>\n  <VariationData>\n      <Parentage>child</Parentage>\n      \n        <Size>S</Size>\n        <Color>Black</Color>\n        <VariationTheme>SizeColor</VariationTheme>\n      \n    </VariationData>\n  <ClassificationData>\n    <Department>mens</Department>\n  </ClassificationData>\n</ClothingAccessories>\n"

      it "should render clothing_category template" do
        expect Hyperion.Amazon.TemplateBuilder.clothing_category(list())
        |> to(eq(template()))
      end
    end
  end
end