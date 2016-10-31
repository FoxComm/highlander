defmodule Marketplace.MerchantShippingSolution do
  use Marketplace.Web, :model

  schema "merchant_shipping_solutions" do
    belongs_to :merchant, Marketplace.Merchant
    belongs_to :shipping_solutions, Marketplace.ShippingSolution, foreign_key: :shipping_solution_id
  end

  @required_fields ~w(merchant_id shipping_solution_id)a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields)
    |> validate_required_code(@required_fields)
  end
end
