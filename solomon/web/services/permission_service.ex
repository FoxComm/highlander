defmodule Solomon.PermissionClaimService do
  import Ecto
  import Ecto.Query
  alias Ecto.Changeset
  alias Ecto.Multi
  alias Ecto.Query
  alias Solomon.Permission
  alias Solomon.Repo
  alias Solomon.Resource
  alias Solomon.ScopeService
  alias Solomon.System

  def insert_permission(params) do
    perm_changeset = Permission.changeset(%Permission{}, params)
    |> create_claim_changeset
    #Multi.new
    #|> Multi.insert(:permission, perm_changeset)
    #|> Multi.run(:claim, fn %{permission: permission} -> create_claim_changeset(permission) end)
    
  end

  defp create_claim_changeset(perm_changeset) do
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
