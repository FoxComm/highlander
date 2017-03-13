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
    end
  end
end