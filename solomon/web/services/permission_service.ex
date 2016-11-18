defmodule Solomon.PermissionClaimService do
  import Ecto
  import Ecto.Query
  alias Ecto.Changeset
  alias Ecto.Multi
  alias Ecto.Query
  alias Solomon.Permission
  alias Solomon.Repo
  alias Solomon.Resource
  alias Solomon.RolePermission
  alias Solomon.ScopeService
  alias Solomon.System
  alias Solomon.ScopeService

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
      fn resource -> create_and_insert_claim_changeset(Permission.changeset(
          %Permission{},
          %{resource_id: resource.id, scope_id: scope_id, actions: resource.actions}
        ))
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
      nil -> permissions
      {:error, changeset} -> {:error, changeset}
    end
  end

  defp insert_role_permission(role_id, permission_id) do
    changeset = RolePermission.changeset(%RolePermission{}, %{role_id: role_id, permission_id: permission_id})
    Repo.insert(changeset)
  end

  defp create_and_insert_claim_changeset(perm_changeset) do
    resource_id = Changeset.get_change(perm_changeset, :resource_id)
    scope_id = Changeset.get_change(perm_changeset, :scope_id)
    actions = Changeset.get_change(perm_changeset, :actions)
    claim_frn = Repo.all(
      from resource in Resource,
      join: system in System,
      where: resource.id == ^resource_id,
      where: resource.system_id == system.id,
      select: %{
        resource_name: resource.name,
        system_name: system.name,
        actions: resource.actions
      },
      limit: 1
    )
    |> List.first
    |> Map.merge(%{scope_id: scope_id})
    |> construct_frn
    case claim_frn do
      {:ok, frn} ->
        changeset_with_claim = Permission.changeset(perm_changeset, %{"frn" => frn})
        Multi.new
        |> Multi.insert(:permission, changeset_with_claim)
      :error ->
        {:error, Changeset.add_error(perm_changeset, :resource_id, "failed to create frn")}
    end
  end

  defp construct_frn(fp) do
    case fp do
      fp when is_nil fp -> :error
      fp ->
        scope_path = ScopeService.get_scope_path_by_id!(fp.scope_id)
        {:ok, "frn:#{fp.system_name}:#{fp.resource_name}:#{scope_path}"}
    end
  end
end
