defmodule Permissions.Account do
  use Permissions.Web, :model

  schema "accounts" do
    field :name, :string
    field :ratchet, :integer
  end

  def changeset(model, params \\ :empty) do 
    model 
    |> cast(params, ~w(name), ~w(ratchet)) #Not sure if we should accept ratchet from the endpoint.
  end

  def update_changeset(model, params \\ :empty) do 
    model 
    |> cast(params, ~w(name ratchet), ~w())
  end

end
