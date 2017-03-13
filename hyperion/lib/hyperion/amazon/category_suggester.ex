defmodule Hyperion.Amazon.CategorySuggester do
  import Ecto.Query
  @moduledoc """
  Suggests category for the product by it's title and/or query_string
  """

  @doc """
  Suggests category by query and title
  """
  def suggest_categories(%{q: q, title: title, limit: limit}, cfg) do
    asin = get_product_asin(title, cfg)
    primary = get_primary_category(asin, cfg)
    secondary = fetch_secondary_categories(q, primary[:node_id], limit)
    case primary[:data] do
      nil -> %{primary: nil, secondary: secondary, count: Enum.count(secondary)}
      _ -> %{primary: primary[:data], secondary: secondary, count: Enum.count(primary[:data] ++ secondary)}
    end
  end

  @doc """
  Suggests category only by query
  """
  def suggest_categories(%{q: q, limit: limit}, cfg) do
    secondary = fetch_secondary_categories(q, nil, limit)
    %{primary: nil, secondary: secondary, count: Enum.count(secondary)}
  end

  @doc """
  Suggests category only by title
  """
  def suggest_categories(%{title: title, limit: _limit}, cfg) do
    asin = get_product_asin(title, cfg)
    primary = get_primary_category(asin, cfg)
    case primary[:data] do
      nil -> %{primary: nil, secondary: nil, count: 0}
      _ -> %{primary: primary[:data], secondary: nil, count: Enum.count primary[:data]}
    end
  end

  @doc """
  Return empty result when no params passed
  """
  def suggest_categories(%{}, _), do: %{primary: nil, secondary: nil, count: 0}

  # Amazon response is a list.
  # Sometimes needed element plased in head, sometimes in tail
  # We just need to pick correct one
  defp pick_right_category(data) do
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

  defp get_primary_category(asin, cfg) do
    case MWSClient.get_product_categories_for_asin(asin, cfg) do
      {:success, resp} ->
        category = case resp["GetProductCategoriesForASINResponse"]["GetProductCategoriesForASINResult"]["Self"] do
                     x when is_list(x) -> pick_right_category(x)
                     x -> x
                   end
        fetch_main_category_details(category["ProductCategoryId"])
      {_, _} ->
        raise "No categories found for given ASIN"
    end
  end

  defp fetch_main_category_details(node_id) do
    data = (from c in Category, select: %{node_id: c.node_id, node_path: c.node_path, size_opts: c.size_opts,
                                          department: c.department, item_type: c.item_type})
           |> where([c], c.node_id in ^[node_id])
           |> Hyperion.Repo.all
    case data do
      [] -> %{data: nil, node_id: node_id}
      _ -> %{data: data, node_id: node_id}
    end
  end

  defp fetch_secondary_categories(q, nil, limit) do
    (from c in Category, where: ilike(c.node_path,^"%#{String.downcase(q)}%") and not is_nil(c.node_id), limit: ^limit)
    |> Hyperion.Repo.all
  end

  defp fetch_secondary_categories(q, node_id, limit) do
    (from c in Category,
     where: ilike(c.node_path,^"%#{String.downcase(q)}%") and c.node_id != ^node_id and not is_nil(c.node_id),
     limit: ^limit)
    |> Hyperion.Repo.all
  end
end


