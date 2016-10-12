defmodule Permissions.SystemView do
  use Permissions.Web, :view
  alias Permissions.SystemView

  def render("index.json", %{systems: systems}) do
    %{systems: render_many(systems, SystemView, "system.json")}
  end

  def render("show.json", %{system: system}) do
    %{system: render_one(system, SystemView, "system.json")}
  end

  def render("system.json", %{system: system}) do
    %{id: system.id,
      name: system.name,
      description: system.description
    }
  end
end
