defmodule OnboardingService.ProductsUpload do
  use OnboardingService.Web, :model

  schema "products_uploads" do
    field :file_url, :string

    timestamps
  end

  @required_fields ~w(file_url)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_uri(:file_url)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_uri(:file_url)
  end
end
