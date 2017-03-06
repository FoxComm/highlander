/* @flow */

// libs
import React, { Component } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// styles
import styles from './faq.css';

type Props = {
  isInitiallyCollapsed: boolean,
  question: string,
  answer: string
};

type State = {
  isCollapsed: boolean,
};

class FAQ extends Component {
  props: Props;

  static defaultProps = {
    isInitiallyCollapsed: true,
  };

  state: State = {
    isCollapsed: this.props.isInitiallyCollapsed,
  };

  @autobind
  toggleCollapsed() {
    this.setState({
      isCollapsed: !this.state.isCollapsed,
    });
  }

  render() {
    const modifiers = classNames({
      [styles._collapsed]: this.state.isCollapsed,
    });

    return (
      <article styleName="faq" className={modifiers}>
        <h2 styleName="question" onClick={this.toggleCollapsed}>{this.props.question}</h2>
        <p
          styleName="answer"
          dangerouslySetInnerHTML={{__html: this.props.answer}}
        />
      </article>
    );
  }
}

export default FAQ;
