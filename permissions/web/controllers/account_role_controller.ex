defmodule Permissions.AccountRoleController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.AccountRole
  alias Permissions.Account

  def index(conn, %{"account_id" => account_id}) do 
    account_roles = 
      Repo.all(account_roles(account_id))
      |> Repo.preload(:permission)
    render(conn, "index.json", account_roles: account_roles)
  end

  def create(conn, %{"granted_permission" => account_role_params, "account_id" => account_id}) do
    changeset = AccountRole.changeset(%AccountRole{account_id: String.to_integer(account_id)}, account_role_params)

    case Repo.insert(changeset) do
      {:ok, account_role} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", account_account_role_path(conn, :show, account_id, account_role))
        |> render("account_role.json", account_role: account_role)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    account_role = 
      Repo.get!(AccountRole, id) 
      |> Repo.preload(:permission)
    render(conn, "account_role.json", account_role: account_role)
  end
  
  def delete(conn, %{"id" => id}) do
    account_role = Repo.get!(AccountRole, id)
    Repo.delete!(account_role)
    
    conn
    |> put_status(:ok)
    |> render("deleted.json")
  end

  defp account_roles(account_id) do
    account = Repo.get!(Account, account_id)
    assoc(account, :account_roles)
  end
end

