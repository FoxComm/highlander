defmodule Solomon.AccountPermissionView do
  use Solomon.Web, :view
  alias Solomon.AccountPermissionView
  alias Solomon.PermissionView

  #Throughout this controller, we present the id of the granted_role as the AccountPermission ID
  def render("index.json", %{account_permissions: account_permissions}) do
    %{granted_permissions: render_many(account_permissions, AccountPermissionView, "account_permission.json")}
  end

  # We add 'grant_' in front of id to help the API consumer know that this is explicitly
  # the id for the mapping, and not the role itself.
  def render("account_permission.json", %{account_permission: account_permission}) do
    %{permission_id: account_permission.permission_id
    }
  end

end
