const unexpected = require('unexpected');
const unexpectedReactShallow = require('unexpected-react-shallow');

const expectJSX = unexpected.clone()
  .installPlugin(unexpectedReactShallow);

// patch expect

global.expect = (function(expect) {
  return function(target) {
    if (arguments.length > 1) {
      return expectJSX.apply(this, arguments);
    } else {
      return expect(target);
    }
  };
})(global.expect);
