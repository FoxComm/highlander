defmodule Permissions.RoleClaim do
  use Permissions.Web, :model

  schema "role_claims" do
    belongs_to :claim, Permissions.Claim
    belongs_to :role, Permissions.Role
  end
end
