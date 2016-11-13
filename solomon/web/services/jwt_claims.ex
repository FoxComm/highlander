defmodule Solomon.JWTClaims do
  import Ecto.Query
  alias Solomon.Repo
  alias Solomon.Account
  alias Solomon.AccountRole
  alias Solomon.Role
  alias Solomon.RolePermission
  alias Solomon.Permission
  alias Solomon.User
  alias Solomon.ScopeService

  def token_claim(account_id, scope_id) do
    account = Repo.get!(Account, account_id)
    {claims, roles} = get_claims_roles(account_id, scope_id)
    user = Repo.get_by!(User, account_id: account_id)
    %{
      "aud" => "user",
      "email" => user.email,
      "ratchet" => account.ratchet,
      "id" => account_id,
      "scope" => ScopeService.get_scope_path_by_id!(scope_id),
      "roles" => roles,
      "name" => user.name,
      "claims" => claims,
      "iss" => "FC",
      # TODO : per access method TTL
      "exp" => exp_from_ttl_days(tokenTTL)
    }
  end

  defp get_claims_roles(account_id, scope_id) do
    Repo.all(
      from ar in AccountRole,
      where: ar.account_id == ^account_id,
      join: r in Role,
      on: ar.role_id == r.id,
      where: r.scope_id == ^scope_id,
      join: rp in RolePermission,
      on: rp.role_id == r.id,
      join: p in Permission,
      on: p.id == rp.permission_id,
      select: {p, r.name}
    )
    |> Enum.unzip
    |> process_perms_and_roles
  end

  defp process_perms_and_roles({permissions, role_names}) do
    {
      Enum.map(permissions, fn p -> %{p.frn => p.actions} end)
      |> Enum.reduce(%{}, fn (x, y) -> Map.merge(x, y) end),
      Enum.uniq(role_names)
    }
  end

  defp exp_from_ttl_days(days) do
    exp_from_ttl_hours(days * 24)
  end
  
  defp exp_from_ttl_hours(hours) do
    exp_from_ttl_mins(hours * 60)
  end
  
  defp exp_from_ttl_mins(mins) do
    exp_from_ttl_secs(mins * 60)
  end

  defp exp_from_ttl_secs(secs) do
    :os.system_time(:seconds) + secs
  end

  def tokenTTL do
    Application.get_env(:solomon, Solomon.JWTClaims)[:tokenTTL]
    |> Integer.parse
    |> fn {int, _} -> int end.()
  end
end
