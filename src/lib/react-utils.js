
import _ from 'lodash';
import React from 'react';

function mergeHandlers(...handlers) {
  return (...args) => {
    handlers.forEach(handler => handler(...args));
  };
}

export function mergeEventHandlers(child, newEventHandlers) {
  return _.transform(newEventHandlers, (result, handler, type) => {
    result[type] = child.props[type] ? mergeHandlers(handler, child.props[type]) : handler;
  });
}

/**
 * Extended version of React.cloneElement.
 * Has ability to merge event handlers and pass defaultProps for cloning element.
 */
export function cloneElement(element, {props, handlers, defaultProps}, children) {
  const newProps = {
    ...defaultProps,
    ...element.props,
    ...props,
    ...mergeEventHandlers(element, handlers)
  };

  return React.cloneElement(element, newProps, children);
}

