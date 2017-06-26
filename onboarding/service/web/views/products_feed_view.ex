defmodule OnboardingService.ProductsFeedView do
  use OnboardingService.Web, :view

  def render("index.json", %{products_feeds: products_feeds}) do
    %{product_feeds: render_many(products_feeds, OnboardingService.ProductsFeedView, "products_feed.json")}
  end

  def render("show.json", %{products_feed: products_feed}) do
    %{product_feed: render_one(products_feed, OnboardingService.ProductsFeedView, "products_feed.json")}
  end

  def render("products_feed.json", %{products_feed: products_feed}) do
    %{id: products_feed.id,
      name: products_feed.name,
      url: products_feed.url,
      format: products_feed.format,
      schedule: products_feed.schedule}
  end
end
