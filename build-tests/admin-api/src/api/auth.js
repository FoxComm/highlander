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

import endpoints from '../endpoints';

export default class Auth {
  constructor(api) {
    this.api = api;
  }

  _processJWT(promise, jwt) {
    const error = new Error();
    Error.captureStackTrace(error, this._processJWT);
    return promise.then(
      response => {
        jwt = response.header.jwt;

        return response.body;
      },
      err => {
        error.message = err.message || String(err);
        error.response = err.response;
        error.responseJson = err.response ? err.response.body : err;
        throw error;
      }
    )
    .then(data => {
      if (data.email) {
        return {
          user: data,
          jwt,
        };
      }

      if (data.errors) {
        const error = new Error(data.errors[0]);
        error.responseJson = data;
        throw error;
      }

      throw new Error('Server error, try again later. Sorry for inconvenience :(');
    });
  }

  // @method signup(email: String, name: String, password: String): Promise
  // Register new user
  signup(email, name, password) {
    let jwt = null;

    const signupPromise = this.api.post(
      endpoints.signup,
      {email, name, password},
      {
        credentials: 'same-origin',
        handleResponse: false
      }
    );

    return this._processJWT(signupPromise, jwt);
  }

  // @method login(email: String, password: String, org: String): Promise<LoginResponse>
  // Authenticate user by username and password.
  // `org` is the name of the organization you want to log in under
  login(email, password, org) {
    let jwt = null;

    const loginPromise = this.api.post(
      endpoints.login,
      {email, password, org},
      {
        credentials: 'same-origin',
        handleResponse: false
      }
    );

    return this._processJWT(loginPromise, jwt);
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

  // @method restorePassword(email: String): Promise
  // requests email instructions to reset password
  restorePassword(email) {
    return this.api.post(endpoints.sendRestPassword, { email });
  }

  // @method restorePassword(code: String, newPassword: String): Promise
  // creates new password
  resetPassword(code, newPassword) {
    return this.api.post(endpoints.resetPassword, { code, newPassword });
  }
}
