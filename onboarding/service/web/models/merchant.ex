defmodule OnboardingService.Merchant do
  use OnboardingService.Web, :model

  schema "merchants" do
    field :name, :string
    field :business_name, :string
    field :phone_number, :string
    field :email_address, :string
    field :description, :string
    field :site_url, :string
    field :state, :string, default: "new"
    field :scope_id, :integer #Scopes live in Permissions/Solomon
    field :organization_id, :integer #Organizations live in Permissions/Solomon

    timestamps

    has_one :merchant_application, OnboardingService.MerchantApplication
    has_many :merchant_addresses, OnboardingService.MerchantAddress
    has_many :merchant_accounts, OnboardingService.MerchantAccount
    has_one :merchant_social_profile, OnboardingService.MerchantSocialProfile
    has_one :merchant_business_profile, OnboardingService.MerchantBusinessProfile
    has_one :social_profile, through: [:merchant_social_profile, :social_profile]
    has_one :business_profile, through: [:merchant_business_profile, :business_profile]
    has_one :merchant_products_feed, OnboardingService.MerchantProductsFeed
    has_one :products_feed, through: [:merchant_products_feed, :products_feed]
    has_one :merchant_products_upload, OnboardingService.MerchantProductsUpload
    has_one :products_upload, through: [:merchant_products_upload, :products_upload]
  end

  @states ~w(new approved suspended cancelled activated)s
  @required_fields ~w(business_name phone_number email_address site_url state)a
  @optional_fields ~w(name description scope_id organization_id)a

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_inclusion_code(:state, @states)
    |> validate_phone_number(:phone_number)
    |> validate_uri(:site_url)
    |> validate_email(:email_address)
    |> unique_constraint_code(:email_address, name: :merchant_email)
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_inclusion_code(:state, @states)
    |> validate_phone_number(:phone_number)
    |> validate_uri(:site_url)
    |> validate_email(:email_address)
    |> unique_constraint_code(:email_address, name: :merchant_email)
  end

end
