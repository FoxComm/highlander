defmodule Marketplace.MerchantSocialProfile do 
  use Marketplace.Web, :model

  schema "merchant_social_profiles" do
    belongs_to :merchant, Marketplace.Merchant
    belongs_to :social_profile, Marketplace.SocialProfile
  end

  @required_params ~w(merchant_id social_profile_id)
  @optional_params ~w()

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_params, @optional_params)
  end
end
