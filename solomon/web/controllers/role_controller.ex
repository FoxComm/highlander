defmodule Solomon.RoleController do
  use Solomon.Web, :controller
  alias Solomon.Repo
  alias Solomon.Role
  alias Solomon.ScopeService

  def index(conn, _params) do
    roles = ScopeService.scoped_index(conn, Role)
    |> Repo.preload(:permissions)
    render(conn, "index.json", roles: roles)
  end

  def create(conn, %{"role" => role_params}) do
    # scope_id = Map.get(role_params, "scope_id")
    changeset = Role.changeset(%Role{}, role_params)
    # |> ScopeService.validate_scoped_changeset(conn, scope_id)

    case Repo.insert(changeset) do
      {:ok, role} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", role_path(conn, :show, role))
        |> render("show.json", role: role)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    role = ScopeService.scoped_show(conn, Role, id)
    case role do
      {:error, changeset} ->
        conn
        |> put_status(:unauthorized)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
      _ ->
        role_with_permissions =
          role
          |> Repo.preload(:permissions)
        conn
        |> render("show_with_permissions.json", role: role_with_permissions)
    end
  end

  def update(conn, %{"id" => id, "role" => role_params}) do
    role = Repo.get!(Role, id)
    scope_id = Map.get(role_params, "scope_id", role.scope_id)
    changeset = Role.update_changeset(role, role_params)
                |> ScopeService.validate_scoped_changeset(conn, role.scope_id)
                |> ScopeService.validate_scoped_changeset(conn, scope_id)
    case Repo.update(changeset) do
      {:ok, role} ->
        conn
        |> render("show.json", role: role)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end

end

