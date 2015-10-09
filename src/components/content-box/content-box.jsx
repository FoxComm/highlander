'use strict';

import React, { PropTypes } from 'react';

export default class ContentBox extends React.Component {

  static propTypes = {
    title: PropTypes.string,
    className: PropTypes.string,
    actionBlock: PropTypes.node,
    children: PropTypes.node
  }

  get rootClassName() {
    return `${this.props.className} fc-content-box`;
  }

  render() {
    return (
      <div className={ this.rootClassName }>
        <header>
          <div className="fc-grid">
            <div className="fc-col-2-3 fc-title">{ this.props.title }</div>
            <div className="fc-col-1-3 fc-controls">
              { this.props.actionBlock }
            </div>
          </div>
        </header>
        <article>
          { this.props.children }
        </article>
      </div>
    );
  }
}
