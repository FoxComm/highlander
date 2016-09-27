defmodule Marketplace.MerchantApplicationSocialProfile do 
  use Marketplace.Web, :model

  schema "merchant_application_social_profiles" do
    belongs_to :merchant_application, Marketplace.MerchantApplication
    belongs_to :social_profile, Marketplace.SocialProfile
  end

  @required_fields ~w(merchant_application_id social_profile_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end
end
