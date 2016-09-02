defmodule Permissions.Role do
  use Permissions.Web, :model

  schema "roles" do
    field :name, :text

    belongs_to :scope, Permissions.Scope
  end
end
