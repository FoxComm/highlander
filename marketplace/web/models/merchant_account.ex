defmodule Marketplace.MerchantAccount do
  use Marketplace.Web, :model
  import Marketplace.Validation

  schema "merchant_accounts" do
    field :first_name, :string
    field :last_name, :string
    field :phone_number, :string
    field :business_name, :string
    field :description, :string
    field :email_address, :string
    field :password, :string
    field :solomon_id, :integer

    timestamps

    belongs_to :merchant, Marketplace.Merchant

  end

  @required_fields ~w(first_name last_name email_address password)a
  @optional_fields ~w(phone_number business_name description)a

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_phone_number(:phone_number)
    |> validate_email(:email_address)
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_phone_number(:phone_number)
    |> validate_email(:email_address)
  end
end
