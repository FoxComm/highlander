defmodule Marketplace.MerchantLegalProfile do 
  use Marketplace.Web, :model
  import Marketplace.Validation

  schema "merchant_legal_profiles" do
    belongs_to :merchant, Marketplace.Merchant
    belongs_to :legal_profile, Marketplace.LegalProfile
  end

  @required_fields ~w(merchant_id legal_profile_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> unique_constraint_code(:merchant_id)
  end
end
