defmodule Marketplace.MerchantProductsFeedController do
  use Marketplace.Web, :controller

  alias Ecto.Multi
  alias Marketplace.ProductsFeed
  alias Marketplace.ProductsFeedView
  alias Marketplace.MerchantProductsFeed

  def index(conn, params), do: secured_route(conn, params, &index/3)
  defp index(conn, %{"merchant_id" => merchant_id}, claims) do
    products_feeds = Repo.all(from pf in ProductsFeed,
                              join: mpf in MerchantProductsFeed,
                              where: pf.id == mpf.products_feed_id
                              and mpf.merchant_id == ^merchant_id,
                              select: pf)
    render(conn, ProductsFeedView, "index.json", products_feeds: products_feeds)
  end

  def create(conn, params), do: secured_route(conn, params, &create/3)
  defp create(conn, %{"products_feed" => products_feed_params, "merchant_id" => merchant_id}, claims) do
    case Repo.transaction(insert_and_relate(products_feed_params, merchant_id)) do
      {:ok, %{products_feed: products_feed, merchant_products_feed: m_pf}} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_products_feed_path(conn, :index, merchant_id))
        |> render(ProductsFeedView, "products_feed.json", products_feed: products_feed)
      {:error, failed_operation, failed_value, changes_completed} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, params), do: secured_route(conn, params, &show/3)
  defp show(conn, %{"id" => id}, claims) do
    products_feed = Repo.get_by!(ProductsFeed, id: id)

    conn
    |> render(ProductsFeedView, "show.json", products_feed: products_feed)
  end

  def update(conn, params), do: secured_route(conn, params, &update_/3)
  defp update_(conn, %{"id" => id, "products_feed" => products_feed_params}, claims) do
    products_feed = Repo.get_by!(ProductsFeed, id: id)
    changeset = ProductsFeed.update_changeset(products_feed, products_feed_params)
    case Repo.update(changeset) do
      {:ok, products_feed} ->
        conn
        |> render(ProductsFeedView, "products_feed.json", products_feed: products_feed)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  defp insert_and_relate(products_feed_params, merchant_id) do
    pf_cs = ProductsFeed.changeset(%ProductsFeed{}, products_feed_params)
    Multi.new
    |> Multi.insert(:products_feed, pf_cs)
    |> Multi.run(:merchant_products_feed, fn %{products_feed: products_feed} ->
      map_products_feed_to_merchant(products_feed, merchant_id) end
    )
  end

  defp map_products_feed_to_merchant(products_feed, merchant_id) do
    mpf_cs = MerchantProductsFeed.changeset(%MerchantProductsFeed{}, %{
        "merchant_id" => merchant_id,
        "products_feed_id" => products_feed.id
      })

    Repo.insert(mpf_cs)
  end
end
