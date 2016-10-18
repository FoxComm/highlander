/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// styles
import styles from './css/swatch-input.css';

type Props = {
  onChange: (value: string) => void,
  value: string,
};

class SwatchInput extends Component {
  props: Props;

  static defaultProps = {
    value: '',
  };

  @autobind
  handleChange({ target }) {
    this.props.onChange(target.value);
  };

  render(): Element {
    const hexCode = this.props.value;
    const colorStyle = {
      background: `#${hexCode}`,
    };

    return (
      <div styleName="swatch-input">
        <input
          type="text"
          maxLength="6"
          className="fc-text-input"
          onChange={this.handleChange}
          value={hexCode}
        />
        <span styleName="swatch" style={colorStyle} />
      </div>
    );
  }
}

export default SwatchInput;
