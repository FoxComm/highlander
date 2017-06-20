defmodule OnboardingService.ShippingSolution do
  use OnboardingService.Web, :model

  schema "shipping_solutions" do
    field :carrier_name, :string
    field :price, :integer

    timestamps()
  end

  @required_fields ~w(carrier_name price)a

  def changeset(struct, params \\ %{}) do
    struct
    |> cast(params, @required_fields)
    |> validate_required_code(@required_fields)
  end
end
