defmodule Permissions.Permission do
  use Permissions.Web, :model

  schema "permissions" do 
    belongs_to :resource, Permissions.Resource
    belongs_to :action, Permissions.Action
    belongs_to :scope, Permissions.Scope
  end
end
