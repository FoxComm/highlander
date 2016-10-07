/**
 * @miniclass LoginResponse (Auth)
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
 * @miniclass GoogleSigninResponse (Auth)
 * @field url: String
 * Url for redirection.
 */


// @class Auth
// Accessible via [auth](#foxapi-auth) property of [FoxApi](#foxapi) instance.

import * as endpoints from '../endpoints';

export default class Auth {
  constructor(api) {
    this.api = api;
  }

  // @method signup(email: String, name: String, password: String): Promise
  // Register new user
  signup(email, name, password) {
    return this.api.post(endpoints.signup, {email, name, password});
  }

  // @method login(email: String, password: String, org: String): Promise<LoginResponse>
  // Authenticate user by username and password.
  // `org` is the name of the organization you want to log in under
  login(email, password, org) {
    let jwt = null;

    return this.api.post(
      endpoints.login,
      {email, password, org},
      {
        credentials: 'same-origin',
        handleResponse: false
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
        if (user.email) {
          return {
            user,
            jwt,
          };
        }
        throw new Error('Server error, try again later. Sorry for inconvenience :(');
      });
  }

  // @method googleSignin(): Promise<GoogleSigninResponse>
  googleSignin(){
    return this.api.get(endpoints.googleSignin);
  }

  // @method logout(): Promse
  // Removes JWT cookie.
  logout() {
    return this.api.post(endpoints.logout);
  }
}
