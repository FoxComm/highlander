defmodule Permissions.Resource do
  use Permissions.Web, :model

  schema "resources" do 
    field :name, :string
    field :frn, :string #Fox Resource Name
    field :description, :string
  end

end
