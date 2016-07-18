import React, { PropTypes } from 'react';
import { Link as ReactRouterLink } from 'react-router';

export default class Link extends React.Component {
  static propTypes = {
    to: PropTypes.string.isRequired,
    params: PropTypes.object,
    children: PropTypes.node
  };

  render() {
    let {to, params, children, ...otherProps} = this.props;
    let location = {
      name: to,
      params,
    };

    return (
      <ReactRouterLink activeClassName="is-active" {...otherProps} to={location} >
        {children}
      </ReactRouterLink>
    );
  }
}
