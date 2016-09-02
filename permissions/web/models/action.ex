defmodule Permissions.Action do
  use Permissions.Web, :model

  schema "actions" do
    field :name, :string
    
    belongs_to :resource, Permissions.Resource
  end
end
