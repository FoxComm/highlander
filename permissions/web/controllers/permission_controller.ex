defmodule Permissions.PermissionController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.Permission

  def index(conn, _params) do 
    permissions = Repo.all(Permission)
    render(conn, "index.json", permissions: permissions)
  end

  def create(conn, %{"permission" => permission_params}) do
    changeset = Permission.changeset(%Permission{}, permission_params)

    case Repo.insert(changeset) do
      {:ok, permission} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", permission_path(conn, :show, permission))
        |> render("permission.json", permission: permission)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    permission = 
      Repo.get!(Permission, id)
      |> Repo.preload([:resource, :action, :scope])
    render(conn, "full_permission.json", permission: permission)
  end

  def update(conn, %{"id" => id, "permission" => permission_params}) do
    permission = Repo.get!(Permission, id)
    changeset = Permission.update_changeset(permission, permission_params)
    case Repo.update(changeset) do
      {:ok, permission} -> 
        conn
        |> render("show.json", permission: permission)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end 

end

