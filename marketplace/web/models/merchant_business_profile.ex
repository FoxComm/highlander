defmodule Marketplace.MerchantBusinessProfile do 
  use Marketplace.Web, :model

  schema "merchant_business_profiles" do
    belongs_to :merchant, Marketplace.Merchant
    belongs_to :business_profile, Marketplace.BusinessProfile
  end

  @required_params ~w(merchant_id business_profile_id)
  @optional_params ~w()

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_params, @optional_params)
  end
end
