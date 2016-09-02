defmodule Permissions.ScopeDomain do
  use Permissions.Web, :model

  schema "scope_domains" do
    field :domain, :text

    belongs_to :scope, Permissions.Scope
  end
end
