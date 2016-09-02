defmodule Permissions.Role do
  use Permissions.Web, :model

  schema "roles" do
    field :name, :string

    belongs_to :scope, Permissions.Scope
  end

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(name scope_id), ~w())
  end

end
