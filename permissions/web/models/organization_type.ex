defmodule Permissions.OrganizationType do
  use Permissions.Web, :model

  schema "organization_types" do
    field :name, :string

  end
end
