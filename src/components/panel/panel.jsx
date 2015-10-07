'use strict';

import React from 'react';
import Wrapper from '../wrapper/wrapper';

export default class Panel extends React.Component {
  render() {
    return (
      <div className="fc-panel">
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

Panel.propTypes = {
  children: React.PropTypes.any,
  title: React.PropTypes.string,
  controls: React.PropTypes.any,
  content: React.PropTypes.any
};
