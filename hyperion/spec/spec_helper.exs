{:ok, _} = Application.ensure_all_started(:ex_machina)

Hyperion.Repo.start_link()
Ecto.Adapters.SQL.Sandbox.mode(Hyperion.Repo, {:shared, self()})

ESpec.configure fn(config) ->
  config.before fn(_tags) ->
    Hyperion.Repo.delete_all(Category)
  end

  config.finally fn(_shared) ->
    :ok
  end
end


