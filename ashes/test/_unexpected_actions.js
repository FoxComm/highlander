import _ from 'lodash';
import { applyMiddleware } from 'redux';
import thunk from 'redux-thunk';

const middlewares = [thunk];

export const name = 'unexpected-redux-actions';

export function installInto(expect) {
  expect.addAssertion(
    '<function|object> to dispatch actions <array> <function|object?>',
    (expect, subject, expectedActions, initialState) => {
      expect.subjectOutput = function(output) {
        output.text('<defined action>');
      };

      expect.argsOutput = function(output) {
        output.text('[', 'white', 'bold').nl();

        expectedActions.forEach(action => {
          output
            .appendInspected({
              ...(action.type ? action : null),
              type: (action || action.type).toString(),
            })
            .text(',')
            .nl();
        });

        output.text(']', 'white', 'bold');
      };

      let expectedActionsForConsume = [...expectedActions];

      return expect.promise((resolve, reject) => {
        let expectationsFullfilled = false;

        const doneOnce = err => {
          if (expectationsFullfilled) return;

          if (err) {
            reject(err);
          } else {
            resolve();
          }

          expectationsFullfilled = true;
        };

        const mockStoreWithoutMiddleware = () => {
          return {
            getState() {
              return _.isFunction(initialState) ? initialState() : initialState;
            },

            dispatch(action) {
              if (expectationsFullfilled) return action;

              const expectedAction = expectedActionsForConsume.shift();

              try {
                if (!expectedAction) {
                  return expect.fail(`Unexpected ${action.type} action`);
                }

                if (_.isString(expectedAction)) {
                  expect(action.type, 'to equal', expectedAction);
                } else if (_.isFunction(expectedAction)) {
                  expect(action.type, 'to equal', expectedAction.toString());
                } else if (_.isPlainObject(expectedAction)) {
                  expect(action, 'to satisfy', {
                    ...expectedAction,
                    type: expectedAction.type.toString(),
                  });
                } else {
                  throw new Error(`wrong expected action type: ${expectedAction}`);
                }
                if (!expectedActionsForConsume.length) {
                  doneOnce();
                }
                return action;
              } catch (e) {
                doneOnce(e);
              }
            },
          };
        };

        const store = applyMiddleware(...middlewares)(mockStoreWithoutMiddleware)();

        store.dispatch(subject);
      });
    }
  );
}
