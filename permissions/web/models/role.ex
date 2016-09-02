defmodule Permissions.Role do
  use Permissions.Web, :model

  schema "roles" do
    field :name, :string

    belongs_to :scope, Permissions.Scope
  end
end
