defmodule Solomon.PermissionController do
  use Solomon.Web, :controller
  alias Solomon.Repo
  alias Solomon.Permission
  alias Solomon.PermissionClaimService
  alias Solomon.ScopeService

  def index(conn, _params) do
    permissions = ScopeService.scoped_index(conn, Permission)
    render(conn, "index.json", permissions: permissions)
  end

  def create(conn, %{"permission" => permission_params}) do
    scope_id = Map.get(permission_params, "scope_id")
    changeset = Permission.changeset(%Permission{}, permission_params)
                |> ScopeService.validate_scoped_changeset(conn, scope_id)

    case Repo.transaction(PermissionClaimService.create_and_insert_claim_changeset(changeset)) do
      {:ok, %{permission: permission}} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", permission_path(conn, :show, permission))
        |> render("show.json", permission: permission)
      {:error, failed_operation, failed_value, changes_completed} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"id" => id}) do
    permission = ScopeService.scoped_show(conn, Permission, id)
    case permission do
      {:error, changeset} ->
        conn
        |> put_status(:unauthorized)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
      _ ->
        permission_full =
          permission
          |> Repo.preload([:resource, :scope])
        conn
        |> render("show_full.json", permission: permission_full)
    end
  end

  def update(conn, %{"id" => id, "permission" => permission_params}) do
    permission = Repo.get!(Permission, id)
    scope_id = Map.get(permission_params, "scope_id", permission.scope_id)
    changeset = Permission.update_changeset(permission, permission_params)
                |> ScopeService.validate_scoped_changeset(conn, permission.scope_id)
                |> ScopeService.validate_scoped_changeset(conn, scope_id)
    case Repo.update(changeset) do
      {:ok, permission} ->
        conn
        |> render("show.json", permission: permission)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
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

