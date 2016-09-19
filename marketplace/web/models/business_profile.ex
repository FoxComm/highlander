defmodule Marketplace.BusinessProfile do 
  use Marketplace.Web, :model

  schema "business_profiles" do
    field :monthly_sales_volume, :integer
    field :target_audience, :string
    field :categories, {:array, :string}

    timestamps
  end

  @required_params ~w()
  @optional_params ~w(monthly_sales_volume target_audience categories)

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_params, @optional_params)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_params, @optional_params)
  end

end
