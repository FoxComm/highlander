'use strict';

import React from 'react';

export default class SectionTitle extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    var titleMarkup = '';
    if (this.props.count != undefined) {
      titleMarkup = (
        <h1 className="fc-title">
          { this.props.title }
          <span className="fc-subtitle">
            { this.props.count }
          </span>
        </h1>
      );
    } else {
      titleMarkup = (
        <h1 className="fc-title">
          { this.props.title }
        </h1>
      );
    }
    var buttonMarkup = '';
    if (this.props.buttonClickHandler != undefined) {
      buttonMarkup = (
        <div className="fc-col-2-6 fc-push-2-6 fc-actions">
          <button className="fc-btn fc-btn-primary"
                  onClick={ this.props.buttonClickHandler.bind(this) }>
            <i className="icon-add"></i> { this.props.title }
          </button>
        </div>
      );
    }
    return (
      <div className="fc-grid gutter">
        <div className="fc-col-2-6">
          { titleMarkup }
        </div>
        { buttonMarkup }
      </div>
    );
  }
}

SectionTitle.propTypes = {
  title: React.PropTypes.string,
  count: React.PropTypes.number,
  buttonClickHandler: React.PropTypes.func,
};
