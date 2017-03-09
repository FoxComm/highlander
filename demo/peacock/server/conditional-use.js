
function test(middleware, fn) {
  return function *(next) {
    if (fn(this)) {
      yield middleware.call(this, next);
    } else {
      yield next;
    }
  };
}

module.exports = test;
