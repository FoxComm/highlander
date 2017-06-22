{:ok, _} = Application.ensure_all_started(:ex_machina)

Geronimo.Repo.start_link()

Ecto.Adapters.SQL.Sandbox.mode(Geronimo.Repo, {:shared, self()})

ESpec.configure fn(config) ->
  config.before fn(_tags) ->
    Geronimo.Repo.delete_all(Geronimo.ContentType)
    Geronimo.Repo.delete_all(Geronimo.Entity)
  end

  config.finally fn(_shared) ->
    :ok
  end
end
