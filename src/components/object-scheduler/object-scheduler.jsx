/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import moment from 'moment';
import _ from 'lodash';
import { isActive } from '../../paragons/common';

import { illuminateAttributes, setAttribute, setAttributes } from '../../paragons/form-shadow-object';

import { Dropdown, DropdownItem } from '../dropdown';
import DateTimePicker from '../date-time-picker/date-time-picker';

type Props = FormShadowAttrs & {
  onChange: (form: FormAttributes, shadow: ShadowAttributes) => void,
  title: string,
};

type State = {
  showActiveFromPicker: boolean,
  showActiveToPicker: boolean,
};

export default class ProductState extends Component<void, Props, State> {
  static propTypes = {
    form: PropTypes.object.isRequired,
    shadow: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    title: PropTypes.string,
  };

  state: State = this.fieldsStateFromProps(this.props);

  fieldsStateFromProps(props: FormShadowAttrs): State {
    const activeFrom = this.getFormAttribute(props, 'activeFrom');
    const activeTo = this.getFormAttribute(props, 'activeTo');

    return {
      showActiveFromPicker: !!activeFrom,
      showActiveToPicker: !!activeTo
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    this.setState(this.fieldsStateFromProps(nextProps));
  }

  illuminatedAttributes(props: FormShadowAttrs): IlluminatedAttributes {
    return illuminateAttributes(props.form, props.shadow);
  }

  getFormAttribute(props: FormShadowAttrs, name: string): any {
    return _.get(this.illuminatedAttributes(props), [name, 'value']);
  }

  get activeFrom(): ?string {
    return this.getFormAttribute(this.props, 'activeFrom');
  }

  get activeTo(): ?string {
    return this.getFormAttribute(this.props, 'activeTo');
  }

  get activeFromPicker(): ?Element {
    if (this.state.showActiveFromPicker) {
      const activePhrase = `${this.props.title} will be active on:`;
      return (
        <div className="fc-product-state__picker">
          <div className="fc-product-state__picker-header">{activePhrase}</div>
          <div className="fc-product-state__picker-label">
            Start
          </div>
          <DateTimePicker
            dateTime={this.activeFrom}
            onChange={this.updateActiveFrom}
            onCancel={this.handleCancelFrom} />
        </div>
      );
    }
  }

  get activeToPicker(): ?Element {
    if (this.state.showActiveFromPicker) {
      const picker = this.state.showActiveToPicker
        ? (
          <DateTimePicker
            dateTime={this.activeTo}
            onChange={this.updateActiveTo}
            onCancel={this.handleCancelTo} />
        )
        : <a onClick={this.handleShowActiveTo}><i className="icon-add" /></a>;

      return (
        <div className="fc-product-state__picker _end">
          <div className="fc-product-state__picker-label">
            End
          </div>
          {picker}
        </div>
      );
    }
  }

  get isActive(): bool {
    return isActive(this.activeFrom, this.activeTo);
  }

  @autobind
  updateActiveTo(value: ?string) {
    if (this.isPeriodValid(this.activeFrom, value)) {
      this.updateAttribute('activeTo', value);
    }
  }

  @autobind
  updateActiveFrom(value: ?string) {
    if (this.isPeriodValid(value, this.activeTo)) {
      this.updateAttribute('activeFrom', value);
    }
  }

  isPeriodValid(activeFrom: ?string, activeTo: ?string): boolean {
    if (!activeTo || !activeFrom) return true;

    const activeFromTime = moment(activeFrom).toDate().getTime();
    const activeToTime = moment(activeTo).toDate().getTime();

    return activeToTime >= activeFromTime;
  }

  updateAttribute(label: string, value: ?string) {
    const { form, shadow } = this.props;
    const [newForm, newShadow] = setAttribute(label, 'datetime', value, form, shadow);
    this.props.onChange(newForm, newShadow);
  }

  setFromTo(activeFrom: ?string, activeTo: ?string): FormShadowAttrsPair {
    const { form, shadow } = this.props;

    return setAttributes({
      activeFrom: {
        value: activeFrom,
        type: 'datetime'
      },
      activeTo: {
        value: activeTo,
        type: 'datetime'
      }
    }, form, shadow);
  }

  @autobind
  handleActiveChange(value: string) {
    const now = moment().utc().format('YYYY-MM-DDTHH:mm:ss.SSSZ');
    const activeFrom = value == 'active' ? now : null;

    const [newForm, newShadow] = this.setFromTo(activeFrom, null);
    this.props.onChange(newForm, newShadow);
  }

  @autobind
  handleCancelFrom() {
    const [newForm, newShadow] = this.setFromTo(null, null);

    this.setState({
      showActiveFromPicker: false,
      showActiveToPicker: false,
    }, () => this.props.onChange(newForm, newShadow));
  }

  @autobind
  handleCancelTo() {
    this.setState({
      showActiveToPicker: false,
    }, () => this.updateActiveTo(null));
  }

  @autobind
  handleShowActiveTo() {
    this.setState({
      showActiveFromPicker: true,
      showActiveToPicker: true,
    });
  }


  get activeDropdown(): Element {
    const activeState = this.isActive ? 'active' : 'inactive';
    const isDisabled = this.state.showActiveFromPicker;
    return (
      <Dropdown
        className="fc-product-state__active-state"
        disabled={isDisabled}
        value={activeState}
        onChange={this.handleActiveChange}>
        <DropdownItem value="active">Active</DropdownItem>
        <DropdownItem value="inactive">Inactive</DropdownItem>
      </Dropdown>
    );
  }

  @autobind
  handleClickCalendar() {
    if (this.state.showActiveFromPicker) {
      this.setState({
        showActiveFromPicker: false,
        showActiveToPicker: false,
      });
    } else {
      this.setState({
        showActiveFromPicker: true,
        showActiveToPicker: !_.isNull(this.activeTo) && !_.isUndefined(this.activeTo),
      });
    }
  }

  @autobind
  handleClickCloseFrom() {
    this.setState({
      showActiveFromPicker: false,
      showActiveToPicker: false,
    });
  }

  render(): Element {
    return (
      <div className="fc-product-state">
        <div className="fc-product-state__header">
          <div className="fc-product-state__text">
            State
          </div>
          <div className="fc-product-state__icon">
            <a onClick={this.handleClickCalendar}><i className="icon-calendar" /></a>
          </div>
        </div>
        {this.activeDropdown}
        {this.activeFromPicker}
        {this.activeToPicker}
      </div>
    );
  }
}
