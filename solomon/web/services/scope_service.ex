defmodule Solomon.ScopeService do
  alias Ecto.Changeset
  alias Solomon.Repo
  alias Solomon.Resource
  alias Solomon.Scope
  alias Solomon.RolePermission
  alias Solomon.PermissionClaimService

  def create_role_with_permissions(role_cs, resources) do
    scope_id = Changeset.get_change(role_cs, :scope_id)
    case Repo.insert(role_cs) do
      {:ok, role} ->
        case insert_permissions(scope_id, resources) do
          {:error, changeset} ->
            {:error, changeset}
          permissions ->
            permissions
            |> Enum.each(fn permission -> insert_role_permission(role.id, permission.id) end)
            {:ok, role}
        end
      {:error, changeset} -> {:error, changeset}
    end
  end

  defp insert_permissions(scope_id, resources) do
    permissions = Repo.all(Resource)
    |> Enum.filter(fn resource -> Enum.any?(resources, fn s -> s == resource.name end) end)
    |> Enum.map(
      fn resource -> PermissionClaimService.insert_permission(
        %{resource_id: resource.id, scope_id: scope_id, actions: resource.actions})
      end)
    |> handle_permission_error
    case permissions do
      {:error, changeset} ->
        {:error, changeset}
      permissions ->
        permissions
        |> Enum.map(fn txn ->
          case Repo.transaction(txn) do
            {:ok, %{permission: permission}} -> permission
            # This should be handled
            {:error, _, _, _} -> nil
          end
        end)
    end
  end

  defp handle_permission_error(permissions) do
    case Enum.find(permissions, fn x -> is_tuple(x) end) do
      nil ->
        permissions
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
