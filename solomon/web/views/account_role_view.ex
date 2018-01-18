defmodule Solomon.AccountRoleView do
  use Solomon.Web, :view
  alias Solomon.AccountRoleView
  alias Solomon.PermissionView

  # Throughout this controller, we present the id of the granted_role as the AccountRole ID
  def render("index.json", %{account_roles: account_roles}) do
    %{granted_roles: render_many(account_roles, AccountRoleView, "account_role.json")}
  end

  def render("show.json", %{account_role: account_role}) do
    %{granted_role: render_one(account_role, AccountRoleView, "account_role.json")}
  end

  # We add 'grant_' in front of id to help the API consumer know that this is explicitly
  # the id for the mapping, and not the role itself.
  def render("account_role.json", %{account_role: account_role}) do
    %{grant_id: account_role.id, role_id: account_role.role_id}
  end

  def render("deleted.json", _) do
    %{deleted: "success"}
  end
end
