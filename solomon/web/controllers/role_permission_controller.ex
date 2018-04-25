defmodule Solomon.RolePermissionController do
  use Solomon.Web, :controller
  alias Solomon.Repo
  alias Solomon.RolePermission
  alias Solomon.Role
  alias Solomon.PermissionClaimService

  def index(conn, %{"role_id" => role_id}) do
    role_permissions =
      Repo.all(role_permissions(role_id))
      |> Repo.preload(:permission)

    render(conn, "index.json", role_permissions: role_permissions)
  end

  def create(conn, %{"granted_permission" => role_permission_params, "role_id" => role_id}) do
    changeset =
      RolePermission.changeset(
        %RolePermission{role_id: String.to_integer(role_id)},
        role_permission_params
      )

    case Repo.insert(changeset) do
      {:ok, role_permission} ->
        conn
        |> put_status(:created)
        |> put_resp_header(
          "location",
          role_role_permission_path(conn, :show, role_id, role_permission)
        )
        |> render("show.json", role_permission: role_permission, role_id: role_id)

      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    role_permission =
      Repo.get!(RolePermission, id)
      |> Repo.preload(:permission)

    render(conn, "show.json", role_permission: role_permission)
  end

  def delete(conn, %{"id" => id}) do
    role_permission = Repo.get!(RolePermission, id)
    Repo.delete!(role_permission)

    conn
    |> put_status(:ok)
    |> render("deleted.json")
  end

  defp role_permissions(role_id) do
    role = Repo.get!(Role, role_id)
    assoc(role, :role_permissions)
  end
end
