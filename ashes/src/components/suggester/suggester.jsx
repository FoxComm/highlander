/**
 * @flow weak
 */

// libs
import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';
import s from './suggester.css';

type State = {
  value: string,
};

export class Suggester extends Component {
  state: State = {
    value: '',
  };

  static defaultProps = {
    data: {},
    primaryTitle: 'Proposed:',
    othersTitle: 'Other:',
    onChange: () => {},
    onPick: () => {},
    className: '',
  };

  static propTypes = {
    proposed: PropTypes.array,
    data: PropTypes.shape({
      primary: PropTypes.array,
      secondary: PropTypes.array,
    }).isRequired,
    primaryTitle: PropTypes.string,
    othersTitle: PropTypes.string,
    onChange: PropTypes.func,
    onPick: PropTypes.func,
    className: PropTypes.string,
  };

  componentDidUpdate() {
    if (this._input) {
      this._input.scrollLeft = this._input.scrollWidth;
    }
  }

  render() {
    const { primaryTitle, othersTitle, className, data: { primary, secondary } } = this.props;
    const { value } = this.state;

    const primaryHtml = !!primary && primary.map(line => (
      <div className={classNames(s.item, s._proposed)} key={line.id} onClick={() => this._onPick(line)}>

        <div className={s.itemPrefix}>{line.prefix}</div>
        <div className={s.itemValue}>{line.text}</div>
      </div>
    ));
    const secondaryHtml = secondary.map(line => (
      <div className={s.item} key={line.id} onClick={() => this._onPick(line)}>
        <div className={s.itemPrefix}>{line.prefix}</div>
        <div className={s.itemValue}>{line.text}</div>
      </div>
    ));

    return (
      <div className={classNames(s.root, className)}>
        <input
          onChange={(e) => this._onType(e)}
          ref={d => this._input = d}
          value={value}
          type="text"
          className={classNames(s.input, 'fc-input')} />

        <div className={s.dropdown}>
          {!!primary && [
            <div className={s.listHeader} key="primaryTitle">{primaryTitle}</div>,
            <div key="primaryHtml">{primaryHtml}</div>,
            <div className={s.listHeader} key="othersTitle">{othersTitle}</div>,
          ]}

          <div className={s.others}>
            {secondaryHtml}
          </div>
        </div>
      </div>
    );
  }

  _input: any;

  _onType(e) {
    this.setState({ value: e.target.value });
    this.props.onChange(e.target.value);
  }

  _onPick(item) {
    const { onPick } = this.props;

    this.setState({ value: `${item.prefix} » ${item.text}` });
    this.props.onPick(item.id);
  }
}
