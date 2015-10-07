'use strict';

import React, { PropTypes } from 'react';

export default class ContentBox extends React.Component {

  static propTypes = {
    title: PropTypes.string,
    className: PropTypes.string,
    children: PropTypes.node
  }

  render() {
    let rootClassName = `${this.props.className} fc-content-box`;
    return (
      <div className={ rootClassName }>
        <header>{ this.props.title }</header>
        <article>
          { this.props.children }
        </article>
      </div>
    );
  }
}
