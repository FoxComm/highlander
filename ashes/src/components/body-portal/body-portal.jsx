// libs
import React, { Component, Element } from 'react';
import ReactDOM from 'react-dom';

type Props = {
  left: ?number;
  top: ?number;
  className: ?string;
};

export default class BodyPortal extends Component {
  static props: Props;

  static defaultProps: Props = {
    left: 0,
    top: 0,
    className: '',
  };

  _target: HTMLElement; // HTMLElement, a div that is appended to the body
  _component: Element; // ReactElement, which is mounted on the target

  updateStyle(): void {
    const { left, top } = this.props;

    const style = this._target.style;

    style.left = left + 'px';
    style.top = top + 'px';
    style.zIndex = 201;
  }

  componentDidMount(): void {
    this.port();
  }

  port() {
    const { className, children } = this.props;

    const container = document.createElement('div');
    container.className = className;

    this._target = document.body.appendChild(container);
    this._component = ReactDOM.render(React.Children.only(children), this._target);

    this.updateStyle();
  }

  componentDidUpdate(): void {
    this.updateStyle();

    this._component = ReactDOM.render(React.Children.only(this.props.children), this._target);
  }

  componentWillUnmount(): void {
    ReactDOM.unmountComponentAtNode(this._target);
    document.body.removeChild(this._target);
  }

  render(): Element {
    return null;
  }
}
