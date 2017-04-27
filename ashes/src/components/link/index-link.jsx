/* @flow */
import React, { Component, Element } from 'react';
import Link from './link';

import { isPermitted } from 'lib/claims';
import type { Claims } from 'lib/claims';
import type { LinkProps } from './link';

export default class IndexLink extends Component {
  props: LinkProps;

  static defaultProps = {
    actualClaims: {},
    expectedClaims: {},
  };

  render() {
    const { children, actualClaims, expectedClaims } = this.props;

    if (isPermitted(expectedClaims, actualClaims)) {
      return <Link {...this.props} onlyActiveOnIndex={true}>{children}</Link>;
    }

    return null;
  }
}
