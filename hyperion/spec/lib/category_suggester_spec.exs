defmodule Hyperion.Amazon.CategorySuggesterSpec do
  use ESpec
  import Hyperion.Factory

  describe "suggest_categories" do
    context "when q and title are passed" do
      let asin: build(:product_by_title)
      let amazon_categories: build(:categories_by_asin)
      let main_category: build(:category, %{node_id: 1252178011})
      let secondary_category: build(:category, %{node_path: "Foo"})

      before do
        Hyperion.Repo.insert!(main_category)
        Hyperion.Repo.insert!(secondary_category)
        allow(MWSClient).to accept(:list_matching_products, fn(_, _) -> {:success, asin} end)
        allow(MWSClient).to accept(:get_product_categories_for_asin, fn(_, _) -> {:success, amazon_categories} end)
      end

      it "should return primary and secondary categories" do
        res = Hyperion.Amazon.CategorySuggester.suggest_categories(%{q: "Foo", title: "Bar", limit: 15}, %{})
        expect res.count |> to(eq(2))
        expect res |> to(have_key(:primary))
        expect res |> to(have_key(:secondary))
      end
    end # q and title

    context "when only title given" do
      let asin: build(:product_by_title)
      let amazon_categories: build(:categories_by_asin)
      let main_category: build(:category, %{node_id: 1252178011})

      before do
        Hyperion.Repo.insert!(main_category)
        allow(MWSClient).to accept(:list_matching_products, fn(_, _) -> {:success, asin} end)
        allow(MWSClient).to accept(:get_product_categories_for_asin, fn(_, _) -> {:success, amazon_categories} end)
      end

      it "should return only main category" do
        expect Hyperion.Amazon.CategorySuggester.suggest_categories(%{title: "Bar", limit: 15}, %{})
        |> to eq(%{count: 1, primary: [%{department: "Boys", item_type: "tees", node_id: 1252178011,
                                         node_path: "Tees", size_opts: nil}], secondary: nil})
      end
    end # only title given

    context "when only query string is given" do
      let secondary_category: build(:category, %{node_path: "Foo"})

      before do
        Hyperion.Repo.insert!(secondary_category)
      end

      it "should return only second category" do
        res = Hyperion.Amazon.CategorySuggester.suggest_categories(%{q: "Foo", limit: 15}, %{})
        expect res.count |> to eq(1)
        expect res.primary |> to eq(nil)
      end
    end # only q is given

    context "when no data given" do
      it "should return empty result" do
        expect Hyperion.Amazon.CategorySuggester.suggest_categories(%{}, %{})
        |> to eq(%{count: 0, primary: nil, secondary: nil})
      end
    end # when no data given
  end # suggest_categories
end
