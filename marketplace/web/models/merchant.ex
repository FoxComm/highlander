defmodule Marketplace.Merchant do
  use Marketplace.Web, :model

  schema "merchants" do
    field :name, :string
    field :business_name, :string
    field :phone_number, :string
    field :email_address, :string
    field :description, :string
    field :site_url, :string
    field :state, :string, default: "new"

    timestamps

    has_one :merchant_application, Marketplace.MerchantApplication
    has_many :merchant_addresses, Marketplace.MerchantAddress
    has_many :merchant_accounts, Marketplace.MerchantAccount
    has_one :merchant_social_profile, Marketplace.MerchantSocialProfile
    has_one :merchant_business_profile, Marketplace.MerchantBusinessProfile
    has_one :social_profile, through: [:merchant_social_profile, :social_profile]
    has_one :business_profile, through: [:merchant_business_profile, :business_profile]
  end

  @states ~w(new approved suspended cancelled)a
  @required_fields ~w(name description state)
  @optional_fields ~w(business_name phone_number email_address site_url)

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields, @optional_fields)
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields, @optional_fields)
  end

end
