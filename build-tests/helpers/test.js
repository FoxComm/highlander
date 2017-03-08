import test from 'ava';
import config from '../config';

const NETWORK_ERRORS = [
  'Service Unavailable',
  'Gateway Timeout',
  'Bad Gateway',
  'Unauthorized',
];

export default (name, cb, ...args) => test(name, async (t, ...cbArgs) => {
  let retryCount = 0;
  for (; ;) {
    try {
      await cb(t, ...cbArgs);
      return;
    } catch (error) {
      if (
        retryCount < config.networkErrorRetries &&
        NETWORK_ERRORS.find(msg => error.message.indexOf(msg) > -1)
      ) {
        retryCount += 1;
        continue;
      } else if (error.responseJson && error.responseJson.errors) {
        t.fail(error.responseJson.errors.join('; '));
        return;
      } else {
        throw error;
      }
    }
  }
}, ...args);
