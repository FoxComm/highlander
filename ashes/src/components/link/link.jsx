/* @flow */

// libs
import React, { Component, Element } from 'react';
import { Link as ReactRouterLink } from 'react-router';
import { isPermitted } from 'lib/claims';
import type { Claims } from 'lib/claims';

export type LinkProps = {
  actualClaims: Claims,
  children?: Element<*>|Array<Element<*>>,
  expectedClaims: Claims,
  params: Object,
  to: string,
  activeClassName?: string,
};

export default class Link extends Component {
  props: LinkProps;

  static defaultProps = {
    actualClaims: {},
    expectedClaims: {},
  };

  render() {
    const { to, params, children, actualClaims, expectedClaims, activeClassName = '', ...otherProps } = this.props;
    const location = {
      name: to,
      params,
    };

    if (isPermitted(expectedClaims, actualClaims)) {
      return (
        <ReactRouterLink activeClassName={activeClassName} {...otherProps} to={location}>
          {children}
        </ReactRouterLink>
      );
    }

    return null;
  }
}
