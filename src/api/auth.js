
// @namespace FoxApi
// @section Auth methods

// @method signup(user: String, password: String): Promise
// Register new user
export function signup(user, password) {
  return this.post(this.path.signup, {user, password});
}

// @method login(user: String, password: String): Promise
// Authenticate user by username and password, returns promise with jwt token
export function login(user, password) {
  return this.post(this.path.login, {user, password})
}
