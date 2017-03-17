defmodule Hyperion.Amazon.PusherSpec do
  use ESpec
  import Hyperion.Factory

  describe "push" do
    context "when no product pushed" do
      let product: build(:sku_with_images)
      let subm_results: build(:submission_result)
      let feed_result: {:success, %{"SubmitFeedResponse" => %{"SubmitFeedResult" => %{"FeedSubmissionInfo" => %{"Foo" => "BAR"}}}}}

      before do
        allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> product() end)
        allow(Hyperion.Amazon.Pusher).to accept(:get_submisstion_result, fn(_, false) -> subm_results() end)
        allow(MWSClient).to accept(:submit_product_feed, fn(_, _) -> feed_result() end)
        allow(MWSClient).to accept(:submit_price_feed, fn(_, _) -> feed_result() end)
        allow(MWSClient).to accept(:submit_inventory_feed, fn(_, _) -> feed_result() end)
        allow(MWSClient).to accept(:submit_images_feed, fn(_, _) -> feed_result() end)
      end

      it "should execute all steps" do
        res = Hyperion.Amazon.Pusher.push(1, %MWSClient.Config{seller_id: 123}, "JWT", false, [%{sku: 123, quantity: 1}])
        expect res.product_feed   |> to(eq(%{"Foo" => "BAR"}))
        expect res.price_feed     |> to(eq(%{"Foo" => "BAR"}))
        expect res.inventory_feed |> to(eq(%{"Foo" => "BAR"}))
        expect res.images_feed    |> to(eq(%{"Foo" => "BAR"}))
        expect res.product_id     |> to(eq(product.body["id"]))
      end
    end # when no product pushed
  end # push
end