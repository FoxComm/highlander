defmodule Solomon.ScopeService do
  import Plug.Conn
  import Ecto.Query
  alias Ecto.Changeset
  alias Solomon.ErrorView
  alias Solomon.Repo
  alias Solomon.Resource
  alias Solomon.Scope
  alias Solomon.RolePermission
  alias Solomon.Scope
  alias Solomon.Permission
  alias Solomon.PermissionClaimService

  def get_scope_path_by_id(scope_id) do
    case Repo.get(Scope, scope_id) do
      nil ->
        {:error, "scope not found"}
      scope ->
        {:ok, get_scope_path(scope)}
    end
  end

  def get_scope_path_by_id!(scope_id) do
    Repo.get!(Scope, scope_id)
    |> get_scope_path
  end

  def get_scope_path(scope) do
    case scope.parent_path do
      nil -> to_string(scope.id)
      "" -> to_string(scope.id)
      parent_path -> parent_path <> "." <> to_string(scope.id)
    end
  end

  def get_request_scope_regex(conn) do
    case get_resp_header(conn, "scope") do
      [] ->
        conn
        |> send_resp(:unauthorized, Poison.encode!(%{
          errors: [
            "unauthorized request"
          ]
        }))
      [scope] ->
        "^" <> Regex.escape(scope)
        |> Regex.compile!
    end
  end

  def scoped_index(conn, Scope) do
    req_scope_path = get_request_scope_regex(conn)
    Repo.all(Scope)
    |> Enum.filter(
      fn scope ->
        Regex.match?(req_scope_path, get_scope_path(scope))
      end
    )
  end

  def scoped_index(conn, schema) do
    req_scope_path = get_request_scope_regex(conn)
    Repo.all(
      from object in schema,
      join: scope in Scope,
      on: object.scope_id == scope.id,
      select: {object, scope}
    )
    |> Enum.filter(
      fn {object, scope} ->
        Regex.match?(req_scope_path, get_scope_path(scope))
      end
    )
    |> Enum.map(fn {object, scope} -> object end)
  end

  def scoped_show(conn, Scope, id) do
    req_scope_path = get_request_scope_regex(conn)
    scope = Repo.get!(Scope, id)
    if Regex.match?(req_scope_path, get_scope_path(scope)) do
      scope
    else {
      :error,
      Ecto.Changeset.add_error(
        Scope.changeset(scope, %{}),
        :scope,
        "insufficient permissions"
      )
    }
    end
  end

  def scoped_show(conn, schema, id) do
    req_scope_path = get_request_scope_regex(conn)
    object = Repo.get!(schema, id)
    if Regex.match?(req_scope_path, get_scope_path_by_id!(object.scope_id)) do
      object
    else {
      :error,
      Ecto.Changeset.add_error(
        schema.changeset(object, %{}),
        :scope,
        "insufficient permissions"
      )
    }
    end
  end

  def validate_scoped_changeset(changeset, conn, scope_id) do
    req_scope_path = get_request_scope_regex(conn)
    if(
      !changeset.valid? || # pass on existing errors
      Regex.match?(req_scope_path, get_scope_path_by_id!(scope_id))
    ) do
      changeset
    else
      Ecto.Changeset.add_error(
        changeset,
        :scope,
        "insufficient permissions"
      )
    end
  end

  defp handle_permission_error(permissions) do
    case Enum.find(permissions, fn x -> is_tuple(x) end) do
      nil -> permissions
      {:error, changeset} -> {:error, changeset}
    end
  end

  defp insert_role_permission(role_id, permission_id) do
    changeset = RolePermission.changeset(%RolePermission{}, %{role_id: role_id, permission_id: permission_id})
    Repo.insert(changeset)
  end

  def get_scope_path_by_id(scope_id) do
    case Repo.get(Scope, scope_id) do
      nil ->
        {:error, "scope not found"}
      scope ->
        {:ok, get_scope_path(scope)}
    end
  end

  def get_scope_path_by_id!(scope_id) do
    Repo.get!(Scope, scope_id)
    |> get_scope_path
  end

  def get_scope_path(scope) do
    if(is_integer(scope.id)) do
      case scope.parent_path do
        nil -> to_string(scope.id)
        "" -> to_string(scope.id)
        parent_path -> parent_path <> "." <> to_string(scope.id)
      end
    else
      {:error, "invalid scope"}
    end
  end

  def super_scope_regex(scope_path) do
    "^" <> Regex.escape(scope_path)
    |> Regex.compile!
  end
end
