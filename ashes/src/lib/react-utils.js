import _ from 'lodash';
import React from 'react';
import invariant from 'invariant';

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
export function cloneElement(element, { props, handlers, defaultProps }, children) {
  const newProps = {
    ...defaultProps,
    ...element.props,
    ...props,
    ...mergeEventHandlers(element, handlers)
  };

  return React.cloneElement(element, newProps, children);
}

export function addKeys(prefix, elements) {
  return _.reduce(elements, (acc, element, i) => {
    if (element) {
      return [...acc, React.cloneElement(element, { key: `${prefix}-${i}` })];
    }
    return acc;
  }, []);
}

export function getDisplayName(Component) {
  return Component.displayName || Component.name || 'Component';
}

export function getTransitionProps(styles: Object) {
  return function (name: string, timeout: number | Object = 100, appear: boolean = false, component: string = 'div') {
    const appearName = styles[`${name}Appear`];
    const appearActiveName = styles[`${name}AppearActive`];
    const enterName = styles[`${name}Enter`];
    const enterActiveName = styles[`${name}EnterActive`];
    const leaveName = styles[`${name}Leave`];
    const leaveActiveName = styles[`${name}LeaveActive`];

    invariant(!appear || appear && appearName && appearActiveName,
      `You've requested appear transition for "${name}" but provided no styles for it`);

    // trying to get timeouts.appear{enter|leave} if timeout is an object or use timeout itself otherwise
    const appearTimeout = _.get(timeout, 'appear', timeout);
    const enterTimeout = _.get(timeout, 'enter', timeout);
    const leaveTimeout = _.get(timeout, 'leave', timeout);

    return {
      component,
      transitionName: {
        appear: appearName,
        appearActive: appearActiveName,
        enter: enterName,
        enterActive: enterActiveName,
        leave: leaveName,
        leaveActive: leaveActiveName,
      },
      transitionAppear: appear,
      transitionAppearTimeout: appearTimeout,
      transitionEnterTimeout: enterTimeout,
      transitionLeaveTimeout: leaveTimeout,
    };
  };
}
