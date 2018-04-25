defmodule Geronimo.Factory do
  use ExMachina.Ecto, repo: Geronimo.Repo
  use Geronimo.EctoFactory
end
