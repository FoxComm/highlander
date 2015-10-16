'use strict';

import React, { PropTypes } from 'react';
import _ from 'lodash';

export default class Panel extends React.Component {
  static propTypes = {
    children: PropTypes.any,
    title: PropTypes.string,
    className: PropTypes.string,
    controls: PropTypes.any,
    content: PropTypes.any,
    enablePaddings: PropTypes.bool
  };

  get rootClassName() {
    return `${this.props.className} fc-panel`;
  }

  get contentClassName() {
    let klass = 'fc-panel-content';
    if (this.props.enablePaddings) {
      klass = 'fc-panel-content-list';
    }
    return klass;
  }

  render() {
    return (
      <div className={ this.rootClassName }>
        <div className="fc-panel-header">
          <div className="fc-panel-controls">
            {this.props.controls && this.props.controls.props.children}
          </div>
          <div className='fc-panel-title'>
            <span>{this.props.title}</span>
          </div>
        </div>
        <div className={ this.contentClassName }>
          {this.props.content && this.props.content.props.children}
          {this.props.children}
        </div>
      </div>
    );
  }
}
