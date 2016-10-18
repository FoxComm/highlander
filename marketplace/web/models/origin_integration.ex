defmodule Marketplace.OriginIntegration do
  use Marketplace.Web, :model

  schema "origin_integrations" do
    field :shopify_key, :string
    field :shopify_password, :string
    field :shopify_domain, :string

    timestamps()
  end

  @doc """
  Builds a changeset based on the `struct` and `params`.
  """
  def changeset(struct, params \\ %{}) do
    struct
    |> cast(params, [:shopify_key, :shopify_password, :shopify_domain])
    |> validate_required([:shopify_key, :shopify_password, :shopify_domain])
  end
end
