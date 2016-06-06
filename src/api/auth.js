/**
 * @miniclass LoginResponse (FoxApi)
 * @field jwt: String
 * [JWT](https://jwt.io/) token.
 *
 * @field user.name: String
 * User name.
 *
 * @field user.email: String
 * User email
 */

/**
 * @miniclass GoogleSigninResponse (FoxApi)
 * @field url: String
 * Url for redirection.
 */


// @namespace FoxApi
// @section Auth methods

import * as endpoints from '../endpoints';

// @method signup(user: String, password: String): Promise
// Register new user
export function signup(user, password) {
  return this.post(endpoints.signup, {user, password});
}

// @method login(user: String, password: String, kind: String): Promise<LoginResponse>
// Authenticate user by username and password.
// `kind` can be 'customer' or 'admin'
export function login(user, password, kind) {
  let jwt = null;

  return this.post(
    endpoints.login,
    {user, password, kind},
    {
      credentials: 'same-origin'
    }
  )
    .then(response => {
      jwt = response.headers.get('jwt');
      if (response.status == 200 && jwt) {
        return response.json();
      }
      throw new Error('Server error, try again later. Sorry for inconvenience :(');
    })
    .then(user => {
      if (user.email && user.name) {
        return {
          user,
          jwt,
        };
      }
      throw new Error('Server error, try again later. Sorry for inconvenience :(');
    });
}

// @method googleSignin(): Promise<GoogleSigninResponse>
export function googleSignin(){
  return this.get(endpoints.googleSignin);
}

// @method logout(): Promse
// Removes JWT cookie.
export function logout() {
  return this.post(endpoints.logout);
}
