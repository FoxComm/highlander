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
                     x when is_list(x) -> pick_right_category(x)
                     x -> x
                   end
        get_all_categories([node_id: category["ProductCategoryId"], name: category["ProductCategoryName"]])
      {_, _} ->
        raise "No categories found for given ASIN"
    end
  end

  # Amazon response is a list.
  # Sometimes needed element plased in head, sometimes in tail
  # We just need to pick correct one
  def pick_right_category(data) do
    cond do
      hd(data)["ProductCategoryName"] == "Shops" ->
        tl(data) |> hd
      true -> hd(data)
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

    all_categories = (from c in Category, where: ilike(c.node_path,^"%#{String.downcase(name)}%") and c.node_id != ^node_id )
                     |> Hyperion.Repo.all
    case main_category do
      [] -> %{primary: nil, secondary: all_categories, count: Enum.count(all_categories)}
      _ -> %{primary: hd(main_category), secondary: all_categories, count: Enum.count(all_categories) + 1}
    end
  end
end


