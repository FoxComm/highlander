defmodule Hyperion.EctoFactory do
  defmacro __using__(_opts) do
    quote do
      def category_factory do
        %Category{
          node_path: "Tees",
          node_id: 123,
          department: "Boys",
          item_type: "tees"
        }
      end

      def submission_result_factory do
        %SubmissionResult{
          product_id: 123,
          product_feed: nil,
          price_feed: nil,
          inventory_feed: nil,
          variations_feed: nil,
          images_feed: nil,
          product_feed_result: nil,
          price_feed_result: nil,
          inventory_feed_result: nil,
          variations_feed_result: nil,
          images_feed_result: nil,
          completed: false
        }
      end
    end
  end
end
