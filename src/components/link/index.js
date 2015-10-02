
import React from 'react';
import { Link as ReactRouterLink } from 'react-router';

import {interpolateRoute} from '../../route-helpers';

export class Link extends React.Component {
  static contextTypes = {
    history: React.PropTypes.object.isRequired
  };

  static propTypes = {
    to: React.PropTypes.string.isRequired,
    params: React.PropTypes.object
  };

  render() {
    let {to, params, ...otherProps} = this.props;
    let path = interpolateRoute(this.context.history, to, params);

    return (
      <ReactRouterLink activeClassName="active" {...otherProps} to={path} >
        {this.props.children}
      </ReactRouterLink>
    );
  }
}
