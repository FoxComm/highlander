defmodule Hyperion.Factory do
  # use ExMachina
  use ExMachina.Ecto, repo: MyApp.Repo
  use Hyperion.ProductFactory
  use Hyperion.EctoFactory
end