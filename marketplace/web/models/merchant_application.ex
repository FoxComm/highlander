defmodule Marketplace.MerchantApplication do
  use Marketplace.Web, :model

  schema "merchant_applications" do
    field :reference_number, Ecto.UUID, autogenerate: true
    field :name, :string
    field :business_name, :string
    field :email_address, :string
    field :description, :string
    field :state, :string, default: "new"

    timestamps
   
    has_one :merchant_application_social_profile, Permissions.MerchantApplicationSocialProfile
    has_one :merchant_application_business_profile, Permissions.MerchantApplicationBusinessProfile
    has_one :social_profile, through: [:merchant_application_social_profile, :social_profile]
    has_one :business_profile, through: [:merchant_application_business_profile, :business_profile]
  end

  @states ~w(new approved rejected abandoned)a
  @required_fields ~w(name business_name email_address)
  @optional_fields ~w(description)

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields, @optional_fields)
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params,  @required_fields, @optional_fields)
  end

end
