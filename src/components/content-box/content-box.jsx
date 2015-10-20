'use strict';

import React, { PropTypes } from 'react';

export default class ContentBox extends React.Component {

  static propTypes = {
    title: PropTypes.string,
    className: PropTypes.string,
    actionBlock: PropTypes.node,
    children: PropTypes.node,
    isTable: PropTypes.bool,
    footer: PropTypes.node
  }

  get rootClassName() {
    return `${this.props.className ? this.props.className : null} fc-content-box`;
  }

  render() {
    return (
      <div className={ this.rootClassName }>
        <header className="fc-content-box-header">
          <div className="fc-grid">
            <div className="fc-col-md-2-3 fc-title">{ this.props.title }</div>
            <div className="fc-col-md-1-3 fc-controls">
              { this.props.actionBlock }
            </div>
          </div>
        </header>
        <article className={this.props.isTable ? null : 'fc-content-box-content'}>
          { this.props.children }
        </article>
        {this.props.footer}
      </div>
    );
  }
}
