defmodule OnboardingService.MerchantApplicationSocialProfile do 
  use OnboardingService.Web, :model

  schema "merchant_application_social_profiles" do
    belongs_to :merchant_application, OnboardingService.MerchantApplication
    belongs_to :social_profile, OnboardingService.SocialProfile
  end

  @required_fields ~w(merchant_application_id social_profile_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> unique_constraint_code(:merchant_application_id, name: :merch_app_social_index)
  end
end
