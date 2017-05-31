// @flow
// inspired by https://github.com/ReactTraining/react-history/blob/master/modules/Prompt.js
// but adapted to history v2.x package

import { Component } from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';

type TransitionResult = Function|string;

type Props = {
  when: boolean,
  message: ?TransitionResult,
}

type TransitionCallback = (result: ?TransitionResult) => void;

export default class Prompt extends Component {
  props: Props;
  unlisten: Function;

  static contextTypes = {
    router: PropTypes.object.isRequired,
  };

  static defaultProps = {
    when: true,
  };

  componentDidMount() {
    this.unlisten = this.context.router.listenBefore(this.transitionHook);
  }

  @autobind
  transitionHook(nextLocation: Location, callback: TransitionCallback) {
    let result;
    if (this.props.when) {
      result = this.props.message;
    }
    callback(result);
  }

  componentWillUnmount() {
    this.unlisten();
  }

  render() {
    return null;
  }
}
