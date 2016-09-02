defmodule Permissions.Resource do
  use Permissions.Web, :model

  schema "resources" do 
    field :name, :string
    field :description, :string

    belongs_to :system, Permissions.System
  end

end
