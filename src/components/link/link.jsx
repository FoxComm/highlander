'use strict';

import React, { PropTypes } from 'react';
import { Link as ReactRouterLink } from 'react-router';

import {interpolateRoute} from '../../route-helpers';

export default class Link extends React.Component {
  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  static propTypes = {
    to: PropTypes.string.isRequired,
    params: PropTypes.object,
    children: PropTypes.node
  };

  render() {
    let {to, params, ...otherProps} = this.props;
    let path = interpolateRoute(this.context.history, to, params);

    return (
      <ReactRouterLink activeClassName="is-active" {...otherProps} to={path} >
        {this.props.children}
      </ReactRouterLink>
    );
  }
}