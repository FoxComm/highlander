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
      } else {
        let errorMessageLines = [error.message];
        if (error.responseJson && error.responseJson.errors) {
          errorMessageLines = errorMessageLines.concat(error.responseJson.errors);
        }
        if (t.apiLog) {
          errorMessageLines = errorMessageLines.concat(
            t.apiLog.map(entry =>
              `${entry.apiCategoryName}.${entry.methodName}(${entry.args.map(JSON.stringify)}) -> ` +
              `${JSON.stringify(entry.result)}`),
          );
        }
        throw new Error(errorMessageLines.join('\n\n;;;\n\n'));
      }
    }
  }
}, ...args);
