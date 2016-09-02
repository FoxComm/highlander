defmodule Permissions.RoleArchetype do
  use Permissions.Web, :model

  schema "role_archetypes" do
    field :name, :text

    belongs_to :scope, Permissions.Scope
  end
end
