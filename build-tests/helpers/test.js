import test from 'ava';

export default (name, cb, ...args) => test(name, async (t, ...cbArgs) => {
  try {
    await cb(t, ...cbArgs);
  } catch (error) {
    if (error && error.responseJson && error.responseJson.errors) {
      t.fail(error.responseJson.errors.join('; '));
    } else {
      throw error;
    }
  }
}, ...args);
