defmodule Permissions.Claim do 
  use Permissions.Web, :model

  schema "claims" do
    field :frn, :string #Fox Resource Name
  end

  def changeset_from_frn(model, params \\ :empty) do
    model
    |> cast(params, ~w(frn), ~w())
  end
end

