// Need to decide what we move from
// https://github.com/FoxComm/firebird/blob/master/src/node_modules/modules/auth.js
// Definitely API calls & URIs, but also JWT storage?

export function signup(credentials) {
  return this.post('/public/registrations/new', credentials);
}

export function login(credentials) {
  return this.post('/public/login', credentials)
}
