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
          images_feed: nil
        }
      end
    end
  end
end