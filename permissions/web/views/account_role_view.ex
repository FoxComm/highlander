defmodule Permissions.AccountRoleView do
  use Permissions.Web, :view
  alias Permissions.AccountRoleView
  alias Permissions.PermissionView

  #Throughout this controller, we present the id of the granted_role as the AccountRole ID
  def render("index.json", %{account_roles: account_roles}) do
    %{granted_roles: render_many(account_roles, AccountRoleView, "account_role.json")}
  end

  def render("show.json", %{account_role: account_role, account_id: account_id}) do
    %{granted_role: render_one(account_role, PermissionView, "account_role.json")}
  end

  def render("account_role.json", %{account_role: account_role}) do
    %{id: account_role.id,
      role_id: account_role.role_id
    }
  end

  def render("deleted.json", _) do
    %{deleted: "success"}
  end
end
