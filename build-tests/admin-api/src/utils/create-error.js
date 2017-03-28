export default function createError(err) {
  const error = new Error(err.message || String(err));
  error.response = err.response;
  error.responseJson = err.response ? err.response.body : err;
  return error;
}
