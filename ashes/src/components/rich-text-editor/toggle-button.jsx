/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// components
import Icon from 'components/core/icon';

type Props = {
  className?: string,
  isActive: boolean,
  labelIcon: string,
  title?: string,
  onClick: (value: any) => void,
  value: any,
};

function stop(event: Object) {
  event.preventDefault();
  event.stopPropagation();
}

export default class ToggleButton extends Component {
  props: Props;

  @autobind
  handleClick(event: SyntheticEvent) {
    this.props.onClick(this.props.value);
  }

  render() {
    const className = classNames('fc-rich-text-editor__command-button', this.props.className, {
      '_active': this.props.isActive,
    });

    return (
      <button
        type="button"
        title={this.props.title}
        className={className}
        onClick={this.handleClick}
        onMouseDown={stop}
        onMouseUp={stop}>
        <Icon name={this.props.labelIcon} />
      </button>
    );
  }
}
