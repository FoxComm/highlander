defmodule Solomon.Validation do
  import Ecto.Query
  alias Solomon.Repo

  def email_is_taken(email) do
    results = Repo.all(
      from user in Solomon.User,
      select: user.email
    )
    |> Enum.filter(fn x -> String.downcase(x) == String.downcase(email) end)
    case results do
      [] -> false
      _ -> true
    end
  end

  def email_is_taken(email, id) do
    result = Repo.all(
      from user in Solomon.User,
      where: user.id != ^id,
      select: user.email
    )
    |> Enum.filter(fn x -> String.downcase(x) == String.downcase(email) end)
    case result do
      nil -> false
      _ -> true
    end
  end
end
