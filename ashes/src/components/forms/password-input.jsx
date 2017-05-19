// @flow

import React, { Component } from 'react';
import styles from './css/password-input.css';
import { autobind } from 'core-decorators';

let zxcvbn;

type State = {
  score: number,
  feedback: string,
};

export default class PasswordInput extends Component {
  state: State = {
    score: -1,
    feedback: '',
  };

  componentDidMount() {
    // https://webpack.js.org/guides/migrating/#code-splitting-with-es2015
    import('zxcvbn').then(m => zxcvbn = m);
  }

  checkStength(value: string) {
    if (zxcvbn) {
      const result = zxcvbn(value);
      /*
      scores:
         0 # too guessable: risky password. (guesses < 10^3)
         1 # very guessable: protection from throttled online attacks. (guesses < 10^6)
         2 # somewhat guessable: protection from unthrottled online attacks. (guesses < 10^8)
         3 # safely unguessable: moderate protection from offline slow-hash scenario. (guesses < 10^10)
         4 # very unguessable: strong protection from offline slow-hash scenario. (guesses >= 10^10)
       */

      let score = 0;
      let feedback = result.feedback.warning || 'Weak';
      if (result.score >= 3) {
        score = 2;
        feedback = 'Strong';
      } else if (result.score > 1 && Math.floor(result.guesses_log10) > 5) {
        score = 1;
        feedback = 'Moderate';
        if (result.feedback.warning) {
          feedback = `Moderate. ${result.feedback.warning}.`;
        }
      }

      this.setState({
        score,
        feedback,
      });
    }
  }

  @autobind
  handleChange(event: Object) {
    this.checkStength(event.target.value);

    if (this.props.onChange) {
      this.props.onChange(event);
    }
  }

  @autobind
  handleInput(event: Object) {
    this.checkStength(event.target.value);

    if (this.props.onInput) {
      this.props.onInput(event);
    }
  }

  render() {
    const {className, onChange, onInput, ...inputProps} = this.props;
    return (
      <div styleName="password-input">
        <input
          className={className}
          styleName="input"
          onChange={this.handleChange}
          onInput={this.handleInput}
          {...inputProps}
        />
        <div title={this.state.feedback} styleName="indicators" className={`_score_${this.state.score}`}>
          <div></div>
          <div></div>
          <div></div>
        </div>
      </div>
    );
  }
}
