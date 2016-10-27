defmodule Marketplace.ShippingSolution do
  use Marketplace.Web, :model

  schema "shipping_solutions" do
    field :carrier_name, :string
    field :price, :string

    timestamps()
  end

  @required_fields ~w(carrier_name price)a

  def changeset(struct, params \\ %{}) do
    struct
    |> cast(params, @required_fields)
    |> validate_required_code(@required_fields)
  end
end
