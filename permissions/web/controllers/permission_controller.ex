defmodule Permissions.PermissionController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.Permission
  alias Permissions.PermissionClaimService

  def index(conn, _params) do 
    permissions = Repo.all(Permission)
    render(conn, "index.json", permissions: permissions)
  end

  def create(conn, %{"permission" => permission_params}) do
    case Repo.transaction(PermissionClaimService.insert_permission(permission_params)) do
      {:ok, %{permission: permission}} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", permission_path(conn, :show, permission))
        |> render("permission.json", permission: permission)
      {:error, failed_operation, failed_value, changes_completed} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"id" => id}) do
    permission = 
      Repo.get!(Permission, id)
      |> Repo.preload([:resource, :scope])
    render(conn, "full_permission.json", permission: permission)
  end

  def update(conn, %{"id" => id, "permission" => permission_params}) do
    permission = Repo.get!(Permission, id)
    changeset = Permission.update_changeset(permission, permission_params)
    case Repo.update(changeset) do
      {:ok, permission} -> 
        conn
        |> render("permission.json", permission: permission)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end 

  def delete(conn, %{"id" => id}) do 
    permission = Repo.get!(Permission, id)
    Repo.delete!(permission)

    conn
    |> put_status(:ok)
    |> render("deleted.json")
  end
end

