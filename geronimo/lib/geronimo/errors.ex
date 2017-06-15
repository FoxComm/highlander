defmodule NotAllowedError do
  defexception [message: "Can not verify JWT header"]
end

defmodule NotFoundError do
  defexception [message: "Not found"]
end

defmodule ForbiddenError do
  defexception [message: "You can not access this resource"]
end
