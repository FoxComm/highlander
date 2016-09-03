defmodule Permissions.RoleController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.Role

  def index(conn, _params) do 
    roles = Repo.all(Role)
    render(conn, "index.json", roles: roles)
  end

  def create(conn, %{"role" => role_params}) do
    changeset = Role.changeset(%Role{}, role_params)

    case Repo.insert(changeset) do
      {:ok, role} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", role_path(conn, :show, role))
        |> render("role.json", role: role)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    role = 
      Repo.get!(Role, id)
      |> Repo.preload(:permissions)
    render(conn, "show.json", role: role)
  end

  def update(conn, %{"id" => id, "role" => role_params}) do
    role = Repo.get!(Role, id)
    changeset = Role.update_changeset(role, role_params)
    case Repo.update(changeset) do
      {:ok, role} -> 
        conn
        |> render("show.json", role: role)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end 

end

