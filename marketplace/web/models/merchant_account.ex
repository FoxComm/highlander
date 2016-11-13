defmodule Marketplace.MerchantAccount do
  use Marketplace.Web, :model

  schema "merchant_accounts" do
    field :first_name, :string
    field :last_name, :string
    field :phone_number, :string
    field :business_name, :string
    field :description, :string
    field :email_address, :string
    field :solomon_id, :integer
    field :stripe_account_id, :string

    timestamps

    belongs_to :merchant, Marketplace.Merchant

  end

  @required_fields ~w(first_name last_name email_address merchant_id)a
  @optional_fields ~w(phone_number business_name description solomon_id stripe_account_id)a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_phone_number(:phone_number)
    |> validate_email(:email_address)
    |> unique_constraint_code(:email_address, name: :merchant_account_email)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_phone_number(:phone_number)
    |> validate_email(:email_address)
    |> unique_constraint_code(:email_address, name: :merchant_account_email)
  end
end
