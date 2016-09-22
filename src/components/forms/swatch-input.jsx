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
  value: ?string,
};

type State = {
  value: ?string,
};

class SwatchInput extends Component {
  props: Props;

  static defaultProps = {
    value: ''
  };

  componentWillUpdate(nextProps: Props) {
    if (this.state.value != nextProps.value) {
      this.setState({ value: nextProps.value });
    }
  }

  @autobind
  handleChange(value: string) {
    if (this.props.onChange) {
      this.props.onChange(value);
    } else {
      this.setState({value});
    }
  };

  state: State = {
    value: this.props.value
  };

  render(): Element {
    const hexCode = this.state.value.toUpperCase();
    const colorStyle = {
      background: `#${hexCode}`,
    };

    return (
      <div styleName="swatch-input">
        <input
          type="text"
          maxLength="6"
          className="fc-text-input"
          onChange={({ target }) => this.handleChange(target.value)}
          value={hexCode} 
        />
        <span styleName="swatch" style={colorStyle} />
      </div>
    );
  }
}

export default SwatchInput;