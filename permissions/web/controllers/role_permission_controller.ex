defmodule Permissions.RolePermissionController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.RolePermission
  alias Permissions.Role

  def index(conn, %{"role_id" => role_id}) do 
    role_permissions = 
      Repo.all(role_permissions(role_id))
      |> Repo.preload(:permission)
    render(conn, "index.json", role_permissions: role_permissions)
  end

  def create(conn, %{"permission" => permission_params}) do
    changeset = RolePermission.changeset(%RolePermission{}, permission_params)

    case Repo.insert(changeset) do
      {:ok, permission} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", permission_path(conn, :show, permission))
        |> render("permission.json", permission: permission)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(RolePermissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    role_permission = 
      Repo.get!(RolePermission, id) 
      |> Repo.preload(:permission)
      |> Repo.preload(:permission, [:resource, :action, :scope])
    render(conn, "role_permission.json", role_permission: role_permission)
  end

  def update(conn, %{"id" => id, "permission" => permission_params}) do
    permission = Repo.get!(RolePermission, id)
    changeset = RolePermission.update_changeset(permission, permission_params)
    case Repo.update(changeset) do
      {:ok, permission} -> 
        conn
        |> render("show.json", permission: permission)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(RolePermissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end 

  defp role_permissions(role_id) do
    role = Repo.get!(Role, role_id)
    assoc(role, :role_permissions)
  end
end

