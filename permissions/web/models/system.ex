defmodule Permissions.System do
  use Permissions.Web, :model

  schema "systems" do
    field :name, :string
    field :description, :string
  end

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(name), ~w(description))
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(name), ~w(description))
  end

end

