defmodule Marketplace.MerchantApplication do
  use Marketplace.Web, :model
  import Marketplace.Validation

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

  @states ~w(new approved rejected abandoned)s
  @required_fields ~w(name business_name email_address)a
  @optional_fields ~w(description state merchant_id phone_number site_url)a

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
    |> validate_phone_number(:phone_number)
    |> validate_uri(:site_url)
    |> validate_email(:email_address)
    |> validate_inclusion(:state, @states)
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
    |> validate_phone_number(:phone_number)
    |> validate_uri(:site_url)
    |> validate_email(:email_address)
    |> validate_inclusion(:state, @states)
  end

end
