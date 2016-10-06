defmodule Marketplace.ProductsFeedView do
  use Marketplace.Web, :view

  def render("index.json", %{products_feeds: products_feeds}) do
    %{data: render_many(products_feeds, Marketplace.ProductsFeedView, "products_feed.json")}
  end

  def render("show.json", %{products_feed: products_feed}) do
    %{data: render_one(products_feed, Marketplace.ProductsFeedView, "products_feed.json")}
  end

  def render("products_feed.json", %{products_feed: products_feed}) do
    %{id: products_feed.id,
      name: products_feed.name,
      url: products_feed.url,
      format: products_feed.format,
      schedule: products_feed.schedule}
  end
end
