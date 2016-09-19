defmodule Marketplace.MerchantApplicationBusinessProfile do 
  use Marketplace.Web, :model

  schema "merchant_application_business_profiles" do
    belongs_to :merchant_application, Marketplace.MerchantApplication
    belongs_to :business_profile, Marketplace.BusinessProfile
  end

  @required_params ~w(merchant_application_id business_profile_id)
  @optional_params ~w()

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_params, @optional_params)
  end

end
