defmodule Permission.Claim do 
  use Permissions.Web, :model

  schema "claims" do
    field :frn, :text #Fox Resource Name
  end
end
