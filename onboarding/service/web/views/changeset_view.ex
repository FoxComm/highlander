defmodule OnboardingService.ChangesetView do
  use OnboardingService.Web, :view

  def translate_errors(changeset) do 
    Enum.map(changeset.errors, fn {key, {err, x}} -> %{key => err} end)
    |> Enum.reduce(fn(m, acc) -> Map.merge(m, acc) end)
  end
  
  def render("errors.json", %{changeset: changeset}) do 
    %{errors: translate_errors(changeset)}
  end
end
