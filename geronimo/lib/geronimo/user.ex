defmodule Geronimo.User do
  defstruct aud: nil,
            claims: nil,
            email: nil,
            exp: nil,
            id: nil,
            iss: nil,
            name: nil,
            ratchet: nil,
            roles: nil,
            scope: nil
end
