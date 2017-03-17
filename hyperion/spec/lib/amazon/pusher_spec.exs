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
        allow(SubmissionResult).to accept(:first_or_create, fn(_) -> subm_results() end)
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

    context "when only product data and price pushed" do
      let product: build(:sku_with_images)
      let subm_results: build(:submission_result, %{product_feed: %{"foo" => "bar"}, price_feed: %{"foo" => "bar"}})
      let feed_result: {:success, %{"SubmitFeedResponse" => %{"SubmitFeedResult" => %{"FeedSubmissionInfo" => %{"BAR" => "BAZ"}}}}}

      before do
        Hyperion.Repo.insert!(subm_results)
        allow(Hyperion.PhoenixScala.Client).to accept(:get_product, fn(_, _) -> product() end)
        allow(SubmissionResult).to accept(:first_or_create, fn(_) -> subm_results() end)
        allow(MWSClient).to accept(:submit_inventory_feed, fn(_, _) -> feed_result() end)
        allow(MWSClient).to accept(:submit_images_feed, fn(_, _) -> feed_result() end)
      end

      it "should execute only ivnentory and images steps" do
        res = Hyperion.Amazon.Pusher.push(1, %MWSClient.Config{seller_id: 123}, "JWT", false, [%{sku: 123, quantity: 1}])
        expect res.product_feed   |> to(eq(%{"Foo" => "BAR"}))
        expect res.price_feed     |> to(eq(%{"Foo" => "BAR"}))
        expect res.inventory_feed |> to(eq(%{"BAR" => "BAZ"}))
        expect res.images_feed    |> to(eq(%{"BAR" => "BAZ"}))
        expect res.product_id     |> to(eq(product.body["id"]))
      end
    end #product and price are pushed
  end # push
end