/* @flow */

// libs
import React, { Component, Element } from 'react';
import { Link as ReactRouterLink } from 'react-router';
import { isPermitted } from 'lib/claims';

// styles
import s from './link.css';

// types
import type { Claims } from 'lib/claims';

export type Props = {
  /** Route name to link to */
  to: string,
  /** Route params */
  params: Object,
  /** User claims */
  actualClaims: Claims,
  /** Claims required to render link */
  expectedClaims: Claims,
  params?: Object,
  to: string,
  activeClassName?: string,
  /** Children */
  children?: Element<any> | Array<Element<any>>,
};

/**
 * react-router's Link wrapper to support Fox claims
 * Implemented as class to use it in PageNav with refs(not supported for functional components)
 *
 * @function Link
 */
export class Link extends Component {
  props: Props;

  render() {
    const { to, params, children, actualClaims = {}, expectedClaims = {}, ...otherProps } = this.props;
    const location = {
      name: to,
      params,
    };

    if (!isPermitted(expectedClaims, actualClaims)) {
      return null;
    }

    return (
      <ReactRouterLink className={s.link} {...otherProps} to={location}>
        {children}
      </ReactRouterLink>
    );
  }
}

export class IndexLink extends Component {
  props: Props;

  render() {
    return React.createElement(Link, { ...this.props, onlyActiveOnIndex: true });
  }
}
