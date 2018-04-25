Postgrex.Types.define(
  Geronimo.PostgresTypes,
  [Geronimo.Type.Ltree] ++ Ecto.Adapters.Postgres.extensions(),
  json: Poison
)
