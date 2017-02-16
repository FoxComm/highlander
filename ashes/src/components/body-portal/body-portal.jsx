// libs
import React, { Component, Children, Element } from 'react';
import ReactDOM from 'react-dom';

type Props = {
  active?: boolean;
  left: ?number;
  top: ?number;
  className: ?string;
};

export default class BodyPortal extends Component {
  props: Props;

  static defaultProps: Props = {
    active: true,
    left: 0,
    top: 0,
    className: '',
  };

  _target: HTMLElement; // HTMLElement<*>, a div that is appended to the body

  updateStyle(): void {
    const { left, top } = this.props;

    const style = this._target.style;

    style.left = left + 'px';
    style.top = top + 'px';
    style.zIndex = 201;
  }

  componentDidMount(): void {
    if (!this.props.active) {
      return;
    }

    this.port();
  }

  componentDidUpdate(): void {
    if (!this.props.active) {
      return;
    }

    this.renderContent();
  }

  componentWillUnmount(): void {
    if (!this.props.active) {
      return;
    }

    ReactDOM.unmountComponentAtNode(this._target);
    document.body.removeChild(this._target);
  }

  port() {
    const { className } = this.props;

    const container = document.createElement('div');
    container.className = className;

    this._target = document.body.appendChild(container);

    this.renderContent();
  }

  renderContent() {
    if (!this.props.children) {
      return;
    }

    this.updateStyle();

    ReactDOM.unstable_renderSubtreeIntoContainer(
      this, Children.only(this.props.children), this._target
    );
  }

  render() {
    return this.props.active ? null : this.props.children;
  }
}
