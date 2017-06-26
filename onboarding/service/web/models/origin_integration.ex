defmodule OnboardingService.OriginIntegration do
  use OnboardingService.Web, :model

  schema "origin_integrations" do
    field :shopify_key, :string
    field :shopify_password, :string
    field :shopify_domain, :string

    timestamps()
  end

  @required_fields ~w()a
  @optional_fields ~w(shopify_key shopify_password shopify_domain)a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_uri(:shopify_domain)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required_code(@required_fields)
    |> validate_uri(:shopify_domain)
  end
end
