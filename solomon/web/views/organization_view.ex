defmodule Solomon.OrganizationView do
  use Solomon.Web, :view
  alias Solomon.OrganizationView

  def render("index.json", %{organizations: organizations}) do
    %{organizations: render_many(organizations, OrganizationView, "organization.json")}
  end

  def render("show.json", %{organization: organization}) do
    %{organization: render_one(organization, OrganizationView, "organization.json")}
  end

  def render("organization.json", %{organization: organization}) do
    %{
      id: organization.id,
      name: organization.name,
      kind: organization.kind,
      parent_id: organization.parent_id,
      scope_id: organization.scope_id
    }
  end
end
