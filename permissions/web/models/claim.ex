defmodule Permission.Claim do 
  use Permissions.Web, :model

  schema "claims" do
    field :frn, :string #Fox Resource Name
  end
end
