defmodule Marketplace.MerchantApplication do
  use Marketplace.Web, :model

  schema "merchant_applications" do
    field :reference_number, Ecto.UUID, autogenerate: true
    field :name, :string
    field :business_name, :string
    field :phone_number, :string
    field :email_address, :string
    field :description, :string
    field :site_url, :string
    field :state, :string, default: "new"

    timestamps
   
    has_one :merchant_application_social_profile, Marketplace.MerchantApplicationSocialProfile
    has_one :merchant_application_business_profile, Marketplace.MerchantApplicationBusinessProfile
    has_one :social_profile, through: [:merchant_application_social_profile, :social_profile]
    has_one :business_profile, through: [:merchant_application_business_profile, :business_profile]
    belongs_to :merchant, Marketplace.Merchant
  end

  @states ~w(new approved rejected abandoned)a
  @required_fields ~w(name business_name email_address)
  @optional_fields ~w(description state merchant_id)

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields, @optional_fields)
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params,  @required_fields, @optional_fields)
  end

end
