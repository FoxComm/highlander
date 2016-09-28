defmodule Marketplace.MerchantAddress do
  use Marketplace.Web, :model
  import Marketplace.Validation

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

  @required_fields ~w(name address1 city state zip)a
  @optional_fields ~w(address2 is_headquarters phone_number)a
  
  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
    |> validate_phone_number(:phone_number)
    |> validate_postal(:zip)
    |> validate_US_state(:state)
  end
end
