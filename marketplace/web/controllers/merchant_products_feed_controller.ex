defmodule Marketplace.MerchantProductsFeedController do
  use Marketplace.Web, :controller

  alias Ecto.Multi
  alias Marketplace.ProductsFeed
  alias Marketplace.MerchantProductsFeed

  def index(conn, _params) do
    merchant_products_feeds = Repo.all(MerchantProductsFeed)
    render(conn, "index.json", merchant_products_feeds: merchant_products_feeds)
  end

  def create(conn, %{"products_feed" => products_feed_params, "merchant_id" => merchant_id}) do
    case Repo.transaction(insert_and_relate(products_feed_params, merchant_id)) do
      {:ok, %{products_feed: products_feed, merchant_products_feed: m_pf}} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_products_feed_path(conn, :show, merchant_id))
        |> render(ProductsFeedView, "products_feed.json", products_feed: products_feed)
      {:error, failed_operation, failed_value, changes_completed} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"merchant_id" => m_id}) do
    m_pf = Repo.get_by!(MerchantProductsFeed, merchant_id: m_id)
    |> Repo.preload(:products_feed)

    conn
    |> render(ProductsFeedView, "show.json", products_feed: m_pf.products_feed)
  end

  def update(conn, %{"merchant_id" => m_id, "products_feed" => products_feed_params}) do
    m_pf = Repo.get_by!(MerchantProductsFeed, merchant_id: m_id)
    |> Repo.preload(:products_feed)

    changeset = ProductsFeed.update_changeset(m_pf.products_feed, products_feed_params)
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
