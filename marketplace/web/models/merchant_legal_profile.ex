defmodule Marketplace.MerchantLegalProfile do 
  use Marketplace.Web, :model

  schema "merchant_legal_profiles" do
    belongs_to :merchant, Marketplace.Merchant
    belongs_to :legal_profile, Marketplace.LegalProfile
  end

  @required_params ~w(merchant_id legal_profile_id)
  @optional_params ~w()

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_params, @optional_params)
  end
end
