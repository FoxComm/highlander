# This is a virtual mapping, that is really a has_many :through 
# twice over.  I assumed that it would be cleanest to have this 
# as a separate controller.  

# We might consider this being somehow integrated into role_permissions 
# controller.

defmodule Solomon.AccountPermissionController do
  use Solomon.Web, :controller
  alias Solomon.Repo
  alias Solomon.Role
  alias Solomon.Account
  alias Solomon.Permission

  def index(conn, %{"account_id" => account_id}) do
    ac_id = String.to_integer(account_id)
    account_permissions = Repo.all(
      from account in Account,
      join: account_roles in assoc(account, :account_roles),
      join: role in assoc(account_roles, :role),
      join: permissions in assoc(role, :permissions),
      where: account.id == ^ac_id,
      select: %{
        permission_id: permissions.id
      },
      distinct: permissions.id)
    render(conn, "index.json", account_permissions: account_permissions)
  end

  defp role_permissions(role_id) do
    role = Repo.get!(Role, role_id)
    assoc(role, :role_permissions)
  end

  defp account_roles(account_id) do 
    account = Repo.get(Account, account_id)
    assoc(account, :account_roles)
  end
end

