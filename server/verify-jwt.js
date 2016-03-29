
import _ from 'lodash';
import fs from 'fs';
import jsonwebtoken from 'jsonwebtoken';

const getPublicKey = _.memoize(() => {
  try {
    return fs.readFileSync(process.env.PHOENIX_PUBLIC_KEY);
  } catch (err) {
    throw new Error(`Can't load public key at path PHOENIX_PUBLIC_KEY=${process.env.PHOENIX_PUBLIC_KEY}, exit`);
  }
});

export default function *verifyJwt(next) {
  const jwt = this.cookies.get('JWT');
  const publicKey = getPublicKey();

  let decodedToken;

  if (jwt) {
    try {
      decodedToken = jsonwebtoken.verify(jwt, publicKey, {
        issuer: 'FC',
        audience: 'customer',
        algorithms: ['RS256', 'RS384', 'RS512'],
      });
    } catch (err) {
      console.error("Can't decode token: ", err);
    }
  }

  if (decodedToken) {
    this.state.auth = {
      jwt,
      user: decodedToken,
    };
  }

  yield next;
}
