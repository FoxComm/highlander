defmodule Marketplace.MerchantAddress do
  use Marketplace.Web, :model

  schema "merchant_addresses" do 
    field :name, :string
    field :address1, :string
    field :address2, :string
    field :city, :string
    field :state, :string
    field :zip, :string
    field :is_headquarters, :boolean, default: false
    field :phone_number, :string

    timestamps

    belongs_to :merchant, Marketplace.Merchant
  end

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(name address1 city state zip), ~w(address2 is_headquarters phone_number))
  end
end
