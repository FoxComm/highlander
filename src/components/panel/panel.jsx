'use strict';

import React, { PropTypes } from 'react';
import _ from 'lodash';

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

  get rootClassName() {
    return `fc-panel ${this.props.className ? this.props.className : ''}`;
  }

  get contentClasses() {
    return `fc-panel-content ${this.props.featured ? 'fc-panel-content-featured' : ''}`;
  }

  render() {
    return (
      <div className={ this.rootClassName }>
        <div className="fc-panel-header">
          {this.props.title}
        </div>
        <div className={this.contentClasses}>
          {this.props.content && this.props.content.props.children}
          {this.props.children}
        </div>
      </div>
    );
  }
}
