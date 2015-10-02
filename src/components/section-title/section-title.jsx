'use strict';

import React from 'react';
import FCTitle from './fc-title';
import FCTitleWithSubtitle from './fc-title-with-subtitle';

export default class SectionTitle extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    let titleMarkup = null;
    if (this.props.count !== undefined) {
      titleMarkup = (
        <FCTitleWithSubtitle title={ this.props.title } subtitle={ this.props.count } />
      );
    } else {
      titleMarkup = (
        <FCTitle title={ this.props.title } />
      );
    }
    let buttonMarkup = null;
    if (this.props.buttonClickHandler !== undefined) {
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
