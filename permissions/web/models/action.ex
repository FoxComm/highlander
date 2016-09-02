defmodule Permissions.Action do
  use Permissions.Web, :model

  schema "actions" do
    field :name, :text
    
    belongs_to :resource, Permissions.Resource
  end
end
