defmodule PushCheckerWorkerSpec do
  use ESpec
  import Hyperion.Factory

  describe "handle_info" do
    let attrs:  %{product_feed: %{"FeedSubmissionId" => "123"},
                price_feed: %{"FeedSubmissionId" => "123"},
                inventory_feed: %{"FeedSubmissionId" => "123"},
                images_feed: %{"FeedSubmissionId" => "123"},
                variations_feed: %{"FeedSubmissionId" => "123"}}
    let pushes: build(:submission_result, attrs)

    before do
      Hyperion.Repo.insert!(pushes)
      allow(Hyperion.Amazon).to accept(:fetch_config, fn() -> %MWSClient.Config{} end)
      allow(MWSClient).to accept(:get_feed_submission_result, fn(_, _) ->
        {:ok, %{"AmazonEnvelope" =>
                %{"Message" =>
                  %{"ProcessingReport" =>
                    %{"Foo" => :crypto.strong_rand_bytes(32) |> Base.encode64}}}}}
      end)
    end

    it "should check push" do
      expect Hyperion.Amazon.Workers.PushCheckerWorker.handle_info(:work, "foo")
      |> to(eq({:noreply, "foo"}))

      r = Hyperion.Repo.all(SubmissionResult) |> hd
      expect r.completed |> to(be_true())
    end
  end
end
