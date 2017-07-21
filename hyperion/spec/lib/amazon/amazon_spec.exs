defmodule AmazonSpec do
  use ESpec
  import Hyperion.Factory

  describe "get_product" do
    context "when no variants present" do
      let resp: build(:product_without_varians)

      let product: [{[activefrom: "2017-02-28T10:38:32.559Z", description: "Stylish fit, stylish finish.",
                      tags: ["sunglasses"], title: "Fox", activefrom: "2017-02-28T10:38:33.627Z", amazon: true,
                      code: "SKU-TRL", retailprice: %{"currency" => "USD", "value" => 10500},
                      saleprice: %{"currency" => "USD", "value" => 10000}, tags: ["sunglasses"], title: "Fox"], 1}]
      before do: allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> resp() end)
      it "returns atomized products Keyword structure" do
        expect(Hyperion.Amazon.product_feed([1], "foo")).to eq(product())
      end
    end # with variants

    context "when variants not present" do
      let resp: build(:product_with_variants)
      let product: [{[parentage: "parent", color: "white", size: "S", activefrom: "2017-03-09T02:21:07.763Z",
                      description: "<p>bar baz</p>", tags: ["a", "b", "c", "d"], title: "foo",
                      activefrom: "2017-03-09T02:21:07.763Z", amazon: true,
                      code: "PARENTSKU123", retailprice: %{"currency" => "USD", "value" => 1000},
                      saleprice: %{"currency" => "USD", "value" => 0}], 1},
                    {[parentage: "child", color: "white", size: "S", activefrom: "2017-03-09T02:21:07.763Z",
                      description: "<p>bar baz</p>", tags: ["a", "b", "c", "d"], title: "foo",
                      activefrom: "2017-03-09T02:21:07.763Z", amazon: true, code: "SKU123",
                      retailprice: %{"currency" => "USD", "value" => 1000},
                      saleprice: %{"currency" => "USD", "value" => 0}], 2}]

      before do: allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> resp() end)

      it "returns atomized products Keyword structure" do
        expect(Hyperion.Amazon.product_feed([1], "foo")).to eq(product())
      end
    end # no variants
  end # get_product

  describe "price_feed" do
    let resp: build(:product_without_varians)
    let product: [{[code: "SKU-TRL", retailprice: %{"currency" => "USD", "value" => 10500}], 1}]
    before do: allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> resp() end)

    it "returns atomized prices for products" do
      expect(Hyperion.Amazon.price_feed([1], "foo")).to eq(product())
    end
  end # price_feed

  describe "images_feed" do
    context "when product have images" do
      let resp: build(:sku_with_images)
      let images: [{{"XMENTEEX1", "Main", "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/245/81B6PnROB1L._UX522_.jpg"}, 1}]

      before do: allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> resp() end)

      it "returns atomized images for products" do
        expect(Hyperion.Amazon.images_feed([1], "foo")).to eq(images())
      end
    end # when product have images

    context "when product have no images" do
      let resp: build(:sku_with_images)
      before do: allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> resp() end)

      it "raises an exception" do
        expect Hyperion.Amazon.images_feed([1], "foo") |> to(raise_exception())
      end
    end # when product have no images
  end # images_feed
end # AmazonSpec
