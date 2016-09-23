defimpl Plug.Exception, for: Ecto.NotSingleResult do
  def status(_exception), do: 404
end

defimpl Plug.Exception, for: Ecto.NoResultsError do
  def status(_exception), do: 404
end
