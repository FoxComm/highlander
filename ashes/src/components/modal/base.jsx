/* @flow */

// libs
import noop from 'lodash/noop';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import Transition from 'react-transition-group/CSSTransitionGroup';

// components
import Overlay from 'components/overlay/overlay';

type Props = {
  isVisible: boolean,
  onCancel?: () => void,
  children?: Element<*> | Array<Element<*>>, // This is an ugly bug in Flow :(
};

export default class ModalContainer extends Component {
  props: Props;

  static defaultProps: $Shape<Props> = {
    onCancel: noop,
  };

  componentDidMount() {
    console.log('modal componentDidMount');
    if (this.props.isVisible) {
      window.addEventListener('keyPress', this.handleKeyPress);
    }
  }

  componentWillReceiveProps() {
    console.log('modal componentWillReceiveProps');
    if (this.props.isVisible) {
      window.addEventListener('keyPress', this.handleKeyPress);
    } else {
      window.removeEventListener('keyPress', this.handleKeyPress);
    }
  }

  @autobind
  handleKeyPress(e) {
    if (e.keyCode === 27 /*esc*/) {
      e.preventDefault();

      this.props.onCancel();
    }
  }

  render() {
    const { children, isVisible, onCancel } = this.props;
    let content;

    if (isVisible) {
      content = (
        <div className="fc-modal">
          <Overlay shown={isVisible} onClick={onCancel} />
          <div className="fc-modal-container" onKeyDown={handleEscKeyPress}>
            {children}
          </div>
        </div>
      );
    }

    return (
      <Transition component="div"
                  transitionName="modal"
                  transitionAppear={true}
                  transitionAppearTimeout={120}
                  transitionEnterTimeout={120}
                  transitionLeaveTimeout={100}
      >
        {content}
      </Transition>
    );
  }
}

