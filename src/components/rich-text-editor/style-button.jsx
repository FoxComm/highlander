/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import classNames from 'classnames';

type Props = {
  isActive: boolean,
  labelIcon: string,
  onClick: (style: string) => void,
  style: string,
};

function stopPropagation(event: Object) {
  event.preventDefault();
  event.stopPropagation();
}

export default class StyleButton extends Component<void, Props, void> {
  handleClick(event: Object) {
    stopPropagation(event);
    this.props.onClick(this.props.style);
  }

  render() {
    const className = classNames('fc-rich-text-editor__command-button', {
      '_active': this.props.isActive,
    });

    return (
      <button
        className={className}
        onClick={this.handleClick.bind(this)}
        onMouseDown={stopPropagation}
        onMouseUp={stopPropagation}>
        <i className={this.props.labelIcon} />
      </button>
    );
  }
}
