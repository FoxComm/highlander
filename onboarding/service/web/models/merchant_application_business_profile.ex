defmodule OnboardingService.MerchantApplicationBusinessProfile do 
  use OnboardingService.Web, :model

  schema "merchant_application_business_profiles" do
    belongs_to :merchant_application, OnboardingService.MerchantApplication
    belongs_to :business_profile, OnboardingService.BusinessProfile
  end

  @required_fields ~w(merchant_application_id business_profile_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> unique_constraint_code(:merchant_application_id, name: :merch_app_business_index)
  end

end
