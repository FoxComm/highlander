defmodule Permissions.Organization do
  use Permissions.Web, :model

  schema "organizations" do 
    field :name, :string

    has_one :organization_type, Permission.OrganizationType
    has_one :parent, Permission.Organization
  end
end
