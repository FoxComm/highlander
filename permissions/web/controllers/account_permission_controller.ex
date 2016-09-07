# This is a virtual mapping, that is really a has_many :through 
# twice over.  I assumed that it would be cleanest to have this 
# as a separate controller.  

# We might consider this being somehow integrated into role_permissions 
# controller.

defmodule Permissions.AccountPermissionController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.Role
  alias Permissions.Account
  alias Permissions.Permission

  def index(conn, %{"account_id" => account_id}) do 
    account_permissions = 
      Repo.all(account_roles(account_id))
      |> Enum.flat_map( fn(ar) -> 
        Repo.all(role_permissions(ar.role_id))
        |> Enum.flat_map( fn(rp) -> 
          Repo.get!(Permission, rp.permission_id)
        end)
      end)
                #Repo.get!(Permission, permission_id)
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

