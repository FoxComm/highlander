defmodule Marketplace.MerchantApplicationBusinessProfile do 
  use Marketplace.Web, :model
  import Marketplace.Validation

  schema "merchant_application_business_profiles" do
    belongs_to :merchant_application, Marketplace.MerchantApplication
    belongs_to :business_profile, Marketplace.BusinessProfile
  end

  @required_fields ~w(merchant_application_id business_profile_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
  end

end
