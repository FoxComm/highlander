defmodule Marketplace.MerchantProductsFeedController do
  use Marketplace.Web, :controller

  alias Marketplace.MerchantProductsFeed

  def index(conn, _params) do
    merchant_products_feeds = Repo.all(MerchantProductsFeed)
    render(conn, "index.json", merchant_products_feeds: merchant_products_feeds)
  end

  def create(conn, %{"merchant_products_feed" => merchant_products_feed_params}) do
    changeset = MerchantProductsFeed.changeset(%MerchantProductsFeed{}, merchant_products_feed_params)

    case Repo.insert(changeset) do
      {:ok, merchant_products_feed} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_products_feed_path(conn, :show, merchant_products_feed))
        |> render("show.json", merchant_products_feed: merchant_products_feed)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "error.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    merchant_products_feed = Repo.get!(MerchantProductsFeed, id)
    render(conn, "show.json", merchant_products_feed: merchant_products_feed)
  end

  def update(conn, %{"id" => id, "merchant_products_feed" => merchant_products_feed_params}) do
    merchant_products_feed = Repo.get!(MerchantProductsFeed, id)
    changeset = MerchantProductsFeed.changeset(merchant_products_feed, merchant_products_feed_params)

    case Repo.update(changeset) do
      {:ok, merchant_products_feed} ->
        render(conn, "show.json", merchant_products_feed: merchant_products_feed)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "error.json", changeset: changeset)
    end
  end

  def delete(conn, %{"id" => id}) do
    merchant_products_feed = Repo.get!(MerchantProductsFeed, id)

    # Here we use delete! (with a bang) because we expect
    # it to always work (and if it does not, it will raise).
    Repo.delete!(merchant_products_feed)

    send_resp(conn, :no_content, "")
  end
end
