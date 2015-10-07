'use strict';

import React, { PropTypes } from 'react';

export default class ContentBox extends React.Component {

  static propTypes = {
    title: PropTypes.string,
    className: PropTypes.string,
    actionBlock: PropTypes.node,
    children: PropTypes.node
  }

  render() {
    let rootClassName = `${this.props.className} fc-content-box`;
    return (
      <div className={ rootClassName }>
        <header>
          <div className="fc-content-box-title">{ this.props.title }</div>
          <div className="fc-content-box-actions">
            { this.props.actionBlock }
          </div>
        </header>
        <article>
          { this.props.children }
        </article>
      </div>
    );
  }
}
