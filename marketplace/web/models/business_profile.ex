defmodule Marketplace.BusinessProfile do 
  use Marketplace.Web, :model

  schema "business_profiles" do
    field :monthly_sales_volume, :string
    field :target_audience, :string
    field :categories, {:array, :string}

    timestamps
  end

  @required_fields ~w()a
  @optional_fields ~w(monthly_sales_volume target_audience categories)a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)

  end

end
