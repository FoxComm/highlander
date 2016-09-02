defmodule Permissions.Resource do
  use Permissions.Web, :model

  schema "resources" do 
    field :name, :text
    field :frn, :text #Fox Resource Name
    field :description, :text
  end

end
