defmodule Solomon.TokenView do
  use Solomon.Web, :view
  alias Solomon.TokenView

  def render("show.json", %{token: token}) do
    %{
      aud: Map.get(token, "aud"),
      email: Map.get(token, "email"),
      ratchet: Map.get(token, "ratchet"),
      id: Map.get(token, "id"),
      scope: Map.get(token, "scope"),
      roles: Map.get(token, "roles"),
      name: Map.get(token, "name"),
      claims: Map.get(token, "claims"),
      iss: Map.get(token, "iss"),
      exp: Map.get(token, "exp")
    }
  end
end
