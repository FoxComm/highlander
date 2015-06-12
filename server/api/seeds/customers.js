'use strict';

function randomDate() {
  let
    start = new Date('1978-08-01'),
    end   = new Date();
  return new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));
}

module.exports = function(count) {
  count = count || 1;
  let customers = [];
};
