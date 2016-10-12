defmodule Solomon.ScopeView do
  use Solomon.Web, :view
  alias Solomon.ScopeView

  def render("index.json", %{scopes: scopes}) do
    %{scopes: render_many(scopes, ScopeView, "scope.json")}
  end

  def render("show.json", %{scope: scope}) do
    %{scope: render_one(scope, ScopeView, "scope.json")}
  end

  def render("scope.json", %{scope: scope}) do
    %{id: scope.id,
      source: scope.source,
      parent_id: scope.parent_id
    }
  end
end
