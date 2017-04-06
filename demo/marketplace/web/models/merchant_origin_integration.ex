defmodule Marketplace.MerchantOriginIntegration do
  use Marketplace.Web, :model

  schema "merchant_origin_integrations" do
    belongs_to :merchant, Marketplace.Merchant
    belongs_to :origin_integration, Marketplace.OriginIntegration

    timestamps()
  end

  @required_fields ~w(merchant_id origin_integration_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
  end
end