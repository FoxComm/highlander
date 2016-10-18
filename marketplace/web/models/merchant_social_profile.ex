defmodule Marketplace.MerchantSocialProfile do 
  use Marketplace.Web, :model

  schema "merchant_social_profiles" do
    belongs_to :merchant, Marketplace.Merchant
    belongs_to :social_profile, Marketplace.SocialProfile
  end

  @required_fields ~w(merchant_id social_profile_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> unique_constraint_code(:merchant_id)
  end
end
