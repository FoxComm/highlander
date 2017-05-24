defmodule Marketplace.MerchantProductsFeed do
  use Marketplace.Web, :model

  schema "merchant_products_feeds" do
    belongs_to :merchant, Marketplace.Merchant
    belongs_to :products_feed, Marketplace.ProductFeed
  end

  @required_fields ~w(merchant_id products_feed_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
  end
end