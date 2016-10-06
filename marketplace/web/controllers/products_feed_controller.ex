defmodule Marketplace.ProductsFeedController do
  use Marketplace.Web, :controller

  alias Marketplace.ProductsFeed

  def index(conn, _params) do
    products_feeds = Repo.all(ProductsFeed)
    render(conn, "index.json", products_feeds: products_feeds)
  end

  def create(conn, %{"products_feed" => products_feed_params}) do
    changeset = ProductsFeed.changeset(%ProductsFeed{}, products_feed_params)

    case Repo.insert(changeset) do
      {:ok, products_feed} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", products_feed_path(conn, :show, products_feed))
        |> render("show.json", products_feed: products_feed)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "error.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    products_feed = Repo.get!(ProductsFeed, id)
    render(conn, "show.json", products_feed: products_feed)
  end

  def update(conn, %{"id" => id, "products_feed" => products_feed_params}) do
    products_feed = Repo.get!(ProductsFeed, id)
    changeset = ProductsFeed.changeset(products_feed, products_feed_params)

    case Repo.update(changeset) do
      {:ok, products_feed} ->
        render(conn, "show.json", products_feed: products_feed)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "error.json", changeset: changeset)
    end
  end

  def delete(conn, %{"id" => id}) do
    products_feed = Repo.get!(ProductsFeed, id)

    # Here we use delete! (with a bang) because we expect
    # it to always work (and if it does not, it will raise).
    Repo.delete!(products_feed)

    send_resp(conn, :no_content, "")
  end
end
