'use strict';

import React, { PropTypes } from 'react';
import classNames from 'classnames';

export default class Panel extends React.Component {
  static propTypes = {
    children: React.PropTypes.any,
    title: React.PropTypes.string,
    content: React.PropTypes.any,
    featured: React.PropTypes.bool
  };

  static defaultProps = {
    featured: false
  };

  render() {
    return (
      <div className={classNames('fc-panel', this.props.className)}>
        <div className="fc-panel-header">
          {this.props.title}
        </div>
        <div className={classNames('fc-panel-content', {'fc-panel-content-featured': this.props.featured})}>
          {this.props.content && this.props.content.props.children}
          {this.props.children}
        </div>
      </div>
    );
  }
}
