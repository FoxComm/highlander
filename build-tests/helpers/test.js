import test from 'ava';
import config from '../config';

const NETWORK_ERRORS = [
  'service unavailable',
  'gateway timeout',
  'bad gateway',
  'unauthorized',
  'too many clients',
  'internal server error',
];

export default (name, cb, ...args) => test(name, async (t, ...cbArgs) => {
  let retryCount = 0;
  for (; ;) {
    try {
      await cb(t, ...cbArgs);
      return;
    } catch (error) {
      let errorMessageLines = error.message ? [error.message] : [];
      if (error.responseJson && error.responseJson.errors) {
        errorMessageLines = errorMessageLines.concat(error.responseJson.errors);
      }
      const lowercaseErrorMessage = errorMessageLines.join(' ').toLowerCase();
      if (
        retryCount < config.networkErrorRetries &&
        NETWORK_ERRORS.find(msg => lowercaseErrorMessage.indexOf(msg) > -1)
      ) {
        retryCount += 1;
        continue;
      } else {
        if (config.fullApiSequenceLogging && t.apiLog) {
          errorMessageLines = errorMessageLines.concat(
            t.apiLog.map(entry =>
              `${entry.apiCategoryName}.${entry.methodName}(${entry.args.map(JSON.stringify)}) -> ` +
              `${JSON.stringify(entry.result)}`),
          );
        }
        error.message = errorMessageLines.join('\n\n');
        throw error;
      }
    }
  }
}, ...args);
