'use strict';

import React from 'react';
import Title from './title';

export default class SectionTitle extends React.Component {
  static propTypes = {
    title: React.PropTypes.node,
    count: React.PropTypes.number,
    buttonClickHandler: React.PropTypes.func,
    children: React.PropTypes.array
  };

  get buttonMarkup() {
    return (
      <div className="fc-col-md-2-6 fc-push-md-2-6 fc-section-title-actions">
        {this.props.buttonClickHandler && (
          <button className="fc-btn fc-btn-primary" onClick={this.props.buttonClickHandler.bind(this)}>
            <i className="icon-add"></i> {this.props.title}
          </button>
         )}
          {this.props.children}
      </div>
    );
  }

  render() {
    return (
      <div className="fc-grid fc-section-title">
        <div className="fc-col-md-2-6">
          <Title title={ this.props.title } subtitle={ this.props.count } />
        </div>
        { this.buttonMarkup }
      </div>
    );
  }
}
