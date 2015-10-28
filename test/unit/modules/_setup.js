
global.localStorage = require('localStorage');

import _ from 'lodash';
import { applyMiddleware } from 'redux';
import thunk from 'redux-thunk';
import path from 'path';

import { ID as actionIdProp } from 'redux-act/lib/constants';

const middlewares = [thunk];

function mockStore(getState, expectedActions, done) {
  if (!Array.isArray(expectedActions)) {
    throw new Error('expectedActions should be an array of expected actions.');
  }
  if (typeof done !== 'undefined' && typeof done !== 'function') {
    throw new Error('done should either be undefined or function.');
  }

  function mockStoreWithoutMiddleware() {
    return {
      getState() {
        return typeof getState === 'function' ?
          getState() :
          getState;
      },

      dispatch(action) {
        const expectedAction = expectedActions.shift();

        try {
          if (_.isString(expectedAction)) {
            expect(action.type).to.equal(expectedAction);
          } else if (_.isFunction(expectedAction)) {
            expect(action[actionIdProp]).to.equal(expectedAction.toString());
          } else if (_.isPlainObject(expectedAction)) {
            expect(action, 'to have properties', {
                ...expectedAction,
                type: expectedAction.type.toString()
            });
          } else {
            throw new Error(`wrong expected action type: ${expectedAction}`);
          }
          if (done && !expectedActions.length) {
            done();
          }
          return action;
        } catch (e) {
          done(e);
        }
      }
    };
  }

  const mockStoreWithMiddleware = applyMiddleware(
    ...middlewares
  )(mockStoreWithoutMiddleware);

  return mockStoreWithMiddleware();
}

global.mockStore = mockStore;

global.requireModule = function(modulePath) {
  return require(path.resolve('src/modules/' + modulePath));
};
