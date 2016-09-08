defmodule Permissions.PermissionClaimService do
  alias Ecto.Multi
  import Ecto
  alias Permissions.Permission
  alias Permissions.Claim

  def insert_permission(params) do
    perm_changeset = Permission.changeset(%Permission{}, params)
    Multi.new
    |> Multi.insert(:permission, perm_changeset)
    |> Multi.run(:claims, %{permission: permission} -> create_claim_changeset(permission) end)
  end

  defp create_claim_changeset(permission) do
    claim_frn = Repo.all(
      from permission in Permission,
      join: resource in assoc(permission, :resource),
      join: action in assoc(permission, :action), #to remove
      join: scope in assoc(permission, :scope),
      where: permission.id == ^permission.id,
      select: %{
        id: permission.id,
        resource_name: resource.name,
        action_name: action.name,
        scope_id: scope.id
      }
    )
    |> claim_frn
    claim_changeset = Claim.changeset_from_frn(%Claim{}, claim_frn)
  end

  defp claim_frn(fp) do
    "Fox/#{fp.scope_id}/#{fp.resource_name}/#{fp.action_name}"
  end
end
