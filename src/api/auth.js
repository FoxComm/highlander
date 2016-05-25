
export function signup(credentials) {
  return this.post(this.path.signup, credentials);
}

export function login(credentials) {
  return this.post(this.path.login, credentials)
}
