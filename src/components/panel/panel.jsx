'use strict';

import React, { PropTypes } from 'react';

export default class Panel extends React.Component {
  static propTypes = {
    children: PropTypes.any,
    title: PropTypes.string,
    className: PropTypes.string,
    controls: PropTypes.any,
    content: PropTypes.any
  };

  get rootClassName() {
    return `${this.props.className} fc-panel`;
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
        <div className="fc-panel-content">
          {this.props.content && this.props.content.props.children}
          {this.props.children}
        </div>
      </div>
    );
  }
}
