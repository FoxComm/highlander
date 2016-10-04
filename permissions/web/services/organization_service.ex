defmodule Permissions.OrganizationService do
  alias Ecto.Changeset
  alias Permissions.Repo
  alias Permissions.Resource
  alias Permissions.RolePermission
  alias Permissions.PermissionClaimService

  def create_role_with_permissions(role_cs, resources) do
    scope_id = Changeset.get_change(role_cs, :scope_id)
    case Repo.insert(role_cs) do
      {:ok, role} ->
        insert_permissions(scope_id, resources)
        |> Enum.each(fn permission -> insert_role_permission(role.id, permission.id) end)
        {:ok, role}
      {:error, changeset} -> {:error, changeset}
    end

  end

  defp insert_permissions(scope_id, resources) do
    Repo.all(Resource) 
    |> Enum.filter(fn resource -> Enum.any?(resources, fn s -> s == resource.name end) end) 
    |> Enum.map(
      fn resource -> PermissionClaimService.insert_permission(
        %{resource_id: resource.id, scope_id: scope_id, actions: resource.actions}) 
      end)
    |> Enum.map(fn txn -> 
      case Repo.transaction(txn) do
        {:ok, %{permission: permission}} -> permission
        # This should be handled
        {:error, _, _, _} -> nil
      end
    end)
  end

  defp insert_role_permission(role_id, permission_id) do
    changeset = RolePermission.changeset(%RolePermission{}, %{role_id: role_id, permission_id: permission_id})
    Repo.insert(changeset)
  end
end
