// @flow

// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// styles
import styles from './question-box.css';

// types
type Props = {
  title: string,
  content: any,
  footer: any,
  isActive?: boolean,
  onClick?: ?Function,
}

type State = {
  isActive: boolean,
}

class QuestionBox extends React.Component {

  props: Props;

  static defaultProps = {
    isActive: false,
    onClick: null,
  };

  state: State = {
    isActive: false
  };

  componentWillMount() {
    const { isActive } = this.props;

    if (isActive) {
      this.toggleActiveState();
    }
  }

  @autobind
  toggleActiveState() {
    const { isActive } = this.state;
    this.setState({isActive: !isActive});
  }

  @autobind
  onClickHandler() {
    const { onClick } = this.props;

    this.toggleActiveState();

    if (!_.isNull(onClick)) {
      return onClick();
    }

    return null;
  }

  render() {
    const { content, title, footer } = this.props;
    const isActive = this.state.isActive ? 'active' : 'inactive';

    return (
      <div styleName={`question-box-container-${isActive}`} onClick={this.onClickHandler}>
        <div styleName="question-box-title">
          {title}
        </div>
        <div styleName="question-box-content">
          {content}
        </div>
        <div styleName="question-box-footer-display">
          {footer}
        </div>
      </div>
    );
  }
}

export default QuestionBox;
