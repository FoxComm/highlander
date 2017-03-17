/* @flow */

// libs
import React, { Component } from 'react';

type Props = {
  autoFocus: boolean,
  className: string,
};

class TypeaheadInput extends Component {
  props: Props;

  _input: HTMLElement;

  static defaultProps = {
    className: '',
  };

  render() {
    const { className, ...rest } = this.props;

    return (
      <div className={className}>
        <i className="fc-typeahead__input-icon icon-search" />
        <input
          className="fc-input fc-typeahead__input"
          type="text"
          {...rest}
          ref={i => this._input = i}
        />
      </div>
    );
  }
}

export default TypeaheadInput;
