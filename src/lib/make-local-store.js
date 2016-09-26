
import React, { Component } from 'react';
import { baseMiddlewares } from '../store';
import { applyMiddleware, createStore } from 'redux';
import reduceReducers from 'reduce-reducers';
import { reducer as asyncReducer } from 'modules/async-utils';

function getDisplayName(WrappedComponent) {
  return WrappedComponent.displayName || WrappedComponent.name || 'Component';
}

// creates local store for given component
export default function makeLocalStore(reducer, initialState = {}) {
  return WrappedComponent => {
    const finalReducer = reduceReducers(reducer, asyncReducer);
    class LocalStore extends Component {
      constructor(...args) {
        super(...args);
        this.store = createStore(finalReducer, initialState, applyMiddleware(...baseMiddlewares));
      }

      render() {
        return (
          <WrappedComponent
            {...this.props}
            store={this.store}
          />
        );
      }
    }

    LocalStore.displayName = `LocalStore(${getDisplayName(WrappedComponent)})`;

    return LocalStore;
  };
}

