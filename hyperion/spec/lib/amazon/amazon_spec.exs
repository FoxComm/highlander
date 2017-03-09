defmodule AmazonSpec do
  use ESpec
  import Hyperion.ProductFactory

  describe "get_product" do
    context "when no variants present" do
      let resp: product_without_varians()
      let product: [{[parentage: "parent", activefrom: "2017-02-28T10:38:32.559Z",
                      description: "Stylish fit, stylish finish.", tags: ["sunglasses"],
                      title: "Fox", activefrom: "2017-02-28T10:38:33.627Z", code: "PARENTSKU-TRL",
                      retailprice: %{"currency" => "USD", "value" => 10500},
                      saleprice: %{"currency" => "USD", "value" => 10000}, tags: ["sunglasses"],
                      title: "Fox"], 1},
                    {[parentage: "child", activefrom: "2017-02-28T10:38:32.559Z",
                      description: "Stylish fit, stylish finish.", tags: ["sunglasses"],
                      title: "Fox", activefrom: "2017-02-28T10:38:33.627Z", code: "SKU-TRL",
                      retailprice: %{"currency" => "USD", "value" => 10500},
                      saleprice: %{"currency" => "USD", "value" => 10000}, tags: ["sunglasses"],
                      title: "Fox"], 2}]
      before do: allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> resp() end)
      it "returns atomized products Keyword structure" do
        expect(Hyperion.Amazon.product_feed([1], "foo")).to eq(product())
      end
    end # with variants

    context "when variants not present" do
      let resp: product_with_variants()
      let product: [{[parentage: "parent", color: "white", size: "S",
                      activefrom: "2017-03-09T02:21:07.763Z", description: "<p>bar baz</p>",
                      tags: ["a", "b", "c", "d"], title: "foo",
                      activefrom: "2017-03-09T02:21:07.763Z", code: "PARENTSKU123",
                      retailprice: %{"currency" => "USD", "value" => 1000},
                      saleprice: %{"currency" => "USD", "value" => 0}], 1},
                    {[parentage: "child", color: "white", size: "S",
                      activefrom: "2017-03-09T02:21:07.763Z", description: "<p>bar baz</p>",
                      tags: ["a", "b", "c", "d"], title: "foo",
                      activefrom: "2017-03-09T02:21:07.763Z", code: "SKU123",
                      retailprice: %{"currency" => "USD", "value" => 1000},
                      saleprice: %{"currency" => "USD", "value" => 0}], 2}]
      before do: allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> resp() end)
      it "returns atomized products Keyword structure" do
        expect(Hyperion.Amazon.product_feed([1], "foo")).to eq(product())
      end
    end # no variants
  end # get_product

  describe "price_feed" do
    let resp: product_without_varians()
    let product: [{[code: "SKU-TRL", retailprice: %{"currency" => "USD", "value" => 10500}], 1}]
    before do: allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> resp() end)

    it "returns atomized prices for products" do
      expect(Hyperion.Amazon.price_feed([1], "foo")).to eq(product())
    end
  end # price_feed

  describe "images_feed" do
    context "when product have images" do
      let resp: sku_with_images()
      let images: [[albums: [Fox: [%{"alt" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                            "id" => 7,
                            "src" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                            "title" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg"}]],
                            code: "SKU-TRL"]]
      before do: allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> resp() end)

      it "returns atomized images for products" do
        expect(Hyperion.Amazon.images_feed([1], "foo")).to eq(images())
      end
    end # when product have images

    context "when product have no images" do
      let resp: sku_with_images()
      before do: allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> resp() end)

      it "raises an exception" do
        expect Hyperion.Amazon.images_feed([1], "foo") |> to(raise_exception())
      end
    end # when product have no images
  end # images_feed
end # AmazonSpec
