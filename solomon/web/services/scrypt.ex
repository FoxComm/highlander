defmodule Solomon.Scrypt do
  use Bitwise
  import Base

  def scrypt(passwd) do
    salt = :crypto.strong_rand_bytes(16)
    scrypt(passwd, salt, 65536, 8, 1, 32)
  end

  def check(passwd, hash) do
    case String.split(hash, "$") do
      [_, "s0", params, enc_salt, _] ->
        {n, r, p} = unwrap_params(params)
        salt = decode64!(enc_salt)
        {:ok, hash == scrypt(passwd, salt, n, r, p, 32)}
      _ ->
        {:error, %{errors: %{hashed_password: "invalid"}}}
    end
  end

  defp unwrap_params(params) do
    <<ln, r, p>> = decode16!(params)
    n = Kernel.trunc(:math.pow(2, ln))
    {n, r, p}
  end

  defp scrypt(passwd, salt, n, r, p, buflen) do
    params = Integer.to_string(bor(Kernel.trunc(:math.log2(n)) <<< 16, bor(r <<< 8, p)), 16)
    :scrypt.start
    derived = :scrypt.scrypt(passwd, salt, n, r, p, buflen)
    |> encode64
    "$s0$" <> params <> "$" <> encode64(salt) <> "$" <> derived
  end
end
