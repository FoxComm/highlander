defmodule Marketplace.MerchantProductsFeedView do
  use Marketplace.Web, :view

  def render("index.json", %{merchant_products_feeds: merchant_products_feeds}) do
    %{data: render_many(merchant_products_feeds, Marketplace.MerchantProductsFeedView, "merchant_products_feed.json")}
  end

  def render("show.json", %{merchant_products_feed: merchant_products_feed}) do
    %{data: render_one(merchant_products_feed, Marketplace.MerchantProductsFeedView, "merchant_products_feed.json")}
  end

  def render("merchant_products_feed.json", %{merchant_products_feed: merchant_products_feed}) do
    %{id: merchant_products_feed.id,
      merchant_id: merchant_products_feed.merchant_id,
      products_feed_id: merchant_products_feed.products_feed_id}
  end
end
