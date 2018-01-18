defmodule Solomon.ScopeDomain do
  use Solomon.Web, :model

  schema "scope_domains" do
    field(:domain, :string)

    belongs_to(:scope, Solomon.Scope)
  end
end
