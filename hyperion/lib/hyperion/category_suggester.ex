defmodule CategorySuggester do
  import Ecto.Query
  @moduledoc """
  Suggests category for the product by it's title
  """

  def suggest_categories(product_name, cfg) do
    asin = get_product_asin(product_name, cfg)
    case MWSClient.get_product_categories_for_asin(asin, cfg) do
      {:success, resp} ->
        category = case resp["GetProductCategoriesForASINResponse"]["GetProductCategoriesForASINResult"]["Self"] do
                     x when is_list(x) -> hd x#if (Enum.count(x) > 1), do: tl(x) |> hd , else: hd(x)
                     x -> x
                   end
        get_all_categories([node_id: category["ProductCategoryId"], name: category["ProductCategoryName"]])
      {_, _} ->
        raise "No categories found for given ASIN"
    end
  end

  defp get_product_asin(query, cfg) do
    case MWSClient.list_matching_products(query, cfg) do
      {:success, res} ->
        product = hd(res["ListMatchingProductsResponse"]["ListMatchingProductsResult"]["Products"]["Product"])
        product["Identifiers"]["MarketplaceASIN"]["ASIN"]
      {_, _} ->
        raise "ASIN not found"
    end
  end


  def get_all_categories([node_id: node_id, name: name]) do
    main_category = (from c in Category, select: %{node_id: c.node_id, node_path: c.node_path,
                                                   department: c.department, item_type: c.item_type})
                    |> where([c], c.node_id in ^[node_id])
                    |> Hyperion.Repo.all
    all_categories = Category.search(name)
    case main_category do
      [] -> %{primary: nil, secondary: all_categories.items, count: Enum.count(all_categories.items)}
      _ -> merge_categories(all_categories, main_category)
    end
  end

  def merge_categories(all, main) do
    main_category = hd(main)
    filtered = all.items
               |> Enum.reject(fn(category) -> category.node_id == main_category.node_id end)

    %{primary: main_category, secondary: filtered, count: Enum.count(filtered) + 1}
  end
end