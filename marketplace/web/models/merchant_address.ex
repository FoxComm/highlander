defmodule Marketplace.MerchantAddress do
  use Marketplace.Web, :model

  schema "merchant_addresses" do 
    field :name, :string
    field :address1, :string
    field :address2, :string
    field :city, :string
    field :state, :string
    field :zip, :string
    field :is_headquarters, :boolean
    field :phone_number, :string

    timestamps

    belongs_to :merchant, Marketplace.Merchant
  end
end
