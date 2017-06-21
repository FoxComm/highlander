// libs
import React, { Component, Children, Element } from 'react';
import ReactDOM from 'react-dom';
import classNames from 'classnames';

type Props = {
  active?: boolean,
  left: ?number,
  top: ?number,
  className: ?string,
  getRef?: Function,
};

export default class BodyPortal extends Component {
  props: Props;

  static defaultProps: Props = {
    active: true,
    left: 0,
    top: 0,
    className: '',
    getRef: () => {},
  };

  _target: HTMLElement; // HTMLElement, a div that is appended to the body

  updateStyle(): void {
    const { left, top } = this.props;

    const style = this._target.style;

    style.left = left + 'px';
    style.top = top + 'px';
    style.zIndex = 50;
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

    // @todo looks like this not working at all, because it is just a wrapper
    container.className = className;

    this._target = document.body.appendChild(container);

    this.renderContent();
  }

  renderContent() {
    if (!this.props.children) {
      return null;
    }

    this.updateStyle();

    const componentNode = ReactDOM.unstable_renderSubtreeIntoContainer(
      this,
      Children.only(this.props.children),
      this._target
    );
    const domNode = ReactDOM.findDOMNode(componentNode);

    this.props.getRef(domNode);
  }

  render() {
    const { active, className, children, getRef } = this.props;

    if (this.props.active) {
      return null; // see renderContent()
    }

    return (
      <div className={classNames(className)} ref={getRef}>
        {children}
      </div>
    );
  }
}
