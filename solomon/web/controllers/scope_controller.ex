defmodule Solomon.ScopeController do
  use Solomon.Web, :controller
  alias Solomon.Repo
  alias Solomon.Scope
  alias Solomon.Role
  alias Solomon.ScopeService
  alias Solomon.PermissionClaimService

  @admin_resources ~w(cart order product sku album coupon user org)s

  def index(conn, _params) do
    scopes = ScopeService.scoped_index(conn, Scope)
    render(conn, "index.json", scopes: scopes)
  end

  def create(conn, %{"scope" => scope_params}) do
    parent_id = Map.get(scope_params, "parent_id")
    changeset = Scope.changeset(%Scope{}, scope_params)
                |> ScopeService.validate_scoped_changeset(conn, parent_id)

    case Repo.insert(changeset) do
      {:ok, scope} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", fc_scope_path(conn, :show, scope))
        |> render("show.json", scope: scope)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    scope = ScopeService.scoped_show(conn, Scope, id)
    case scope do
      {:error, changeset} ->
        conn
        |> put_status(:unauthorized)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
      _ ->
        render(conn, "show.json", scope: scope)
    end
    scope = Repo.get!(Scope, id)
    render(conn, "show.json", scope: scope)
  end

  def update(conn, %{"id" => id, "scope" => scope_params}) do
    scope = Repo.get!(Scope, id)
    parent_id = Map.get(scope_params, "parent_id", scope.parent_id)
    changeset = Scope.update_changeset(scope, scope_params)
                |> ScopeService.validate_scoped_changeset(conn, scope.parent_id)
                |> ScopeService.validate_scoped_changeset(conn, parent_id)
    case Repo.update(changeset) do
      {:ok, scope} ->
        conn
        |> render("show.json", scope: scope)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def create_admin_role(conn, %{"id" => id}) do
    role_cs = Role.changeset(%Role{}, %{name: "admin", scope_id: id})
    case PermissionClaimService.create_role_with_permissions(role_cs, @admin_resources) do
      {:ok, role} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", role_path(conn, :show, role))
        |> render("show.json", role: role)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end
end
