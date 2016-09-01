defmodule Permissions.OrganizationDomain do
  use Permissions.web, :model

  schema "organization_domains" do
    field :domain, :text

    belongs_to :organization, Permissions.Organization
  end
end
