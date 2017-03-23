defmodule NotAllowed do
  defexception [message: "Can not verify JWT header"]
end

defmodule AmazonError do
  defexception [message: "Amazon error occured"]
end
