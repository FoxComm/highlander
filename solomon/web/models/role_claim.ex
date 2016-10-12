defmodule Solomon.RoleClaim do
  use Solomon.Web, :model

  schema "role_claims" do
    belongs_to :claim, Solomon.Claim
    belongs_to :role, Solomon.Role
  end
end
